package com.ment.chat.client.model.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Schema(description = "The Combined Request Responses")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class CreateCombinedConversationResponse {

    @Schema(description = "List of one conversation responses", requiredMode = Schema.RequiredMode.REQUIRED)
    List<CreateConversationResponse> conversationResponses;

}
