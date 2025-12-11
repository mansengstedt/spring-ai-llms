package com.ment.chat.client.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """ 
        Enum representing providers of different Large Language Models (LLM).
        """)
public enum LlmProvider {
    OLLAMA,
    DOCKER,
    OPENAI,
    ANTHROPIC,
    GEMINI
}
