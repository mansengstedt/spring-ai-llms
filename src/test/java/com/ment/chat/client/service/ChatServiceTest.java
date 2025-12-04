package com.ment.chat.client.service;

import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.model.enums.LlmStatus;
import com.ment.chat.client.model.out.GetChatResponse;
import com.ment.chat.client.model.out.GetInteractionResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(value = {"test"})
@EnableAutoConfiguration()
@ComponentScan(basePackages = "com.ment.chat.client")
@DirtiesContext
public class ChatServiceTest extends BaseChatServiceTest {

    //will actually call the real providers
    @Autowired
    private ChatService chatService;

    @ParameterizedTest
    @MethodSource("localProviders")
    void chatProviderCallOk(LlmProvider provider) {
        testProvider(chatService, "Who is Elon musk?", provider);
    }

    @Test
    void chatProviderCombinedCallOk() {
        var completions = chatService.createCompletionsByAllProviders(createCompletionRequest("Who is Donald Trump?", null));

        assertThat(completions.getInteractionCompletions().size()).isEqualTo(4); //number of providers
        completions.getInteractionCompletions()
                .forEach(interaction -> {
                    if (interaction.getLlm() != null) {
                        // provider answered
                        assertThat(interaction.getCompletion()).isNotNull();
                    } else {
                        // provider did not answer
                        assertThat(interaction.getCompletion()).isNull();
                    }
                });
    }

    @ParameterizedTest
    @MethodSource("externalProviders")
    void chatProviderCallWithWrongPassword(LlmProvider provider) {
        assertThatThrownBy(() -> chatService.createCompletionByProvider(createCompletionRequest("Who is Donald Trump?", null, provider)))
                .isInstanceOf(NonTransientAiException.class)
                .satisfies(ex ->
                        assertThat(ex.getMessage())
                                .matches(msg ->
                                        msg.contains("authentication_error") ||
                                                msg.contains("invalid_request_error")
                                )
                );
    }

    @ParameterizedTest
    @MethodSource("localProviders")
    void chatProviderGetInteraction(LlmProvider provider) {
        var prompt = "Who is Donald Trump?";

        var completion = chatService.createCompletionByProvider(createCompletionRequest(prompt, null, provider));
        var interaction = chatService.getInteractionByPromptId(completion.getInteractionCompletion().getPromptId());

        assertThat(interaction).isInstanceOf(GetInteractionResponse.class);
        assertThat(completion.getInteractionCompletion().getPromptId()).isEqualTo(interaction.getInteractionPrompt().getPromptId());
        assertThat(interaction.getInteractionPrompt().getChatId()).isNull();
        assertThat(interaction.getInteractionPrompt().getPrompt()).isEqualTo(prompt);
        assertThat(interaction.getInteractionCompletions().size()).isEqualTo(1);
        interaction.getInteractionCompletions()
                .forEach(c -> {
                    assertThat(c.getPromptId()).isEqualTo(completion.getInteractionCompletion().getPromptId());
                    assertThat(c.getLlmProvider()).isEqualTo(provider);
                });
    }

    @Test
    void chatProviderGetChat() {
        var chatId = "test-chat-id";
        var prompt1 = "Who is Elon Musk?";
        var prompt2 = "Is he a friend of Donald Trump?";

        chatService.createCompletionByProvider(createCompletionRequest(prompt1, chatId, LlmProvider.OLLAMA));
        chatService.createCompletionByProvider(createCompletionRequest(prompt2, chatId, LlmProvider.OLLAMA));
        var response = chatService.getChatByChatId(chatId);

        assertThat(response).isInstanceOf(GetChatResponse.class);
        assertThat(response.getInteractions().size()).isEqualTo(2);
        response.getInteractions()
                .forEach(interactionResponse -> {
                    assertThat(interactionResponse.getInteractionPrompt().getChatId()).isEqualTo(chatId);
                    assertThat(interactionResponse.getInteractionCompletions().size()).isEqualTo(1);
                    if (!interactionResponse.getInteractionPrompt().getPrompt().equals(prompt1) && !interactionResponse.getInteractionPrompt().getPrompt().equals(prompt2)) {
                        throw new AssertionError("Unexpected prompt in chat interactions: " + interactionResponse.getInteractionPrompt().getPrompt());
                    }
                });
    }

    @Test
    void statusOfProviders() {
        chatService.getAllProviderStatus().getStatusList()
                .forEach(status -> {
                    switch (status.getProvider()) {
                        case OLLAMA, DOCKER -> assertThat(status.getStatus()).isEqualTo(LlmStatus.AVAILABLE);
                        case OPENAI, ANTHROPIC -> assertThat(status.getStatus()).isEqualTo(LlmStatus.UNAVAILABLE);
                    }
                });
    }


}
