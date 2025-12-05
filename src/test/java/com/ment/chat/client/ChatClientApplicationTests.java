package com.ment.chat.client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ChatClientApplicationTests {

    @Test
    void contextLoads() {
        // Context loads successfully if this test passes
        assertTrue(true, "Application context should load");
    }

}
