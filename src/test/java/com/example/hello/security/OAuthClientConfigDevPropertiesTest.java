package com.example.hello.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles({"dev", "test"})
@SpringBootTest
class OAuthClientConfigDevPropertiesTest {

  @Autowired Environment env;

  @Test
  void githubRedirectUriOverrideIsConfiguredInDevProfile() {
    String redirect =
        env.getProperty("spring.security.oauth2.client.registration.github.redirect-uri");
    assertThat(redirect).isEqualTo("{baseUrl}/auth/callback/{registrationId}");
  }
}
