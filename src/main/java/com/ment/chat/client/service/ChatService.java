package com.ment.chat.client.service;

import com.ment.chat.client.model.in.CreateConversationRequest;
import com.ment.chat.client.model.out.CreateConversationResponse;
import com.ment.chat.client.model.out.GetChatResponse;
import com.ment.chat.client.model.out.GetConversationResponse;

public interface ChatService {

    CreateConversationResponse getExternalChatResponse(CreateConversationRequest conversationRequest);

    CreateConversationResponse getDockerChatResponse(CreateConversationRequest conversationRequest);

    CreateConversationResponse getInternalChatResponse(CreateConversationRequest conversationRequest);

    CreateConversationResponse getCombinedChatResponse(CreateConversationRequest conversationRequest);

    GetConversationResponse getConversation(String requestId);

    GetChatResponse getChat(String chatId);
}
