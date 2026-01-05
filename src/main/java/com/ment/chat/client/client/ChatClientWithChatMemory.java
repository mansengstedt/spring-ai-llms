package com.ment.chat.client.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;

public record ChatClientWithChatMemory(ChatClient chatClient, ChatMemory chatMemory) {

}
