package com.ment.chat.client.model.out;

import com.ment.chat.client.config.LlmProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Schema(title = "Status of all connected LLMs")
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

        @Schema(description = "name of LLM", requiredMode = Schema.RequiredMode.REQUIRED)
        String llm;

        @Schema(description = "status of LLM", requiredMode = Schema.RequiredMode.REQUIRED)
        LlmStatus status;

        @Schema(description = "provider of LLM", requiredMode = Schema.RequiredMode.REQUIRED)
        LlmProvider provider;
    }

    public enum LlmStatus {
        AVAILABLE,
        UNAVAILABLE
    }
}
