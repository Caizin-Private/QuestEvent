package com.questevent.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OpenApiConfigTest {

    @Autowired
    private OpenAPI openAPI;

    @Test
    void openApiBeanShouldLoad() {
        assertNotNull(openAPI);
    }

    @Test
    void openApiInfoShouldBeConfigured() {

        assertEquals("QuestEvent API", openAPI.getInfo().getTitle());
        assertEquals("1.0.0", openAPI.getInfo().getVersion());
        assertTrue(openAPI.getInfo().getDescription().contains("QuestEvent"));
    }

    @Test
    void bearerSecuritySchemeShouldBeConfigured() {

        var schemes = openAPI.getComponents().getSecuritySchemes();

        assertNotNull(schemes);
        assertTrue(schemes.containsKey("bearerAuth"));

        SecurityScheme scheme = schemes.get("bearerAuth");

        assertEquals(SecurityScheme.Type.HTTP, scheme.getType());
        assertEquals("bearer", scheme.getScheme());
        assertEquals("JWT", scheme.getBearerFormat());
    }

    @Test
    void securityRequirementShouldBeAdded() {

        assertFalse(openAPI.getSecurity().isEmpty());
        assertTrue(
                openAPI.getSecurity()
                        .get(0)
                        .containsKey("bearerAuth")
        );
    }
}
