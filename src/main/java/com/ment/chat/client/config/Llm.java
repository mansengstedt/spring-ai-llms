package com.ment.chat.client.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.ment.chat.client.config.Systems.HELPFUL_SYSTEM_NO_LIMIT;
import static com.ment.chat.client.config.Systems.HELPFUL_SYSTEM_MAX_100;
import static com.ment.chat.client.config.Systems.HELPFUL_SYSTEM_MAX_50;

@RequiredArgsConstructor
@Getter

public enum Llm {
    LLM_OPEN_AI_GPT_4O("gpt-4o", 0.7d, HELPFUL_SYSTEM_NO_LIMIT, false),
    LLM_OPEN_AI_GPT_4O_MINI("gpt-4o-mini", 0.7d, HELPFUL_SYSTEM_MAX_100, false),
    LLM_OPEN_AI_GPT_41_NANO("gpt-4.1-nano", 0.7d, HELPFUL_SYSTEM_MAX_50, false),
    LLM_OPEN_AI_GPT_O4_MINI("o4-mini", 1.0d, HELPFUL_SYSTEM_MAX_100, false),
    LLM_OPEN_AI_GPT_5("gpt-5", 1.0d, HELPFUL_SYSTEM_MAX_100, false),
    LLM_OPEN_AI_GPT_5_MINI("gpt-5-mini", 1.0d, HELPFUL_SYSTEM_MAX_100, false),
    LLM_OPEN_AI_GPT_5_NANO("gpt-5-nano", 1.0d, HELPFUL_SYSTEM_MAX_50, false),

    LLM_ANTHROPIC_CLAUDE_4("claude-sonnet-4-20250514", 0.7d, HELPFUL_SYSTEM_NO_LIMIT, false),
    LLM_ANTHROPIC_CLAUDE_4_5("claude-sonnet-4-5-20250929", 0.8d, HELPFUL_SYSTEM_NO_LIMIT, false),

    //Gemma 3 causes exception: Conversation roles must alternate user/assistant/user/assistant/
    LLM_DOCKER_GEMMA_3("ai/gemma3", 0.7d, HELPFUL_SYSTEM_NO_LIMIT, true),
    LLM_DOCKER_DEEPSEEK_R1("ai/deepseek-r1-distill-llama", 0.7d, HELPFUL_SYSTEM_NO_LIMIT, false),

    LLM_OLLAMA_LLAMA_3("llama3", 0.7d, HELPFUL_SYSTEM_NO_LIMIT, false),
    LLM_OLLAMA_QWEN_3("qwen3", 0.7d, HELPFUL_SYSTEM_NO_LIMIT, false),
    LLM_OLLAMA_GEMMA_3("gemma3", 0.7d, HELPFUL_SYSTEM_NO_LIMIT, false);

    private final String name;
    private final Double temperature;
    private final String  system;
    private final Boolean alternateRoles;

}
