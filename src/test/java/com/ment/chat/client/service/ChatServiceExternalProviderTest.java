package com.ment.chat.client.service;

import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.model.enums.LlmStatus;
import com.ment.chat.client.model.in.CreateCompletionRequest;
import com.ment.chat.client.model.out.CreateCompletionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = {"test", "external_keys"})
@EnableAutoConfiguration()
@DirtiesContext
public class ChatServiceExternalProviderTest extends BaseChatServiceTest {

    @Autowired
    private ChatService chatService;

    @ParameterizedTest
    @MethodSource("externalProviders")
    void chatProviderCallOk(LlmProvider provider) {
        testProvider(chatService, provider);
    }

    @Test
    void statusOfProviders() {
        chatService.getAllProviderStatus().getStatusList()
                .forEach(status -> {
                    switch (status.getProvider()) {
                        case OLLAMA, DOCKER -> assertThat(status.getStatus()).isEqualTo(LlmStatus.AVAILABLE);
                        case OPENAI, ANTHROPIC -> assertThat(status.getStatus()).isEqualTo(LlmStatus.AVAILABLE);
                    }
                });
    }

}
