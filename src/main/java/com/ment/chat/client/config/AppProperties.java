package com.ment.chat.client.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app")
@Validated
public record AppProperties(@NotNull @Valid Toggle toggle,
                            @NotNull @Valid Models models) {


    public record Toggle(@NotNull Boolean messageType,
                         Boolean enableChat,
                         Boolean enableChatHistory) {
    }

    public record Models(@NotNull @Valid Ollama ollama,
                         @NotNull @Valid OpenAi openAi,
                         @NotNull @Valid Anthropic anthropic,
                         @NotNull @Valid Docker docker) {

        public record Ollama(@NotNull String llmModelName,
                             @NotNull @Valid ApiConnection apiConnection) {
        }

        public record OpenAi(@NotNull String llmModelName,
                             @NotNull @Valid ApiConnection apiConnection) {
        }

        public record Anthropic(@NotNull String llmModelName,
                                @NotNull @Valid ApiConnection apiConnection) {
        }

        public record Docker(@NotNull String llmModelName,
                             @NotNull @Valid ApiConnection apiConnection) {
        }

        public record ApiConnection(@NotNull String url,
                                    @NotNull @Valid String key) {
        }
    }
}
