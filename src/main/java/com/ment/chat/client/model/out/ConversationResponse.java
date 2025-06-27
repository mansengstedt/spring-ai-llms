package com.ment.chat.client.model.out;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;

@Schema(title = "Prompt Response")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class ConversationResponse {

    @Schema(description = "Answer", requiredMode = Schema.RequiredMode.REQUIRED)
    String answer;

    @Schema(description = "Used llm", example = "llama3", requiredMode = Schema.RequiredMode.REQUIRED)
    String llm;

    @Schema(description = "Usage of tokens", example = "promptTokens=45, ...", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String tokenUsage;

    @Schema(description = "execution time", requiredMode = Schema.RequiredMode.REQUIRED)
    Long executionTimeMs;

    @Schema(description = "Answer date of the request", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSxxx")
    OffsetDateTime answeredAt;

}
