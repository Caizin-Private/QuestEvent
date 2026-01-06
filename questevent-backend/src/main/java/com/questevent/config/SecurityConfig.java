package com.questevent.config;

import com.questevent.service.OAuthSuccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Optional;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CorsConfig corsConfig;
    private final OAuthSuccessService successHandler;

    @Autowired(required = false)
    private ClientRegistrationRepository clientRegistrationRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/login", "/logout-success",
                                "/swagger-ui/**", "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .anyRequest().authenticated()
                );

        // Conditionally configure OAuth2 login if repository is available
        if (clientRegistrationRepository != null) {
            http.oauth2Login(oauth -> oauth
                    .loginPage("/login")
                    .defaultSuccessUrl("/profile", true)
                    .successHandler(successHandler)
            );
            http.addFilterAfter(jwtAuthFilter, OAuth2LoginAuthenticationFilter.class);
        } else {
            // OAuth2 not configured, add JWT filter before form login filter
            http.addFilterBefore(jwtAuthFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        }

        http.logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/logout-success")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
        );

        return http.build();
    }
}
