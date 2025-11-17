package com.ment.chat.client.model.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Schema(description = "The stored completions of a prompt")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class GetInteractionResponse implements Comparable<GetInteractionResponse> {

    @Schema(description = "Original prompt of interaction", requiredMode = Schema.RequiredMode.REQUIRED)
    InteractionPrompt interactionPrompt;

    @Schema(description = "Completions of the prompt of interaction", requiredMode = Schema.RequiredMode.REQUIRED)
    List<InteractionCompletion> interactionCompletions;

    @Override
    public int compareTo(GetInteractionResponse other) {
        return this.interactionPrompt.getPromptedAt().compareTo(other.interactionPrompt.getPromptedAt());
    }

}
