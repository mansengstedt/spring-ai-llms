package com.ment.chat.client.model.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Schema(description = "The created interaction completion of an interaction prompt")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class CreateCompletionResponse {

    @Schema(description = "The created interaction completion", requiredMode = Schema.RequiredMode.REQUIRED)
    InteractionCompletion interactionCompletion;

}
