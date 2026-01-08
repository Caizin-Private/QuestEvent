package com.questevent.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CorsConfigTest {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Test
    void corsConfigurationSourceBeanShouldLoad() {
        assertNotNull(corsConfigurationSource);
    }

    @Test
    void corsConfigurationShouldContainExpectedValues() {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        request.setContextPath("");
        request.setServletPath("");

        CorsConfiguration config =
                corsConfigurationSource.getCorsConfiguration(request);

        assertNotNull(config);

        assertTrue(config.getAllowedOrigins().contains("http://localhost:8080"));

        assertTrue(config.getAllowedMethods().contains("GET"));
        assertTrue(config.getAllowedMethods().contains("POST"));
        assertTrue(config.getAllowedMethods().contains("PUT"));
        assertTrue(config.getAllowedMethods().contains("DELETE"));
        assertTrue(config.getAllowedMethods().contains("OPTIONS"));
        assertTrue(config.getAllowedMethods().contains("PATCH"));

        assertTrue(config.getAllowedHeaders().contains("*"));

        assertTrue(config.getAllowCredentials());

        assertTrue(config.getExposedHeaders().contains("Authorization"));
    }
}
