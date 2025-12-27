package com.ment.chat.client.model.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

@Schema(description = "The list of messages in in the ongoing session")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class GetSessionMessagesResponse {

    @Schema(description = "List of messages for a given chatId in the ongoing session", requiredMode = Schema.RequiredMode.REQUIRED)
    List<Message> sessionMessages;

}
