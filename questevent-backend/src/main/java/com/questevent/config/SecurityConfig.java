package com.questevent.config;

import com.questevent.repository.AllowedUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private AllowedUserRepository allowedUserRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/public").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .successHandler((request, response, authentication) -> {

                            OAuth2User user = (OAuth2User) authentication.getPrincipal();

                            // Fetch email from Azure attributes (different tenants return different keys)
                            String email = user.getAttribute("email");
                            if (email == null) email = user.getAttribute("preferred_username");
                            if (email == null) email = user.getAttribute("upn");

                            System.out.println("\n============================");
                            System.out.println("Azure Login Email = " + email);
                            System.out.println("============================\n");

                            if (!allowedUserRepository.findByEmail(email).isPresent()) {
                                System.out.println("User not allowed!");
                                response.sendRedirect("/access-denied");
                                return;
                            }

                            System.out.println("User allowed!");
                            response.sendRedirect("/home");
                        })
                )
                .logout(logout -> logout.logoutSuccessUrl("/").permitAll());

        return http.build();
    }
}
