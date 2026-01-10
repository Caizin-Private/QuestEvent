package com.questevent.config;
import com.questevent.service.OAuthSuccessService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.web.SecurityFilterChain;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class SecurityConfigTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test void securityFilterChainBeanShouldLoad() { assertNotNull(securityFilterChain); }

    @Test void jwtAuthFilterBeanShouldExist() { JwtAuthFilter filter = context.getBean(JwtAuthFilter.class); assertNotNull(filter); }

    @Test void oauthSuccessHandlerBeanShouldExist() { OAuthSuccessService handler = context.getBean(OAuthSuccessService.class); assertNotNull(handler); }

    @Test void corsConfigBeanShouldExist() { CorsConfig corsConfig = context.getBean(CorsConfig.class); assertNotNull(corsConfig); }
}
