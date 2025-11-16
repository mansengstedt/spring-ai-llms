package com.ment.chat.client.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

import static com.ment.chat.client.config.LlmConfig.LLM_ANTHROPIC_CLAUDE_4_5;
import static com.ment.chat.client.config.LlmConfig.LLM_DOCKER_DEEPSEEK_R1;
import static com.ment.chat.client.config.LlmConfig.LLM_OLLAMA_QWEN_3;
import static com.ment.chat.client.config.LlmConfig.LLM_OPEN_AI_GPT_5;

@Configuration
@Slf4j
public class ChatServiceConfig {

    private final OpenAiApi baseOpenAiApi = OpenAiApi.builder()
            .apiKey("NOT_NEEDED")
            .build();

    @Bean
    public ChatClient ollamaChatClient(OpenAiChatModel baseChatModel, AppProperties appProperties) {
        return mutateClient(baseChatModel,
                nameToLlm(appProperties.models().ollama().llmModelName(), LLM_OLLAMA_QWEN_3),
                appProperties.models().ollama().apiConnection());
    }

    @Bean
    public ChatClient openAiChatClient(OpenAiChatModel baseChatModel, AppProperties appProperties) {
        return mutateClient(baseChatModel,
                nameToLlm(appProperties.models().openAi().llmModelName(), LLM_OPEN_AI_GPT_5),
                appProperties.models().openAi().apiConnection());
    }

    @Bean
    public ChatClient anthropicChatClient(OpenAiChatModel baseChatModel, AppProperties appProperties) {
        return mutateClient(baseChatModel,
                nameToLlm(appProperties.models().anthropic().llmModelName(), LLM_ANTHROPIC_CLAUDE_4_5),
                appProperties.models().anthropic().apiConnection());
    }

    @Bean
    public ChatClient dockerChatClient(OpenAiChatModel baseChatModel, AppProperties appProperties) {
        return mutateClient(baseChatModel,
                nameToLlm(appProperties.models().docker().llmModelName(), LLM_DOCKER_DEEPSEEK_R1),
                appProperties.models().docker().apiConnection());
    }

    private LlmConfig nameToLlm(String name, LlmConfig defaultModel) {
        return Arrays.stream(LlmConfig.values())
                .filter(llmConfig -> llmConfig.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(defaultModel);
    }

    private ChatClient mutateClient(OpenAiChatModel chatModel, LlmConfig llmConfig, AppProperties.Models.ApiConnection apiConnection) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
        OpenAiApi api = configApi(apiConnection);
        OpenAiChatModel model = configChatModel(chatModel, api, llmConfig);
        return ChatClient.builder(model)
                .defaultSystem(llmConfig.getSystem())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    private OpenAiApi configApi(AppProperties.Models.ApiConnection apiConnection) {
        log.info("Configuring OpenAiApi with apiConnection: {}", apiConnection);
        return baseOpenAiApi.mutate()
                .baseUrl(apiConnection.url())
                .apiKey(apiConnection.key())
                .build();
    }

    private OpenAiChatModel configChatModel(OpenAiChatModel chatModel, OpenAiApi api, LlmConfig llmConfig) {
        return chatModel.mutate()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(llmConfig.getName())
                        .temperature(llmConfig.getTemperature())
                        .build())
                .build();
    }
}
