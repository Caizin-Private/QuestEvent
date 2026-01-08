package com.questevent.config;
import com.questevent.dto.UserPrincipal;
import com.questevent.entity.User;
import com.questevent.enums.Role;
import com.questevent.repository.UserRepository;
import com.questevent.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.security.core.context.SecurityContextHolder;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private FilterChain filterChain;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @BeforeEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateWithValidJwtToken() throws Exception {

        when(request.getRequestURI()).thenReturn("/api/users");

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer valid-token");

        when(jwtService.validateToken("valid-token")).thenReturn(true);

        UserPrincipal principal =
                new UserPrincipal(1L, "test@user.com", Role.USER);

        when(jwtService.extractUserPrincipal("valid-token"))
                .thenReturn(principal);

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().isAuthenticated());

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldRejectApiWithoutJwtToken() throws Exception {

        when(request.getRequestURI()).thenReturn("/api/programs");
        when(request.getHeader("Authorization")).thenReturn(null);

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void shouldRejectInvalidJwtToken() throws Exception {

        when(request.getRequestURI()).thenReturn("/api/programs");

        when(request.getHeader("Authorization"))
                .thenReturn("Bearer bad-token");

        when(jwtService.validateToken("bad-token"))
                .thenThrow(new RuntimeException("Invalid"));

        StringWriter sw = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(sw));

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void shouldAllowPublicAuthEndpointWithoutToken() throws Exception {

        when(request.getRequestURI()).thenReturn("/api/auth");

        jwtAuthFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
