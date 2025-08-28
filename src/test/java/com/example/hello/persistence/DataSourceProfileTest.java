package com.example.hello.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Connection;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class DataSourceProfileTest {

    @Autowired
    DataSource dataSource;

    @Test
    void testProfileUsesH2InMemoryDatabase() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            String url = conn.getMetaData().getURL();
            assertThat(url).isNotNull();
            assertThat(url).contains("jdbc:h2:");
        }
    }
}
