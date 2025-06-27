package com.ment.chat.client.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ApiConnection {
    OPEN_AI("https://api.openai.com", "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"),
    DOCKER_AI("http://localhost:12434/engines", "NOT_NEEDED"),
    OLLAMA("http://localhost:11434", "NOT_NEEDED");

    private final String url;
    private final String key;
}
