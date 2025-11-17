package com.ment.chat.client.service;

import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.model.in.CreateCompletionRequest;
import com.ment.chat.client.model.out.CreateCombinedCompletionResponse;
import com.ment.chat.client.model.out.CreateCompletionResponse;
import com.ment.chat.client.model.out.GetChatResponse;
import com.ment.chat.client.model.out.GetInteractionResponse;
import com.ment.chat.client.model.out.GetLlmProviderStatusResponse;

public interface ChatService {

    CreateCompletionResponse createCompletion(CreateCompletionRequest completionRequest, LlmProvider llmProvider);

    CreateCombinedCompletionResponse createCombinedCompletion(CreateCompletionRequest completionRequest);

    GetInteractionResponse getInteraction(String promptId);

    GetChatResponse getChat(String chatId);

    GetLlmProviderStatusResponse getLlmProviderStatus();
}
