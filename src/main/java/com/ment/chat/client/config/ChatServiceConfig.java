package com.ment.chat.client.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.ment.chat.client.config.Llm.LLM_DOCKER_AI_GEMMA_3;
import static com.ment.chat.client.config.Llm.LLM_OLLAMA_QWEN_3;
import static com.ment.chat.client.config.Llm.LLM_OPEN_AI_GPT_O4_MINI;

@Configuration
public class ChatServiceConfig {

    private final OpenAiApi baseOpenAiApi = OpenAiApi.builder()
            .apiKey("NOT_NEEDED")
            .build();

    @Bean("internalChatClient")
    public ChatClient internalChatClient(OpenAiChatModel baseChatModel) {
        return mutateClient(baseChatModel, LLM_OLLAMA_QWEN_3);
    }

    @Bean("externalChatClient")
    public ChatClient externalChatClient(OpenAiChatModel baseChatModel) {
        return mutateClient(baseChatModel, LLM_OPEN_AI_GPT_O4_MINI);
    }

    @Bean("dockerChatClient")
    public ChatClient dockerChatClient(OpenAiChatModel baseChatModel) {
        return mutateClient(baseChatModel, LLM_DOCKER_AI_GEMMA_3);
    }


    private ChatClient mutateClient(OpenAiChatModel chatModel, Llm llm) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
        OpenAiApi api = configApi(llm.getApiConnection());
        OpenAiChatModel model = configChatModel(chatModel, api, llm);
        return ChatClient.builder(model)
                .defaultSystem(llm.getSystem())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    private OpenAiApi configApi(ApiConnection apiCONNECTION) {
        return baseOpenAiApi.mutate()
                .baseUrl(apiCONNECTION.getUrl())
                .apiKey(apiCONNECTION.getKey())
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
