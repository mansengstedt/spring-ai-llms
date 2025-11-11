package com.ment.chat.client.model.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Schema(title = "The stored responses of a request")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class GetConversationResponse {

    @Schema(description = "Request", requiredMode = Schema.RequiredMode.REQUIRED)
    ConversationRequest request;

    @Schema(description = "Responses of request", requiredMode = Schema.RequiredMode.REQUIRED)
    List<CreateConversationResponse> responses;

}
