package com.ment.chat.client.model.in;


import com.ment.chat.client.model.annotation.ValidProviderSize;
import com.ment.chat.client.model.enums.LlmProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.EnumSet;

@Schema(description = "Interaction prompt specification specifying which providers to call")
@Value
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class CreateCompletionsByProvidersRequest extends CreateCompletionsRequest {

    @NotNull
    @ValidProviderSize
    @Schema(description = "Llm providers", example =
    """
    ["ANTHROPIC","OPENAI","GEMINI"]
    """,
    requiredMode = Schema.RequiredMode.REQUIRED)
    EnumSet<LlmProvider> llmProviders;

}
