package com.tcm.platform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 应用通用配置
 * 
 * 任务：
 * 1. 添加 @Configuration 注解（已添加）
 * 2. 定义 2 个 Bean：PasswordEncoder 和 RestTemplate
 */
@Configuration
public class AppConfig {

    private final Duration connectTimeout;
    private final Duration readTimeout;

    public AppConfig(
            @Value("${http.client.connect-timeout:5s}") Duration connectTimeout,
            @Value("${http.client.read-timeout:60s}") Duration readTimeout
    ) {
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    /**
     * 密码编码器 - 使用 BCrypt 加密算法
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /**
     * HTTP 客户端 - 用于调用外部 API（如 AI 服务）
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return new RestTemplate(requestFactory);
    }
}
