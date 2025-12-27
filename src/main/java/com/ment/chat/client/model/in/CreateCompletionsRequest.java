package com.ment.chat.client.model.in;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Schema(description = "Interaction prompt specification using style and chatId for memory")
@Data //Value not possible since it makes the class final which stops inheritance of the subclasses needed
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class CreateCompletionsRequest {

    public static final String DEFAULT_CHAT_ID = "default";

    @NotBlank
    @Size(min = 2, max = 40000, message = "Size must be between 2 and 40000 characters")
    @Schema(description = "Prompt as free text", example = "Who is the present president of USA", requiredMode = Schema.RequiredMode.REQUIRED)
    String prompt;

    @Size(max = 100, message = "Size must be max 100 characters")
    @Schema(description = "The style of the completion", example = "funny like comedian", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String style;

    @Size(max = 128, message = "Size must be max 128 characters")
    @Builder.Default
    @Schema(description = "The chat id of the interaction used for memory", defaultValue = DEFAULT_CHAT_ID, example = "myId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String chatId = DEFAULT_CHAT_ID;

    public String createPrompt() {
        return style != null && !style.isEmpty()
                ? prompt + ". " + style
                : prompt;
    }

}
