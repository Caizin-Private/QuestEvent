package com.questevent.config;

import com.questevent.dto.UserPrincipal;
import com.questevent.entity.User;
import com.questevent.enums.Role;
import com.questevent.repository.UserRepository;
import com.questevent.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validJwtAuthenticatesUser() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        JwtAuthFilter filter = new JwtAuthFilter(mock(UserRepository.class), jwtService);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/api/events");
        when(request.getHeader("Authorization")).thenReturn("Bearer valid");

        UserPrincipal principal =
                new UserPrincipal(1L, "user@test.com", Role.USER);

        when(jwtService.validateToken("valid")).thenReturn(true);
        when(jwtService.extractUserPrincipal("valid")).thenReturn(principal);

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(principal, auth.getPrincipal());

        verify(chain).doFilter(request, response);
    }

    @Test
    void invalidJwtOnApiReturns401() throws Exception {
        JwtService jwtService = mock(JwtService.class);
        JwtAuthFilter filter = new JwtAuthFilter(mock(UserRepository.class), jwtService);

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/api/events");
        when(request.getHeader("Authorization")).thenReturn("Bearer bad");

        when(jwtService.validateToken("bad"))
                .thenThrow(new RuntimeException("invalid"));

        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        filter.doFilterInternal(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(body.toString().contains("Unauthorized"));
    }

    @Test
    void apiWithoutTokenReturns401() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter(mock(UserRepository.class), mock(JwtService.class));

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/api/secure");

        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        filter.doFilterInternal(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(body.toString().contains("JWT Bearer token required"));
    }

    @Test
    void oauth2SessionAuthenticatesUserWithoutContinuingChain() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        JwtAuthFilter filter = new JwtAuthFilter(userRepository, mock(JwtService.class));

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/dashboard");

        DefaultOAuth2User oauthUser =
                new DefaultOAuth2User(
                        List.of(),
                        Map.of("email", "oauth@test.com"),
                        "email"
                );

        OAuth2AuthenticationToken oauthToken =
                new OAuth2AuthenticationToken(oauthUser, List.of(), "google");

        SecurityContextHolder.getContext().setAuthentication(oauthToken);

        User user = new User();
        user.setUserId(10L);
        user.setEmail("oauth@test.com");
        user.setRole(Role.USER);

        when(userRepository.findByEmail("oauth@test.com"))
                .thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, chain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(auth instanceof UsernamePasswordAuthenticationToken);

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        assertEquals("oauth@test.com", principal.email());

        verifyNoInteractions(chain);
    }

    @Test
    void oauth2NotAllowedForApiReturns401() throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter(mock(UserRepository.class), mock(JwtService.class));

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn("/api/secure");

        OAuth2AuthenticationToken oauthToken =
                new OAuth2AuthenticationToken(
                        mock(DefaultOAuth2User.class),
                        List.of(),
                        "google"
                );

        SecurityContextHolder.getContext().setAuthentication(oauthToken);

        StringWriter body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));

        filter.doFilterInternal(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(body.toString().contains("Unauthorized"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/login",      // shouldSkipLoginPath
            "/api/auth",   // authInfoEndpointIsPublic
            "/home"        // unauthenticatedNonApiRequestContinuesFilterChain
    })
    void nonProtectedEndpointsContinueFilterChain(String path) throws Exception {
        JwtAuthFilter filter = new JwtAuthFilter(
                mock(UserRepository.class),
                mock(JwtService.class)
        );

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getRequestURI()).thenReturn(path);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
