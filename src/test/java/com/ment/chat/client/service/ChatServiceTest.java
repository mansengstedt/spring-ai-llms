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

import java.util.EnumSet;

import static com.ment.chat.client.model.enums.LlmProvider.DOCKER;
import static com.ment.chat.client.model.enums.LlmProvider.OLLAMA;
import static com.ment.chat.client.model.in.CreateCompletionsRequest.DEFAULT_CHAT_ID;
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

        assertThat(completions.getInteractionCompletions().size()).isEqualTo(LlmProvider.values().length); //number of providers
        completions.getInteractionCompletions()
                .forEach(interaction -> {
                    if (interaction.getLlm() != null) {
                        // provider answered
                        assertThat(interaction.getCompletion()).isNotNull();
                    } else {
                        // the provider did not answer
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
    void chatProviderGetCompletionsByProviders() {
        var prompt1 = "Who is Elon Musk?";
        var providerSet = EnumSet.of(OLLAMA, DOCKER);

        var completionByProvider = chatService.createCompletionsByProviders(createCompletionsByProvidersRequest(prompt1, providerSet));
        var response1 = chatService.getInteractionByPromptId(completionByProvider.getInteractionCompletions().getFirst().getPromptId());
        var response2 = chatService.getInteractionByCompletionId(completionByProvider.getInteractionCompletions().getFirst().getCompletionId());

        assertThat(completionByProvider.getInteractionCompletions().size()).isEqualTo(providerSet.size());
        assertThat(completionByProvider.getInteractionCompletions().getFirst().getPromptId()).isEqualTo(completionByProvider.getInteractionCompletions().get(1).getPromptId());
        assertThat(completionByProvider.getInteractionCompletions().getFirst().getCompletionId()).isNotEqualTo(completionByProvider.getInteractionCompletions().get(1).getCompletionId());

        assertThat(response1.getInteractionPrompt()).isEqualTo(response2.getInteractionPrompt());
        assertThat(response1.getInteractionPrompt().getPrompt()).isEqualTo(prompt1);
        assertThat(response1.getInteractionPrompt().getChatId()).isEqualTo(DEFAULT_CHAT_ID);
        assertThat(response1.getInteractionCompletions().size()).isEqualTo(providerSet.size());
        assertThat(response2.getInteractionCompletions().size()).isEqualTo(1);

        assertThat(completionByProvider.getInteractionCompletions()).containsExactlyInAnyOrderElementsOf(response1.getInteractionCompletions());
        response1.getInteractionCompletions()
                .forEach(completion ->
                    assertThat(completion.getCompletion()).isNotNull());
    }

    @Test
    void chatProviderGetCompletionsByProvidersAggregate() {
        var prompt1 = "Who is Elon Musk?";
        var providerSet = EnumSet.of(OLLAMA, DOCKER);
        var aggregator = OLLAMA;

        var completionByProviderAggregate = chatService.createCompletionsByProvidersAggregate(createCompletionsByProvidersAggregateRequest(prompt1, providerSet, aggregator));
        var response1 = chatService.getInteractionByPromptId(completionByProviderAggregate.getInteractionCompletions().getFirst().getPromptId());
        var response2 = chatService.getInteractionByCompletionId(completionByProviderAggregate.getInteractionCompletions().getFirst().getCompletionId());

        assertThat(completionByProviderAggregate.getAggregateRequest()).isNotNull();
        assertThat(completionByProviderAggregate.getAggregateRequest().getChatId()).isEqualTo(response1.getInteractionPrompt().getChatId());
        assertThat(completionByProviderAggregate.getAggregateRequest().getLlmProvider()).isEqualTo(aggregator);
        assertThat(completionByProviderAggregate.getAggregateSummary().getPromptId()).isNotNull();

        assertThat(completionByProviderAggregate.getInteractionCompletions().size()).isEqualTo(providerSet.size());
        assertThat(completionByProviderAggregate.getInteractionCompletions().getFirst().getPromptId()).isEqualTo(completionByProviderAggregate.getInteractionCompletions().get(1).getPromptId());
        assertThat(completionByProviderAggregate.getInteractionCompletions().getFirst().getCompletionId()).isNotEqualTo(completionByProviderAggregate.getInteractionCompletions().get(1).getCompletionId());

        assertThat(response1.getInteractionPrompt()).isEqualTo(response2.getInteractionPrompt());
        assertThat(response1.getInteractionPrompt().getPrompt()).isEqualTo(prompt1);
        assertThat(response1.getInteractionPrompt().getChatId()).isEqualTo(DEFAULT_CHAT_ID);
        assertThat(response1.getInteractionCompletions().size()).isEqualTo(providerSet.size());
        assertThat(response2.getInteractionCompletions().size()).isEqualTo(1);

        assertThat(completionByProviderAggregate.getInteractionCompletions()).containsExactlyInAnyOrderElementsOf(response1.getInteractionCompletions());
        response1.getInteractionCompletions()
                .forEach(completion ->
                        assertThat(completion.getCompletion()).isNotNull());
    }

    @Test
    void chatProviderGetPromptById() {
        var chatId = "test-chat-id";
        var prompt1 = "Who is Elon Musk?";
        var prompt2 = "Is he a friend of Donald Trump?";

        chatService.createCompletionByProvider(createCompletionRequest(prompt1, chatId, OLLAMA));
        chatService.createCompletionByProvider(createCompletionRequest(prompt2, chatId, OLLAMA));
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
        chatService.getAllProviderStatus().getLlmProviderStatusList()
                .forEach(status -> {
                    switch (status.getProvider()) {
                        case OLLAMA, DOCKER -> assertThat(status.getStatus()).isEqualTo(LlmStatus.AVAILABLE);
                        case OPENAI, ANTHROPIC -> assertThat(status.getStatus()).isEqualTo(LlmStatus.UNAVAILABLE);
                    }
                });
    }


}
