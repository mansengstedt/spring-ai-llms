package com.ment.chat.client.model.in;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @NotBlank
    @Size(min = 2, max = 40000, message = "Size must be between 2 and 40000 characters")
    @Schema(description = "prompt as free text", requiredMode = Schema.RequiredMode.REQUIRED)
    String prompt;

    @Size(max = 100, message = "Size must be max 100 characters")
    @Schema(description = "the style of the response", example = "funny like comedian", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String style;

    @Size(max = 128, message = "Size must be max 128 characters")
    @Schema(description = "the id of the conversation used for memory", example = "myId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String chatId;

    public String createPrompt() {
        return style != null && !style.isEmpty()
                ? prompt + ". " + style
                : prompt;
    }

}
