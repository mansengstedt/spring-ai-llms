package com.ment.chat.client.model.in;


import com.ment.chat.client.model.enums.LlmProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

/**
 * Value can not be used for using equals/to string in superclass, use Data instead.
 */
@Schema(description = "Interaction prompt specification using style and chatId for memory for a given provider")
@Value
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class CreateCompletionByProviderRequest extends CreateCompletionRequest {

    @NotNull
    @Schema(description = "Llm provider", example = "OPENAI", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    LlmProvider llmProvider;

}
