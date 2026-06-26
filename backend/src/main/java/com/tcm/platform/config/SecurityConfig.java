package com.tcm.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcm.platform.common.Result;
import com.tcm.platform.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置
 * 
 * 任务：
 * 1. 禁用 CSRF（无状态 API 不需要）
 * 2. 配置无状态会话管理
 * 3. 设置 URL 权限规则
 * 4. 添加 JWT 过滤器
 * 
 * URL 权限规则：
 * - /api/auth/**     → 允许所有访问（permitAll）
 * - /api/patient/**  → 患者业务仅患者角色，公开内容接口除外
 * - /api/doctor/**   → 仅医生角色
 * - /api/admin/**    → 按管理功能限制管理员角色
 * - /uploads/**      → 允许所有访问
 * - /swagger-ui/**   → 允许所有访问
 * - 其他请求         → 需要认证
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper
    ) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            objectMapper.writeValue(
                                    response.getWriter(),
                                    Result.error(401, "登录状态无效，请重新登录")
                            );
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            objectMapper.writeValue(
                                    response.getWriter(),
                                    Result.error(403, "当前账号无权执行此操作")
                            );
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/patient/recipe/**").permitAll()
                        .requestMatchers("/api/patient/knowledge/**").permitAll()
                        .requestMatchers("/api/patient/**").hasRole("PATIENT")
                        .requestMatchers("/api/doctor/**").hasRole("DOCTOR")
                        .requestMatchers("/api/admin/personnel/**").hasRole("ADMIN")
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/admin/consultation/*/claim"
                        ).denyAll()
                        .requestMatchers("/api/admin/consultation/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/knowledge/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/recipe/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/export/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/dashboard/**").hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
