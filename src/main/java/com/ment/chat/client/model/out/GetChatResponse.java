package com.ment.chat.client.model.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Schema(description = "The stored responses of all requests in a chat")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class GetChatResponse {

    @Schema(description = "List of conversations", requiredMode = Schema.RequiredMode.REQUIRED)
    List<GetConversationResponse> conversations;

}
