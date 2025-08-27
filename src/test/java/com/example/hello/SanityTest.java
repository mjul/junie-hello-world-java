package com.example.hello;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SanityTest {

    @Test
    void sanity() {
        assertThat(2 + 2).isEqualTo(4);
    }
}
