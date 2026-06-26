package com.tcm.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class AppConfigTest {

    @Test
    void restTemplateUsesLongerReadTimeoutForSlowAiResponses() {
        AppConfig appConfig = new AppConfig(Duration.ofSeconds(5), Duration.ofSeconds(60));

        RestTemplate restTemplate = appConfig.restTemplate();
        SimpleClientHttpRequestFactory requestFactory =
                (SimpleClientHttpRequestFactory) restTemplate.getRequestFactory();

        assertThat(ReflectionTestUtils.getField(requestFactory, "connectTimeout")).isEqualTo(5000);
        assertThat(ReflectionTestUtils.getField(requestFactory, "readTimeout")).isEqualTo(60000);
    }
}
