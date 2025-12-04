package com.ment.chat.client.model.in;


import com.ment.chat.client.model.enums.LlmProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

@Schema(description = "Interaction prompt specification using style and chatId for memory for a given provider")
@Value
@SuperBuilder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class CreateCompletionByProviderRequest extends CreateCompletionRequest {

    @NotNull
    @Schema(description = "Llm provider", example = "OPENAI", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    LlmProvider llmProvider;

}
