package com.ment.chat.client.model.out;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ment.chat.client.config.LlmProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;

@Schema(title = "Requests Response")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class CreateConversationResponse {

    @Schema(description = "Id", requiredMode = Schema.RequiredMode.REQUIRED)
    String id;

    @Schema(description = "Answer", requiredMode = Schema.RequiredMode.REQUIRED)
    String answer;

    @Schema(description = "Used llm", example = "llama3", requiredMode = Schema.RequiredMode.REQUIRED)
    String llm;

    @Schema(description = "Llm provider", example = "OPENAI", requiredMode = Schema.RequiredMode.REQUIRED)
    LlmProvider llmProvider;

    @Schema(description = "Usage of tokens", example = "promptTokens=52, completionTokens=227, totalTokens=279", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String tokenUsage;

    @Schema(description = "execution time", requiredMode = Schema.RequiredMode.REQUIRED)
    Long executionTimeMs;

    @Schema(description = "Answer time of the request", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSxxx")
    OffsetDateTime answeredAt;

}
