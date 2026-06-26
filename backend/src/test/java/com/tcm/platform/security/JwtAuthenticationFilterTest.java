package com.tcm.platform.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tcm.platform.entity.Account;
import com.tcm.platform.mapper.AccountMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void disabledAccountCannotAuthenticateWithAnExistingToken() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        AccountMapper accountMapper = mock(AccountMapper.class);
        Account account = new Account();
        account.setUsername("doctor1");
        account.setRole("doctor");
        account.setEnabled(false);
        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.extractUsername("valid-token")).thenReturn("doctor1");
        when(accountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(account);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtUtil, accountMapper);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
