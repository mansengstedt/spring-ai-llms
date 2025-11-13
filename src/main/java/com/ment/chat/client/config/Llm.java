package com.ment.chat.client.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.ment.chat.client.config.LlmProvider.ANTHROPIC;
import static com.ment.chat.client.config.LlmProvider.DOCKER;
import static com.ment.chat.client.config.LlmProvider.OLLAMA;
import static com.ment.chat.client.config.LlmProvider.OPENAI;
import static com.ment.chat.client.config.Systems.HELPFUL_SYSTEM_NO_LIMIT;
import static com.ment.chat.client.config.Systems.HELPFUL_SYSTEM_MAX_100;
import static com.ment.chat.client.config.Systems.HELPFUL_SYSTEM_MAX_50;

@RequiredArgsConstructor
@Getter

public enum Llm {
    LLM_OPEN_AI_GPT_4O(OPENAI,"gpt-4o", 0.7d, HELPFUL_SYSTEM_NO_LIMIT, false),
    LLM_OPEN_AI_GPT_4O_MINI(OPENAI,"gpt-4o-mini", 0.7d, HELPFUL_SYSTEM_MAX_100, false),
    LLM_OPEN_AI_GPT_41_NANO(OPENAI,"gpt-4.1-nano", 0.7d, HELPFUL_SYSTEM_MAX_50, false),
    LLM_OPEN_AI_GPT_O4_MINI(OPENAI,"o4-mini", 1.0d, HELPFUL_SYSTEM_MAX_100, false),
    LLM_OPEN_AI_GPT_5(OPENAI,"gpt-5", 1.0d, HELPFUL_SYSTEM_MAX_100, false),
    LLM_OPEN_AI_GPT_5_MINI(OPENAI,"gpt-5-mini", 1.0d, HELPFUL_SYSTEM_MAX_100, false),
    LLM_OPEN_AI_GPT_5_NANO(OPENAI,"gpt-5-nano", 1.0d, HELPFUL_SYSTEM_MAX_50, false),

    LLM_ANTHROPIC_CLAUDE_4(ANTHROPIC,"claude-sonnet-4-20250514", 0.7d, HELPFUL_SYSTEM_NO_LIMIT, false),
    LLM_ANTHROPIC_CLAUDE_4_5(ANTHROPIC,"claude-sonnet-4-5-20250929", 0.8d, HELPFUL_SYSTEM_NO_LIMIT, false),

    //Gemma 3 causes exception: Conversation roles must alternate user/assistant/user/assistant/
    LLM_DOCKER_GEMMA_3(DOCKER, "ai/gemma3", 0.7d, HELPFUL_SYSTEM_NO_LIMIT, true),
    LLM_DOCKER_DEEPSEEK_R1(DOCKER, "ai/deepseek-r1-distill-llama", 0.7d, HELPFUL_SYSTEM_NO_LIMIT, false),

    LLM_OLLAMA_LLAMA_3(OLLAMA, "llama3", 0.7d, HELPFUL_SYSTEM_NO_LIMIT, false),
    LLM_OLLAMA_QWEN_3(OLLAMA, "qwen3", 0.7d, HELPFUL_SYSTEM_NO_LIMIT, false),
    LLM_OLLAMA_GEMMA_3(OLLAMA, "gemma3", 0.7d, HELPFUL_SYSTEM_NO_LIMIT, false);

    private final LlmProvider llmProvider;
    private final String name;
    private final Double temperature;
    private final String  system;
    private final Boolean alternateRoles;

}
