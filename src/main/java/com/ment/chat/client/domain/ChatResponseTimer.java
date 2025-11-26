package com.ment.chat.client.domain;

import org.springframework.ai.chat.model.ChatResponse;

public record ChatResponseTimer(ChatResponse chatResponse, Long executionTimeMs) {
}
