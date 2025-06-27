package com.ment.chat.client.model.in;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Schema(title = "Prompt Request")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class ConversationRequest {

    @Schema(description = "prompt as free text", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    String prompt;

    @Schema(description = "the style of the response", example = "funny like comedian", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String style;

    @Schema(description = "the id of the conversation used for memory", example = "myId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String chatId;

    public String createPrompt() {
        return style != null && !style.isEmpty()
                ? prompt + ". " + style
                : prompt;
    }

}
