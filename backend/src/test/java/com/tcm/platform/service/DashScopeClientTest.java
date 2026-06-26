package com.tcm.platform.service;

import com.tcm.platform.dto.AIQuestionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DashScopeClientTest {

    @Test
    void askSendsAuthorizedRequestAndParsesAnswer() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        String url = "https://example.test/dashscope";
        server.expect(once(), requestTo(url))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.model").value("qwen-plus"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[0].content").value(containsString("你是一名中医养生助手")))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("春季如何调养？"))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {
                              "message": {
                                "role": "assistant",
                                "content": "建议规律作息。"
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));
        DashScopeClient client = new DashScopeClient(restTemplate, url, "qwen-plus");

        String answer = client.ask("test-key", "春季如何调养？", List.of());

        assertThat(answer).isEqualTo("建议规律作息。");
        server.verify();
    }

    @Test
    void askIncludesConversationContextInPrompt() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        String url = "https://example.test/dashscope";
        List<AIQuestionRequest.ContextMessage> context = List.of(
                new AIQuestionRequest.ContextMessage("user", "我最近下午容易疲倦"),
                new AIQuestionRequest.ContextMessage("assistant", "可以先看作息和饮食"),
                new AIQuestionRequest.ContextMessage("system", "这条非法角色应被忽略")
        );
        server.expect(once(), requestTo(url))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andExpect(jsonPath("$.messages[1].content").value("我最近下午容易疲倦"))
                .andExpect(jsonPath("$.messages[2].role").value("assistant"))
                .andExpect(jsonPath("$.messages[2].content").value("可以先看作息和饮食"))
                .andExpect(jsonPath("$.messages[3].role").value("user"))
                .andExpect(jsonPath("$.messages[3].content").value("那晚饭要注意什么？"))
                .andRespond(withSuccess("""
                        {
                          "choices": [
                            {
                              "message": {
                                "role": "assistant",
                                "content": "晚饭建议清淡适量。"
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));
        DashScopeClient client = new DashScopeClient(restTemplate, url, "qwen-plus");

        String answer = client.ask("test-key", "那晚饭要注意什么？", context);

        assertThat(answer).isEqualTo("晚饭建议清淡适量。");
        server.verify();
    }

    @Test
    void askThrowsHelpfulMessageWhenDashScopeReturnsError() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        String url = "https://example.test/dashscope";
        server.expect(once(), requestTo(url))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                                {
                                  "code": "InvalidParameter",
                                  "message": "model glm-5 is not available"
                                }
                                """));
        DashScopeClient client = new DashScopeClient(restTemplate, url, "glm-5");

        assertThatThrownBy(() -> client.ask("test-key", "你好", List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DashScope 调用失败")
                .hasMessageContaining("400")
                .hasMessageContaining("model glm-5 is not available");
        server.verify();
    }

    @Test
    void askStreamSendsStreamRequestAndEmitsDeltaContent() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        String url = "https://example.test/dashscope";
        server.expect(once(), requestTo(url))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("qwen-plus"))
                .andExpect(jsonPath("$.stream").value(true))
                .andExpect(jsonPath("$.messages[1].content").value("最近胃口不好怎么办？"))
                .andRespond(withSuccess("""
                        data: {"choices":[{"delta":{"content":"建议先"}}]}

                        data: {"choices":[{"delta":{"content":"清淡饮食。"}}]}

                        data: [DONE]

                        """, MediaType.TEXT_EVENT_STREAM));
        DashScopeClient client = new DashScopeClient(restTemplate, url, "qwen-plus");
        List<String> chunks = new ArrayList<>();

        client.askStream("test-key", "最近胃口不好怎么办？", List.of(), chunks::add);

        assertThat(chunks).containsExactly("建议先", "清淡饮食。");
        server.verify();
    }
}
