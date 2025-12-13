package com.ment.chat.client.service;

import com.ment.chat.client.model.in.CreateCompletionsByProvidersRequest;
import com.ment.chat.client.model.in.CreateCompletionByProviderRequest;
import com.ment.chat.client.model.in.CreateCompletionsByAllProvidersRequest;
import com.ment.chat.client.model.out.CreateCompletionsByProvidersResponse;
import com.ment.chat.client.model.out.CreateCompletionByProviderResponse;
import com.ment.chat.client.model.out.GetChatResponse;
import com.ment.chat.client.model.out.GetInteractionResponse;
import com.ment.chat.client.model.out.GetInteractionsResponse;
import com.ment.chat.client.model.out.GetLlmProviderStatusResponse;

public interface ChatService {

    CreateCompletionByProviderResponse createCompletionByProvider(CreateCompletionByProviderRequest createCompletionByProviderRequest);

    CreateCompletionsByProvidersResponse createCompletionsByProviders(CreateCompletionsByProvidersRequest createCompletionsByProvidersRequest);

    CreateCompletionsByProvidersResponse createCompletionsByAllProviders(CreateCompletionsByAllProvidersRequest createCompletionsByAllProvidersRequest);

    GetInteractionResponse getInteractionByPromptId(String promptId);

    GetInteractionResponse getInteractionByCompletionId(String completionId);

    GetInteractionsResponse getInteractionsByCompletion(String partOfCompletion);

    GetChatResponse getChatByChatId(String chatId);

    GetChatResponse getChatByPrompt(String partOfPrompt);

    GetLlmProviderStatusResponse getAllProviderStatus();
}
