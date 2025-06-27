package com.ment.chat.client.service;

import com.ment.chat.client.model.in.ConversationRequest;
import com.ment.chat.client.model.out.ConversationResponse;

public interface ChatService {

    ConversationResponse getExternalChatResponse(ConversationRequest conversationRequest);

    ConversationResponse getDockerChatResponse(ConversationRequest conversationRequest);

    ConversationResponse getInternalChatResponse(ConversationRequest conversationRequest);
}
