package com.ment.chat.client.service;

import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.model.in.CreateCompletionsByProvidersAggregateRequest;
import com.ment.chat.client.model.in.CreateCompletionsByProvidersRequest;
import com.ment.chat.client.model.in.CreateCompletionByProviderRequest;
import com.ment.chat.client.model.in.CreateCompletionsRequest;
import com.ment.chat.client.model.out.CreateCompletionsByProvidersAggregateResponse;
import com.ment.chat.client.model.out.CreateCompletionsByProvidersResponse;
import com.ment.chat.client.model.out.CreateCompletionByProviderResponse;
import com.ment.chat.client.model.out.GetChatResponse;
import com.ment.chat.client.model.out.GetInteractionResponse;
import com.ment.chat.client.model.out.GetInteractionsResponse;
import com.ment.chat.client.model.out.GetLlmProvidersStatusResponse;
import com.ment.chat.client.model.out.GetSessionMessagesResponse;

public interface ChatService {

    CreateCompletionByProviderResponse createCompletionByProvider(CreateCompletionByProviderRequest createCompletionByProviderRequest);

    CreateCompletionsByProvidersResponse createCompletionsByProviders(CreateCompletionsByProvidersRequest createCompletionsByProvidersRequest);

    CreateCompletionsByProvidersAggregateResponse createCompletionsByProvidersAggregate(CreateCompletionsByProvidersAggregateRequest createCompletionsByProvidersAggregatorRequest);

    CreateCompletionsByProvidersResponse createCompletionsByAllProviders(CreateCompletionsRequest createCompletionsRequest);

    GetInteractionResponse getInteractionByPromptId(String promptId);

    GetInteractionResponse getInteractionByCompletionId(String completionId);

    GetInteractionsResponse getInteractionsByCompletion(String partOfCompletion);

    GetChatResponse getChatByChatId(String chatId);

    GetChatResponse getChatByPrompt(String partOfPrompt);

    GetLlmProvidersStatusResponse getAllProviderStatus();

    void clearSessionHistory(String chatId, LlmProvider provider);

    GetSessionMessagesResponse getSessionMessages(String chatId, LlmProvider provider);
}
