package com.ment.chat.client.service;

import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.model.in.CreateCompletionByProviderRequest;
import com.ment.chat.client.model.in.CreateCompletionsByProvidersAggregateRequest;
import com.ment.chat.client.model.in.CreateCompletionsRequest;
import com.ment.chat.client.model.in.CreateCompletionsByProvidersRequest;
import com.ment.chat.client.model.out.CreateCompletionByProviderResponse;
import org.junit.jupiter.params.provider.Arguments;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseChatServiceTest {

    void testProvider(ChatService chatService, String prompt, LlmProvider provider) {
        var completion = chatService.createCompletionByProvider(createCompletionRequest(prompt, null, provider));

        assertThat(completion).isInstanceOf(CreateCompletionByProviderResponse.class);
        assertThat(completion.getInteractionCompletion().getCompletion()).isNotNull(); //provider dependent
        assertThat(completion.getInteractionCompletion().getLlm()).isNotEmpty(); //provider dependent
        assertThat(completion.getInteractionCompletion().getTokenUsage()).isNotEmpty(); //provider dependent
        assertThat(UUID.fromString(completion.getInteractionCompletion().getCompletionId())).isInstanceOf(UUID.class); //service dependent
        assertThat(completion.getInteractionCompletion().getLlmProvider()).isEqualTo(provider); //service dependent
        assertThat(completion.getInteractionCompletion().getCompletedAt()).isBefore(OffsetDateTime.now()); //service dependent
        assertThat(completion.getInteractionCompletion().getExecutionTimeMs()).isGreaterThan(0); //service dependent
    }

    Stream<Arguments> localProviders() {
        return Stream.of(
                Arguments.of(LlmProvider.OLLAMA),
                Arguments.of(LlmProvider.DOCKER)
        );
    }

    Stream<Arguments> externalProviders() {
        return Stream.of(
                Arguments.of(LlmProvider.OPENAI),
                Arguments.of(LlmProvider.ANTHROPIC)
        );
    }

    @SuppressWarnings("SameParameterValue")
    CreateCompletionsRequest createCompletionRequest(String prompt, String chatId) {
        return CreateCompletionsRequest.builder()
                .prompt(prompt)
                .chatId(chatId)
                .build();
    }

    CreateCompletionByProviderRequest createCompletionRequest(String prompt, String chatId, LlmProvider provider) {
        return CreateCompletionByProviderRequest.builder()
                .prompt(prompt)
                .chatId(chatId)
                .llmProvider(provider)
                .build();
    }

    CreateCompletionsByProvidersRequest createCompletionsByProvidersRequest(String prompt, EnumSet<LlmProvider> providers) {
        return CreateCompletionsByProvidersRequest.builder()
                .prompt(prompt)
                //.chatId(DEFAULT_CHAT_ID) is implicitly set
                .llmProviders(providers)
                .build();
    }

    CreateCompletionsByProvidersAggregateRequest createCompletionsByProvidersAggregateRequest(String prompt, EnumSet<LlmProvider> providers, LlmProvider aggregator) {
        return CreateCompletionsByProvidersAggregateRequest.builder()
                .prompt(prompt)
                //.chatId(DEFAULT_CHAT_ID) is implicitly set
                .llmProviders(providers)
                .llmAggregator(aggregator)
                .build();
    }
}
