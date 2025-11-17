package com.ment.chat.client.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """ 
        Enum representing different Large Language Model (LLM) providers.
        """)
public enum LlmProvider {
    OPENAI,
    ANTHROPIC,
    OLLAMA,
    DOCKER
}
