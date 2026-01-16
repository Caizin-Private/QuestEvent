package com.questevent.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigTest {

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void corsConfigurationSource_isRegistered() {
        assertThat(corsConfigurationSource).isNotNull();
    }

    @Test
    void corsConfiguration_containsExpectedSettings() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/users");
        request.addHeader("Origin", "http://localhost:3000");

        CorsConfiguration config =
                corsConfigurationSource.getCorsConfiguration(request);

        assertThat(config).isNotNull();

        assertThat(config.getAllowedOrigins()).contains(
                "http://localhost:8080",
                "http://localhost:3000",
                "http://localhost:3003",
                "https://www.questevent.online",
                "https://questevent.online"
        );

        assertThat(config.getAllowedMethods()).contains(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        );

        assertThat(config.getAllowedHeaders()).contains("*");
        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getExposedHeaders()).contains("Authorization");
    }
    @Test
    void preflightRequest_isAllowedForConfiguredOrigin() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.options("/api/users")
                                .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                )
                .andExpect(status().isOk());
    }

    @Test
    void corsRequest_fromAllowedOrigin_succeeds() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/")
                                .header(HttpHeaders.ORIGIN, "http://localhost:8080")
                )
                .andExpect(status().isOk());
    }
}
