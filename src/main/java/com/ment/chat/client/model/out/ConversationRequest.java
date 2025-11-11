package com.ment.chat.client.model.out;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Schema(title = "Original Request")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class ConversationRequest {

    @Schema(description = "Id", requiredMode = Schema.RequiredMode.REQUIRED)
    String id;

    @Schema(description = "Prompt", example="who is presently the best male golf player in Sweden", requiredMode = Schema.RequiredMode.REQUIRED)
    String prompt;

    @Schema(description = "chatId", example="swedish-golf-player", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String chatId;


}
