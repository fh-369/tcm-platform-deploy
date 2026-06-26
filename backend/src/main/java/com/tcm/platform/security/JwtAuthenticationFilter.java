package com.tcm.platform.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.tcm.platform.entity.Account;
import com.tcm.platform.mapper.AccountMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * 从 Authorization 请求头中解析 JWT，并写入 Spring Security 上下文。
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtUtil jwtUtil;
    private final AccountMapper accountMapper;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, AccountMapper accountMapper) {
        this.jwtUtil = jwtUtil;
        this.accountMapper = accountMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = extractBearerToken(request);

        if (token != null
                && SecurityContextHolder.getContext().getAuthentication() == null
                && jwtUtil.validateToken(token)) {
            String username = jwtUtil.extractUsername(token);
            Account account = findEnabledAccount(username);
            if (account != null && account.getRole() != null && !account.getRole().isBlank()) {
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority(toSpringRole(account.getRole()));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, List.of(authority));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private Account findEnabledAccount(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        Account account = accountMapper.selectOne(
                Wrappers.<Account>lambdaQuery().eq(Account::getUsername, username)
        );
        return account != null && !Boolean.FALSE.equals(account.getEnabled()) ? account : null;
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length());
    }

    private String toSpringRole(String role) {
        String normalizedRole = role.toUpperCase(Locale.ROOT);
        if (normalizedRole.startsWith(ROLE_PREFIX)) {
            return normalizedRole;
        }
        return ROLE_PREFIX + normalizedRole;
    }
}
