package com.ment.chat.client.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "spring.ai.vertex.ai.gemini")
@Validated
public record GeminiProperties(@NotNull String projectId,
                               @NotNull String location,
                               String apiEndpoint, //for testing purposes
                               String apiKey) //for testing purposes
{}
