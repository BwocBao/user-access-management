package com.r2s.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import io.jsonwebtoken.Claims;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtFilterTest {

    @Test
    void shouldSetAuthentication_whenUserTokenValid() throws ServletException, IOException {

        JwtUtil jwtUtil = mock(JwtUtil.class);
        Claims claims = mock(Claims.class);

        when(jwtUtil.extractAllClaims("token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("beo9");
        when(claims.get("role", String.class)).thenReturn("ROLE_USER");
        when(claims.get("type", String.class)).thenReturn("user");

        JwtFilter filter = new JwtFilter(jwtUtil);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldSetAuthentication_whenServiceTokenValid() throws Exception {

        JwtUtil jwtUtil = mock(JwtUtil.class);
        Claims claims = mock(Claims.class);

        when(jwtUtil.extractAllClaims("token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("auth-service");
        when(claims.get("role", String.class)).thenReturn("ROLE_SERVICE");
        when(claims.get("type", String.class)).thenReturn("service");

        JwtFilter filter = new JwtFilter(jwtUtil);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");

        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldContinueFilter_whenNoAuthorizationHeader() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        JwtFilter filter = new JwtFilter(jwtUtil);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldReturn401_whenServiceNotAllowed() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        Claims claims = mock(Claims.class);

        when(jwtUtil.extractAllClaims("token")).thenReturn(claims);
        when(claims.getSubject()).thenReturn("evil-service");
        when(claims.get("type", String.class)).thenReturn("service");

        JwtFilter filter = new JwtFilter(jwtUtil);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }
}