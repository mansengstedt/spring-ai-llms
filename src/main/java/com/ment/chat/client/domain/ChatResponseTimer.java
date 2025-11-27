package com.ment.chat.client.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.ai.chat.model.ChatResponse;

public record ChatResponseTimer(@Valid @NotNull ChatResponse chatResponse, Long executionTimeMs) {
}
