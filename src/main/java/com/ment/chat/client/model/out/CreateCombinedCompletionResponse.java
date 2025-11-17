package com.ment.chat.client.model.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Schema(description = "The set of completions for a prompt across multiple LLM providers")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class CreateCombinedCompletionResponse {

    @Schema(description = "List of completions in the interaction", requiredMode = Schema.RequiredMode.REQUIRED)
    List<InteractionCompletion> interactionCompletions;

}
