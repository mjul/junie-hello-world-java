package com.example.hello.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.hello.domain.User;
import com.example.hello.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class UserRepositoryTest {

  @Autowired private UserRepository userRepository;

  @Test
  void saveAndFindByProviderAndExternalId() {
    User u = new User();
    u.setProvider("GITHUB");
    u.setExternalId("123");
    u.setUsername("octocat");
    u.setDisplayName("The Octocat");
    u.setEmail("octo@example.com");
    u.setAvatarUrl("https://avatars.example/octo.png");

    User saved = userRepository.save(u);
    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();

    Optional<User> found = userRepository.findByProviderAndExternalId("GITHUB", "123");
    assertThat(found).isPresent();
    assertThat(found.get().getUsername()).isEqualTo("octocat");
  }
}
