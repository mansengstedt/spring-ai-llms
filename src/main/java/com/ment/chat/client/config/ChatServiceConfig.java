package com.ment.chat.client.config;

import com.ment.chat.client.model.enums.LlmProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
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
                nameToLlm(appProperties.models().get(LlmProvider.OLLAMA).llmModelName(), LLM_OLLAMA_QWEN_3),
                appProperties.models().get(LlmProvider.OLLAMA).apiConnection());
    }

    @Bean
    public ChatClient openAiChatClient(OpenAiChatModel baseChatModel, AppProperties appProperties) {
        return mutateClient(baseChatModel,
                nameToLlm(appProperties.models().get(LlmProvider.OPENAI).llmModelName(), LLM_OPEN_AI_GPT_5),
                appProperties.models().get(LlmProvider.OPENAI).apiConnection());
    }


    /**
     * Creates an Anthropic ChatClient bean configured based on application properties.
     * <p>
     * The anthropicChatClient could be mutated from OpenAi classes exactly like Ollama and Docker.
     * If done like this some params must be set explicitly, like 'max_tokens', to avoid errors like:
     * "invalid_request_error","message":"max_tokens: Field required"
     * since AnthropicChatModel can not be mutated from a base Anthropic Model.
     *
     * @param appProperties the application properties containing model and API connection details
     * @return a configured ChatClient for Anthropic
     */
    @Bean
    public ChatClient anthropicChatClient(AppProperties appProperties) {
        return mutateClient(
                nameToLlm(appProperties.models().get(LlmProvider.ANTHROPIC).llmModelName(), LLM_ANTHROPIC_CLAUDE_4_5),
                appProperties.models().get(LlmProvider.ANTHROPIC).apiConnection());
    }

    @Bean
    public ChatClient dockerChatClient(OpenAiChatModel baseChatModel, AppProperties appProperties) {
        return mutateClient(baseChatModel,
                nameToLlm(appProperties.models().get(LlmProvider.DOCKER).llmModelName(), LLM_DOCKER_DEEPSEEK_R1),
                appProperties.models().get(LlmProvider.DOCKER).apiConnection());
    }

    private LlmConfig nameToLlm(String name, LlmConfig defaultModel) {
        return Arrays.stream(LlmConfig.values())
                .filter(llmConfig -> llmConfig.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(defaultModel);
    }

    private ChatClient mutateClient(OpenAiChatModel chatModel, LlmConfig llmConfig, AppProperties.ApiConnection apiConnection) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
        OpenAiApi api = configOpenAiApi(apiConnection, llmConfig.getLlmProvider());
        OpenAiChatModel model = configChatModel(chatModel, api, llmConfig);
        return ChatClient.builder(model)
                .defaultSystem(llmConfig.getSystem())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    private ChatClient mutateClient(LlmConfig llmConfig, AppProperties.ApiConnection apiConnection) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder().build();
        AnthropicApi api = configAnthropicApi(apiConnection, llmConfig.getLlmProvider());
        AnthropicChatModel model = configChatModel(api, llmConfig);
        return ChatClient.builder(model)
                .defaultSystem(llmConfig.getSystem())
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    private OpenAiApi configOpenAiApi(AppProperties.ApiConnection apiConnection, LlmProvider llmProvider) {
        log.info("Configuring OpenAiApi for provider {} with apiConnection: {}", llmProvider, apiConnection);
        return baseOpenAiApi.mutate()
                .baseUrl(apiConnection.url())
                .apiKey(apiConnection.key())
                .build();
    }

    private AnthropicApi configAnthropicApi(AppProperties.ApiConnection apiConnection, LlmProvider llmProvider) {
        log.info("Configuring AnthropicApi for provider {} with apiConnection: {}", llmProvider, apiConnection);
        return AnthropicApi.builder()
                .baseUrl(apiConnection.url())
                .apiKey(apiConnection.key())
                .build();
    }

    private OpenAiChatModel configChatModel(OpenAiChatModel chatModel, OpenAiApi api, LlmConfig llmConfig) {
        return chatModel.mutate()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(llmConfig.getName())
                        .maxTokens(llmConfig.getMaxTokens())
                        .temperature(llmConfig.getTemperature())
                        .build())
                .build();
    }

    private AnthropicChatModel configChatModel(AnthropicApi api, LlmConfig llmConfig) {
        return AnthropicChatModel.builder()
                .anthropicApi(api)
                .defaultOptions(AnthropicChatOptions.builder()
                        .model(llmConfig.getName())
                        //maxTokens must be set explicitly for Anthropic to avoid: "invalid_request_error","message":"max_tokens: Field required"
                        .maxTokens(llmConfig.getMaxTokens())
                        .temperature(llmConfig.getTemperature())
                        .build())
                .build();
    }
}
