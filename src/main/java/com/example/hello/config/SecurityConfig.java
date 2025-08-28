package com.example.hello.config;

import com.example.hello.security.CustomOAuth2UserService;
import java.util.Arrays;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      ObjectProvider<ClientRegistrationRepository> clientRegistrations,
      Environment env,
      CustomOAuth2UserService customOAuth2UserService)
      throws Exception {
    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/", "/login", "/error", "/css/**", "/js/**", "/images/**", "/favicon.ico")
                    .permitAll()
                    .requestMatchers("/actuator/health", "/actuator/info")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            ex -> ex.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login")))
        .headers(
            headers ->
                headers
                    .contentSecurityPolicy(
                        csp ->
                            csp.policyDirectives(
                                "default-src 'self'; img-src 'self' https: data:; style-src 'self' 'unsafe-inline'; script-src 'self'; frame-ancestors 'none'"))
                    .referrerPolicy(
                        ref ->
                            ref.policy(
                                org.springframework.security.web.header.writers
                                    .ReferrerPolicyHeaderWriter.ReferrerPolicy
                                    .STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .permissionsPolicy(pp -> pp.policy("geolocation=(), microphone=()")));

    // Enable OAuth2 login only if client registrations are configured (later phases)
    ClientRegistrationRepository repo = clientRegistrations.getIfAvailable();
    if (repo != null) {
      boolean devProfile = Arrays.asList(env.getActiveProfiles()).contains("dev");
      if (devProfile) {
        http.oauth2Login(
            oauth ->
                oauth
                    .redirectionEndpoint(redir -> redir.baseUri("/auth/callback/*"))
                    .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                    .failureUrl("/login?error")
                    .defaultSuccessUrl("/me", true));
      } else {
        http.oauth2Login(
            oauth ->
                oauth
                    .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                    .failureUrl("/login?error")
                    .defaultSuccessUrl("/me", true));
      }
    }

    // Default CSRF is enabled by Spring Security
    http.logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout"));

    return http.build();
  }
}
