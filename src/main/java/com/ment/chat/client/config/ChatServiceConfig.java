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

import static com.ment.chat.client.config.Llm.LLM_DOCKER_AI_DEEPSEEK_R1;
import static com.ment.chat.client.config.Llm.LLM_OLLAMA_QWEN_3;
import static com.ment.chat.client.config.Llm.LLM_OPEN_AI_GPT_4O;

@Configuration
@Slf4j
public class ChatServiceConfig {

    private final OpenAiApi baseOpenAiApi = OpenAiApi.builder()
            .apiKey("NOT_NEEDED")
            .build();

    @Bean
    public ChatClient internalChatClient(OpenAiChatModel baseChatModel, AppProperties appProperties) {
        return mutateClient(baseChatModel,
                nameToLlm(appProperties.models().internal().llmModelName(), LLM_OLLAMA_QWEN_3),
                appProperties.models().internal().apiConnection());
    }

    @Bean
    public ChatClient externalChatClient(OpenAiChatModel baseChatModel, AppProperties appProperties) {
        return mutateClient(baseChatModel,
                nameToLlm(appProperties.models().external().llmModelName(), LLM_OPEN_AI_GPT_4O),
                appProperties.models().external().apiConnection());
    }

    @Bean
    public ChatClient dockerChatClient(OpenAiChatModel baseChatModel, AppProperties appProperties) {
        return mutateClient(baseChatModel,
                nameToLlm(appProperties.models().docker().llmModelName(), LLM_DOCKER_AI_DEEPSEEK_R1),
                appProperties.models().docker().apiConnection());
    }

    private Llm nameToLlm(String name, Llm defaultModel) {
        return Arrays.stream(Llm.values())
                .filter(llm -> llm.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(defaultModel);
    }

    private ChatClient mutateClient(OpenAiChatModel chatModel, Llm llm, AppProperties.Models.ApiConnection apiConnection) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
        OpenAiApi api = configApi(apiConnection);
        OpenAiChatModel model = configChatModel(chatModel, api, llm);
        return ChatClient.builder(model)
                .defaultSystem(llm.getSystem())
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

    private OpenAiChatModel configChatModel(OpenAiChatModel chatModel, OpenAiApi api, Llm llm) {
        return chatModel.mutate()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(llm.getName())
                        .temperature(llm.getTemperature())
                        .build())
                .build();
    }
}
