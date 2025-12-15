package com.ment.chat.client.model.out;

import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.model.enums.LlmStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Schema(description = "Status of all the provided LLMs")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class GetLlmProvidersStatusResponse {

    @Schema(description = "List of LLM provider status", requiredMode = Schema.RequiredMode.REQUIRED)
    List<LlmProviderStatus> llmProviderStatusList;

    @Value
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Jacksonized
    public static class LlmProviderStatus {

        @Schema(description = "Name of LLM", example = "gpt-5", requiredMode = Schema.RequiredMode.REQUIRED)
        String llm;

        @Schema(description = "Status of LLM", example = "AVAILABLE", requiredMode = Schema.RequiredMode.REQUIRED)
        LlmStatus status;

        @Schema(description = "Provider of LLM", example = "OPENAI", requiredMode = Schema.RequiredMode.REQUIRED)
        LlmProvider provider;
    }

}
