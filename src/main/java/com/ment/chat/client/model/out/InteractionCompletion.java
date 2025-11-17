package com.ment.chat.client.model.out;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ment.chat.client.model.enums.LlmProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;

@Schema(description = "The completion of an interaction with parameters")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class InteractionCompletion implements Comparable<InteractionCompletion> {

    @Schema(description = "Completion Id", example = "2466e808-a176-4a7f-a09b-2d461ef033b5", requiredMode = Schema.RequiredMode.REQUIRED)
    String completionId;

    @Schema(description = "Completion of the interaction", example = "Trump is the present president of USA", requiredMode = Schema.RequiredMode.REQUIRED)
    String completion;

    @Schema(description = "Used llm", example = "llama3", requiredMode = Schema.RequiredMode.REQUIRED)
    String llm;

    @Schema(description = "Llm provider", example = "OPENAI", requiredMode = Schema.RequiredMode.REQUIRED)
    LlmProvider llmProvider;

    @Schema(description = "Usage of tokens", example = "promptTokens=52, completionTokens=227, totalTokens=279", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String tokenUsage;

    @Schema(description = "Execution time in ms", example = "199", requiredMode = Schema.RequiredMode.REQUIRED)
    Long executionTimeMs;

    @Schema(description = "Completion time of the interaction", example ="2025-11-14T16:50:01.115667+01:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSxxx")
    OffsetDateTime completedAt;

    @Override
    public int compareTo(InteractionCompletion other) {
        return completedAt.compareTo(other.getCompletedAt());
    }
}
