package com.example.hello.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DataSourceProfileTest {

  @Autowired DataSource dataSource;

  @Test
  void testProfileUsesH2InMemoryDatabase() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      String url = conn.getMetaData().getURL();
      assertThat(url).isNotNull();
      assertThat(url).contains("jdbc:h2:");
    }
  }
}
