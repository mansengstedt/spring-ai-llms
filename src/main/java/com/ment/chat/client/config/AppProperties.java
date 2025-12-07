package com.ment.chat.client.config;

import com.ment.chat.client.model.enums.LlmProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Map;

@ConfigurationProperties(prefix = "app")
@Validated
public record AppProperties(@NotNull @Valid Toggle toggle,
                            @NotNull @NotEmpty @Valid Map<LlmProvider, ProviderModel> models) {


    public record Toggle(@NotNull Boolean messageType,
                         Boolean enableChat,
                         Boolean enableChatHistory) {
    }

    public record ProviderModel(@NotNull String llmModelName,
                                @NotNull @Valid ApiConnection apiConnection) {
    }

    public record ApiConnection(@NotNull String url,
                                @NotNull String key) {
    }
}
