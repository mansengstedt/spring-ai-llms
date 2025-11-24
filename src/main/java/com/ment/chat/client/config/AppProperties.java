package com.ment.chat.client.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app")
@Validated
public record AppProperties(@Valid @NotNull Toggle toggle,
                            @Valid @NotNull Models models) {

    public record Toggle(@Valid @NotNull Boolean messageType,
                         @Valid @NotNull Boolean enableChat,
                         @Valid @NotNull Boolean enableChatHistory) {
    }

    public record Models(@Valid @NotNull Ollama ollama,
                         @Valid @NotNull OpenAi openAi,
                         @Valid @NotNull Anthropic anthropic,
                         @Valid @NotNull Docker docker) {

        public record Ollama(@Valid @NotNull String llmModelName,
                             @Valid @NotNull ApiConnection apiConnection) {
        }

        public record OpenAi(@Valid @NotNull String llmModelName,
                             @Valid @NotNull ApiConnection apiConnection) {
        }

        public record Anthropic(@Valid @NotNull String llmModelName,
                                @Valid @NotNull ApiConnection apiConnection) {
        }

        public record Docker(@Valid @NotNull String llmModelName,
                             @Valid @NotNull ApiConnection apiConnection) {
        }

        public record ApiConnection(@Valid @NotNull String url,
                                    @Valid @NotNull String key) {
        }
    }
}
