package com.ment.chat.client.model.out;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;

@Schema(description = "The prompt of an interaction with parameters")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class InteractionPrompt {

    @Schema(description = "Prompt Id", example = "f3ccfb64-95d7-4e6a-a9ef-1455a0f81f6e", requiredMode = Schema.RequiredMode.REQUIRED)
    String promptId;

    @Schema(description = "Session Id", example = "1767031505585372000 (represents an instant with nanoseconds)", requiredMode = Schema.RequiredMode.REQUIRED)
    String sessionId;

    @Schema(description = "Interaction prompt", example = "who is presently the best male golf player in Sweden", requiredMode = Schema.RequiredMode.REQUIRED)
    String prompt;

    @Schema(description = "Chat Id", example = "swedish-golf-player", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String chatId;

    @Schema(description = "Prompt time of the interaction", example = "2025-11-14T16:50:01.115667+01:00", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSxxx")
    OffsetDateTime promptedAt;

}
