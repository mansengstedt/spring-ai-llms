package com.ment.chat.client.model.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Schema(description = "The set of completions for a prompt across multiple LLM providers")
@Data //Value not possible since it makes the class final which stops inheritance of the subclasses needed
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class CreateCompletionsByProvidersResponse {

    @Schema(description = "List of completions in the interaction", requiredMode = Schema.RequiredMode.REQUIRED)
    List<InteractionCompletion> interactionCompletions;

}
