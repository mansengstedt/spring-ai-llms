package com.ment.chat.client.model.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.springframework.ai.chat.messages.MessageType;

@Schema(description = "A uniform message that covers all MessageTypes")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class UniformMessage {

    String content;
    MessageType messageType;

}
