package com.tcm.platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcm.platform.dto.AIQuestionRequest;
import org.springframework.http.HttpMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 阿里云 DashScope OpenAI 兼容模式客户端。
 */
@Component
public class DashScopeClient {

    private static final String SYSTEM_PROMPT = """
            你是一名中医养生助手。请仅提供一般性的生活调养建议，不要进行诊断、开具处方或替代医生。
            请根据可靠、通行的健康与养护知识回答，不要声称自己进行了实时联网检索，也不要虚构资料来源。
            平台会在回答之外独立展示相关养生文章和药膳推荐，因此不要编造或猜测平台内的文章、药膳标题。
            回答应清晰、谨慎，并提醒用户：症状严重、持续或出现危险信号时应及时就医。
            """;

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String model;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DashScopeClient(
            RestTemplate restTemplate,
            @Value("${ai.dashscope.base-url}") String baseUrl,
            @Value("${ai.dashscope.model}") String model
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public String ask(String apiKey, String question, List<AIQuestionRequest.ContextMessage> context) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        DashScopeRequest request = new DashScopeRequest(
                model,
                buildMessages(question, context)
        );
        DashScopeResponse response;
        try {
            response = restTemplate.postForObject(
                    baseUrl,
                    new HttpEntity<>(request, headers),
                    DashScopeResponse.class
            );
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "DashScope 调用失败: HTTP " + ex.getRawStatusCode() + " " + ex.getResponseBodyAsString(),
                    ex
            );
        }

        String answer = extractAnswer(response);
        if (!hasText(answer)) {
            throw new IllegalStateException("DashScope 未返回有效回答");
        }
        return answer.trim();
    }

    public void askStream(
            String apiKey,
            String question,
            List<AIQuestionRequest.ContextMessage> context,
            Consumer<String> chunkConsumer
    ) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.TEXT_EVENT_STREAM));

        DashScopeStreamRequest request = new DashScopeStreamRequest(
                model,
                buildMessages(question, context),
                true
        );

        try {
            restTemplate.execute(
                    baseUrl,
                    HttpMethod.POST,
                    clientHttpRequest -> {
                        clientHttpRequest.getHeaders().putAll(headers);
                        objectMapper.writeValue(clientHttpRequest.getBody(), request);
                    },
                    clientHttpResponse -> {
                        if (clientHttpResponse.getStatusCode().isError()) {
                            String body = new String(clientHttpResponse.getBody().readAllBytes(), StandardCharsets.UTF_8);
                            throw new IllegalStateException(
                                    "DashScope 调用失败: HTTP " + clientHttpResponse.getStatusCode().value() + " " + body
                            );
                        }
                        readStream(clientHttpResponse.getBody(), chunkConsumer);
                        return null;
                    }
            );
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "DashScope 调用失败: HTTP " + ex.getRawStatusCode() + " " + ex.getResponseBodyAsString(),
                    ex
            );
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private List<Message> buildMessages(String question, List<AIQuestionRequest.ContextMessage> context) {
        List<Message> messages = new ArrayList<>();
        messages.add(new Message("system", SYSTEM_PROMPT.trim()));

        if (context != null) {
            context.stream()
                    .filter(message -> message != null && hasText(message.content()))
                    .filter(message -> "user".equals(message.role()) || "assistant".equals(message.role()))
                    .map(message -> new Message(message.role(), message.content().trim()))
                    .forEach(messages::add);
        }

        messages.add(new Message("user", question));
        return messages;
    }

    private String extractAnswer(DashScopeResponse response) {
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            return null;
        }
        Choice firstChoice = response.choices().get(0);
        if (firstChoice == null || firstChoice.message() == null) {
            return null;
        }
        return firstChoice.message().content();
    }

    private void readStream(java.io.InputStream inputStream, Consumer<String> chunkConsumer) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String payload = line.substring("data:".length()).trim();
                if ("[DONE]".equals(payload)) {
                    return;
                }
                String content = extractDeltaContent(payload);
                if (hasText(content)) {
                    chunkConsumer.accept(content);
                }
            }
        }
    }

    private String extractDeltaContent(String payload) throws IOException {
        JsonNode root = objectMapper.readTree(payload);
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return "";
        }
        return choices.get(0).path("delta").path("content").asText("");
    }

    private record DashScopeRequest(String model, List<Message> messages) {
    }

    private record DashScopeStreamRequest(String model, List<Message> messages, boolean stream) {
    }

    private record Message(String role, String content) {
    }

    private record DashScopeResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }
}
