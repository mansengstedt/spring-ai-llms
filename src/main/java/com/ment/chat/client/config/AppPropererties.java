package com.ment.chat.client.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app")
@Validated
public record AppPropererties(@NotNull Toggle toggle, @NotNull Models models) {

    public record Toggle(@NotNull Boolean messageType,
                         @NotNull Boolean enableChat,
                          @NotNull Boolean enableChatHistory) {
    }

    public record Models(@NotNull Internal internal, @NotNull External external, @NotNull Docker docker) {
        public record Internal(@NotNull String llmModelName, @NotNull ApiConnection apiConnection) {
        }
        public record External(@NotNull String llmModelName, @NotNull ApiConnection apiConnection) {
        }
        public record Docker(@NotNull String llmModelName, @NotNull ApiConnection apiConnection) {
        }

        public record ApiConnection(@NotNull String url, @NotNull String key) {
        }
    }
}
