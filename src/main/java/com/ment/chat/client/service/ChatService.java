package com.ment.chat.client.service;

import com.ment.chat.client.model.in.CreateCompletionByProviderRequest;
import com.ment.chat.client.model.in.CreateCompletionRequest;
import com.ment.chat.client.model.out.CreateCombinedCompletionResponse;
import com.ment.chat.client.model.out.CreateCompletionResponse;
import com.ment.chat.client.model.out.GetChatResponse;
import com.ment.chat.client.model.out.GetInteractionResponse;
import com.ment.chat.client.model.out.GetInteractionsResponse;
import com.ment.chat.client.model.out.GetLlmProviderStatusResponse;

public interface ChatService {

    CreateCompletionResponse createCompletionByProvider(CreateCompletionByProviderRequest completionRequest);

    CreateCombinedCompletionResponse createCompletionsByAllProviders(CreateCompletionRequest completionRequest);

    GetInteractionResponse getInteractionByPromptId(String promptId);

    GetInteractionsResponse getInteractionsByCompletion(String partOfCompletion);

    GetChatResponse getChatByChatId(String chatId);

    GetChatResponse getChatByPrompt(String partOfPrompt);

    GetLlmProviderStatusResponse getAllProviderStatus();
}
