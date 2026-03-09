package com.r2s.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.mockito.Mockito.*;

class JwtFilterTest {

    @Test
    void shouldSetAuthentication_whenTokenValid() throws ServletException, IOException {

        JwtUtil jwtUtil = mock(JwtUtil.class);

        when(jwtUtil.extractUsername("token")).thenReturn("beo9");
        when(jwtUtil.extractRoles("token")).thenReturn("ROLE_USER");

        JwtFilter filter = new JwtFilter(jwtUtil);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assert SecurityContextHolder.getContext().getAuthentication() != null;

        verify(chain).doFilter(request, response);
    }

}