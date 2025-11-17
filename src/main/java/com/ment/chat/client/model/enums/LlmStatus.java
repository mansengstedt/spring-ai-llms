package com.ment.chat.client.model.enums;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        Enum representing the status, from client perspective, of a Large Language Model (LLM).
        """)
public enum LlmStatus {
    AVAILABLE,
    UNAVAILABLE
}
