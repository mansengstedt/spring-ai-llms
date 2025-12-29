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

@Schema(description = "Interaction prompt specification specifying which providers to call")
@Value
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class CreateCompletionsByProvidersAggregateRequest extends CreateCompletionsByProvidersRequest {

    @NotNull
    @Schema(description = "Llm aggregator", example = "OPENAI", requiredMode = Schema.RequiredMode.REQUIRED)
    LlmProvider llmAggregator;

}
