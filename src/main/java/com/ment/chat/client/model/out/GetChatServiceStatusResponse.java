package com.ment.chat.client.model.out;

import com.ment.chat.client.config.LlmProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Schema(description = "Status of all connected LLMs")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class GetChatServiceStatusResponse {

    @Schema(description = "list of LLM status", requiredMode = Schema.RequiredMode.REQUIRED)
    List<ChatServiceStatus> statusList;

    @Value
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Jacksonized
    public static class ChatServiceStatus {

        @Schema(description = "name of LLM", example = "OLLAMA", requiredMode = Schema.RequiredMode.REQUIRED)
        String llm;

        @Schema(description = "status of LLM", example = "gpt-5", requiredMode = Schema.RequiredMode.REQUIRED)
        LlmStatus status;

        @Schema(description = "provider of LLM", example = "AVAILABLE", requiredMode = Schema.RequiredMode.REQUIRED)
        LlmProvider provider;
    }

    public enum LlmStatus {
        AVAILABLE,
        UNAVAILABLE
    }
}
