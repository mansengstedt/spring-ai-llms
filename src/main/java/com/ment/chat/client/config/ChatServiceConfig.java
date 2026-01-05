package com.ment.chat.client.config;

import com.google.cloud.vertexai.VertexAI;
import com.ment.chat.client.client.ChatClientWithChatMemory;
import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.tools.PublisherTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatModel;
import org.springframework.ai.vertexai.gemini.VertexAiGeminiChatOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Arrays;

import static com.ment.chat.client.config.LlmConfig.LLM_ANTHROPIC_CLAUDE_4_5;
import static com.ment.chat.client.config.LlmConfig.LLM_DOCKER_DEEPSEEK_R1;
import static com.ment.chat.client.config.LlmConfig.LLM_GEMINI_2_5_PRO;
import static com.ment.chat.client.config.LlmConfig.LLM_OLLAMA_QWEN_3;
import static com.ment.chat.client.config.LlmConfig.LLM_OPEN_AI_GPT_5;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ChatServiceConfig {

    private final PublisherTool publisherTool;

    private final OpenAiApi baseOpenAiApi = OpenAiApi.builder()
            .apiKey("NOT_NEEDED")
            .build();

    @Bean
    public ChatClientWithChatMemory ollamaChatClient(OpenAiChatModel baseChatModel, AppProperties appProperties) {
        return mutateClient(baseChatModel,
                nameToLlm(appProperties.models().get(LlmProvider.OLLAMA).llmModelName(), LLM_OLLAMA_QWEN_3),
                appProperties.models().get(LlmProvider.OLLAMA).apiConnection());
    }

    @Bean
    public ChatClientWithChatMemory dockerChatClient(OpenAiChatModel baseChatModel, AppProperties appProperties) {
        return mutateClient(baseChatModel,
                nameToLlm(appProperties.models().get(LlmProvider.DOCKER).llmModelName(), LLM_DOCKER_DEEPSEEK_R1),
                appProperties.models().get(LlmProvider.DOCKER).apiConnection());
    }

    @Bean
    public ChatClientWithChatMemory openAiChatClient(OpenAiChatModel baseChatModel, AppProperties appProperties) {
        return mutateClient(baseChatModel,
                nameToLlm(appProperties.models().get(LlmProvider.OPENAI).llmModelName(), LLM_OPEN_AI_GPT_5),
                appProperties.models().get(LlmProvider.OPENAI).apiConnection());
    }


    /**
     * Creates an Anthropic ChatClient bean configured based on application properties.
     * <p>
     * The anthropicChatClient could be mutated from OpenAi-library classes exactly like Ollama and Docker.
     * If done like this, some params must be set explicitly, like 'max_tokens', to avoid errors like:
     * "invalid_request_error","message":"max_tokens: Field required"
     * since AnthropicChatModel cannot be mutated from a base Anthropic Model.
     *
     * @param appProperties the application properties containing model and API connection details
     * @return a configured ChatClient for Anthropic
     */
    @Bean
    public ChatClientWithChatMemory anthropicChatClient(AppProperties appProperties) {
        return mutateAnthropicClient(
                nameToLlm(appProperties.models().get(LlmProvider.ANTHROPIC).llmModelName(), LLM_ANTHROPIC_CLAUDE_4_5),
                appProperties.models().get(LlmProvider.ANTHROPIC).apiConnection());
    }

    @Bean
    public ChatClientWithChatMemory geminiChatClient(AppProperties appProperties, GeminiProperties geminiProperties) throws IOException {
        return mutateGeminiClient(
                //the accessibility of models like 'gemini-3.0-pro-preview' in a location, like 'europe-north1' might change
                nameToLlm(appProperties.models().get(LlmProvider.GEMINI).llmModelName(), LLM_GEMINI_2_5_PRO),
                geminiProperties);
    }

    private LlmConfig nameToLlm(String name, LlmConfig defaultModel) {
        return Arrays.stream(LlmConfig.values())
                .filter(llmConfig -> llmConfig.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(defaultModel);
    }

    private ChatClientWithChatMemory mutateClient(OpenAiChatModel chatModel, LlmConfig llmConfig, AppProperties.ApiConnection apiConnection) {
        OpenAiApi api = configOpenAiApi(apiConnection, llmConfig.getLlmProvider());
        OpenAiChatModel model = configChatModel(chatModel, api, llmConfig);
        return createChatClient(llmConfig, model) ;
    }

    private ChatClientWithChatMemory mutateAnthropicClient(LlmConfig llmConfig, AppProperties.ApiConnection apiConnection) {
        AnthropicApi api = configAnthropicApi(apiConnection, llmConfig.getLlmProvider());
        AnthropicChatModel model = configChatModel(api, llmConfig);
        return createChatClient(llmConfig, model);
    }

    private ChatClientWithChatMemory mutateGeminiClient(LlmConfig llmConfig, GeminiProperties geminiProperties) throws IOException {
        VertexAiGeminiChatModel model = configVertexAiGeminiChatModel(llmConfig, geminiProperties);
        return createChatClient(llmConfig, model) ;
    }

    private ChatClientWithChatMemory createChatClient(LlmConfig llmConfig, ChatModel model) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .build();
        ChatClient chatClient = ChatClient.builder(model)
                .defaultSystem(llmConfig.getSystem())
                //if system and tool description are consistent, then it works for Anthropic, Gemini but not OpenAI
                .defaultTools(publisherTool)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(
                                chatMemory)
                        .build())
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        return new ChatClientWithChatMemory(chatClient, chatMemory);
    }

    private VertexAiGeminiChatModel configVertexAiGeminiChatModel(LlmConfig llmConfig, GeminiProperties geminiProperties) throws IOException {
        //should contain credentials from the downloaded file in GOOGLE_APPLICATION_CREDENTIALS env variable
        VertexAI vertexAI = configVertex(geminiProperties);
        log.info("vertex credentials: {}", vertexAI.getCredentials());

        //model name from LlmConfig must be valid, don't use Spring AI enum that have invalid names
        return VertexAiGeminiChatModel.builder()
                .defaultOptions(
                        VertexAiGeminiChatOptions.builder()
                                .temperature(llmConfig.getTemperature())
                                .topP((double)1.0F)
                                .model(llmConfig.getName())
                                .build())
                .vertexAI(vertexAI)
                .build();
    }

    private VertexAI configVertex(GeminiProperties geminiProperties) {
        return new VertexAI(geminiProperties.projectId(), geminiProperties.location());
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
                        //defaultTools registration is enough, registration is not needed here
                        //.toolNames("publish_arbitrary_message")
                        .build())
                .build();
    }

}
