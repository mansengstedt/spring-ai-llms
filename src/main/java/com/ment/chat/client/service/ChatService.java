package com.ment.chat.client.service;

import com.ment.chat.client.model.in.CreateConversationRequest;
import com.ment.chat.client.model.out.*;

public interface ChatService {

    CreateConversationResponse getExternalChatResponse(CreateConversationRequest conversationRequest);

    CreateConversationResponse getDockerChatResponse(CreateConversationRequest conversationRequest);

    CreateConversationResponse getInternalChatResponse(CreateConversationRequest conversationRequest);

    CreateCombinedConversationResponse getCombinedChatResponse(CreateConversationRequest conversationRequest);

    GetConversationResponse getConversation(String requestId);

    GetChatResponse getChat(String chatId);

    GetChatServiceStatusResponse getChatServiceStatus();
}
