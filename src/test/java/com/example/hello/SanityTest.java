package com.example.hello;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SanityTest {

  @Test
  void sanity() {
    assertThat(2 + 2).isEqualTo(4);
  }
}
