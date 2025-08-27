package com.example.hello.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                  ObjectProvider<ClientRegistrationRepository> clientRegistrations,
                                                  Environment env) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/error", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
            );

        // Enable OAuth2 login only if client registrations are configured (later phases)
        ClientRegistrationRepository repo = clientRegistrations.getIfAvailable();
        if (repo != null) {
            boolean devProfile = Arrays.asList(env.getActiveProfiles()).contains("dev");
            if (devProfile) {
                http.oauth2Login(oauth -> oauth
                    .redirectionEndpoint(redir -> redir.baseUri("/auth/callback/*"))
                );
            } else {
                http.oauth2Login(Customizer.withDefaults());
            }
        }

        // Default CSRF is enabled by Spring Security
        http.logout(logout -> logout
            .logoutUrl("/logout")
            .logoutSuccessUrl("/login?logout")
        );

        return http.build();
    }
}
