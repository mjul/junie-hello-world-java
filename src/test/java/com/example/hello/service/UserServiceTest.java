package com.example.hello.service;

import com.example.hello.domain.User;
import com.example.hello.repository.UserRepository;
import com.example.hello.security.ProviderProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(UserService.class)
class UserServiceTest {

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserService userService;

    @Test
    void createsNewUserAndSetsTimestamps() {
        ProviderProfile profile = new ProviderProfile("GITHUB", "42", "octocat", "Octo Cat", "octo@example.com", "http://avatar");
        User u = userService.getOrCreateFromProviderProfile(profile);
        assertThat(u.getId()).isNotNull();
        assertThat(u.getCreatedAt()).isNotNull();
        assertThat(u.getUpdatedAt()).isNotNull();
        assertThat(u.getLastLoginAt()).isNotNull();
        assertThat(u.getProvider()).isEqualTo("GITHUB");
        assertThat(u.getExternalId()).isEqualTo("42");
        assertThat(u.getUsername()).isEqualTo("octocat");
        assertThat(u.getDisplayName()).isEqualTo("Octo Cat");
        assertThat(u.getEmail()).isEqualTo("octo@example.com");
        assertThat(u.getAvatarUrl()).isEqualTo("http://avatar");
    }

    @Test
    void updatesExistingUserChangedFieldsAndLastLogin() throws InterruptedException {
        ProviderProfile profile = new ProviderProfile("GITHUB", "100", "login1", "Name 1", "e1@example.com", null);
        User created = userService.getOrCreateFromProviderProfile(profile);
        Instant firstLogin = created.getLastLoginAt();
        assertThat(firstLogin).isNotNull();

        // simulate later login with different details
        Thread.sleep(5); // ensure timestamp moves forward
        ProviderProfile changed = new ProviderProfile("GITHUB", "100", "login2", "Name 2", "e2@example.com", "http://a2");
        User updated = userService.getOrCreateFromProviderProfile(changed);

        assertThat(updated.getId()).isEqualTo(created.getId());
        assertThat(updated.getUsername()).isEqualTo("login2");
        assertThat(updated.getDisplayName()).isEqualTo("Name 2");
        assertThat(updated.getEmail()).isEqualTo("e2@example.com");
        assertThat(updated.getAvatarUrl()).isEqualTo("http://a2");
        assertThat(updated.getLastLoginAt()).isAfter(firstLogin);
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(created.getUpdatedAt());
    }
}
