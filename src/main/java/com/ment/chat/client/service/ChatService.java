package com.ment.chat.client.service;

import com.ment.chat.client.config.LlmProvider;
import com.ment.chat.client.model.in.CreateConversationRequest;
import com.ment.chat.client.model.out.*;

public interface ChatService {

    CreateConversationResponse getChatResponse(CreateConversationRequest conversationRequest, LlmProvider llmProvider);

    CreateCombinedConversationResponse getCombinedChatResponse(CreateConversationRequest conversationRequest);

    GetConversationResponse getConversation(String requestId);

    GetChatResponse getChat(String chatId);

    GetChatServiceStatusResponse getChatServiceStatus();
}
