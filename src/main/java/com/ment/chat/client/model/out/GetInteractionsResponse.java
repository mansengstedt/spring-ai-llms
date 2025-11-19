package com.ment.chat.client.model.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Schema(description = "The stored interactions with a common text in completions")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class GetInteractionsResponse {

    @Schema(description = "List of interaction", requiredMode = Schema.RequiredMode.REQUIRED)
    List<GetInteractionResponse> interactions;

}
