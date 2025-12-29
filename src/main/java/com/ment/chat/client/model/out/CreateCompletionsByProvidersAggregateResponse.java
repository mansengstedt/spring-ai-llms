package com.ment.chat.client.model.out;

import com.ment.chat.client.model.in.CreateCompletionByProviderRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Schema(description = "The set of completions for a prompt across multiple LLM providers together with the aggregate summary")
@Value
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class CreateCompletionsByProvidersAggregateResponse extends CreateCompletionsByProvidersResponse {

    @Schema(description = "Aggregate request from returned completions", requiredMode = Schema.RequiredMode.REQUIRED)
    CreateCompletionByProviderRequest aggregateRequest;

    @Schema(description = "Aggregate summary from the aggregate request used in the interaction", requiredMode = Schema.RequiredMode.REQUIRED)
    InteractionCompletion aggregateSummary;

}
