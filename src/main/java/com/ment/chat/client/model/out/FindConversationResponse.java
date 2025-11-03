package com.ment.chat.client.model.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Schema(title = "The found responses of request")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class FindConversationResponse {

    @Schema(description = "Prompt", requiredMode = Schema.RequiredMode.REQUIRED)
    String prompt;

    @Schema(description = "Responses of request", requiredMode = Schema.RequiredMode.REQUIRED)
    List<ConversationResponse> responses;

}
