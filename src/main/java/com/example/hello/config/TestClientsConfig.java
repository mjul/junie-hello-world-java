package com.example.hello.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
@Profile("test")
public class TestClientsConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration github = ClientRegistration.withRegistrationId("github")
                .clientId("test-client-id")
                .clientSecret("test-secret")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("read:user", "user:email")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("id")
                .clientName("GitHub")
                .build();
        return new InMemoryClientRegistrationRepository(github);
    }
}
