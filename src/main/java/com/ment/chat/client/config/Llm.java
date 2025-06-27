package com.ment.chat.client.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static com.ment.chat.client.config.ApiConnection.DOCKER_AI;
import static com.ment.chat.client.config.ApiConnection.OLLAMA;
import static com.ment.chat.client.config.ApiConnection.OPEN_AI;
import static com.ment.chat.client.config.Systems.HELPFUL_SYSTEM_NO_LIMIT;
import static com.ment.chat.client.config.Systems.HELPFUL_SYSTEM_MAX_100;
import static com.ment.chat.client.config.Systems.HELPFUL_SYSTEM_MAX_50;

@RequiredArgsConstructor
@Getter

public enum Llm {
    LLM_OPEN_AI_GPT_4O("gpt-4o", 0.7d, HELPFUL_SYSTEM_NO_LIMIT, OPEN_AI),
    LLM_OPEN_AI_GPT_4O_MINI("gpt-4o-mini", 0.7d, HELPFUL_SYSTEM_MAX_100, OPEN_AI),
    LLM_OPEN_AI_GPT_41_NANO("gpt-4.1-nano", 0.7d, HELPFUL_SYSTEM_MAX_50, OPEN_AI),
    LLM_OPEN_AI_GPT_O4_MINI("o4-mini", 1.0d, HELPFUL_SYSTEM_MAX_100, OPEN_AI),
    LLM_DOCKER_AI_GEMMA_3("ai/gemma3", 0.7d, HELPFUL_SYSTEM_NO_LIMIT, DOCKER_AI),
    LLM_DOCKER_AI_DEEPSEEK_R1("ai/deepseek-r1-distill-llama", 0.7d, HELPFUL_SYSTEM_NO_LIMIT, DOCKER_AI),
    LLM_OLLAMA_LLAMA_3("llama3", 0.7d, HELPFUL_SYSTEM_NO_LIMIT, OLLAMA),
    LLM_OLLAMA_QWEN_3("qwen3", 0.7d, HELPFUL_SYSTEM_NO_LIMIT, OLLAMA),
    LLM_OLLAMA_GEMMA_3("gemma3", 0.7d, HELPFUL_SYSTEM_NO_LIMIT, OLLAMA);

    private final String name;
    private final Double temperature;
    private final String  system;
    private final ApiConnection apiConnection;

}
