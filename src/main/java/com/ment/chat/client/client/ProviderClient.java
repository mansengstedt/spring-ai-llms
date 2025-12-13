package com.ment.chat.client.client;

import com.ment.chat.client.config.AppProperties;
import com.ment.chat.client.domain.ChatResponseTimer;
import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.model.in.CreateCompletionsByAllProvidersRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.util.StringUtils;

@Slf4j
public class ProviderClient {

    private MessageType messageType = MessageType.USER;

    private final AppProperties appProperties;

    private final ChatClient chatClient;

    public ProviderClient(AppProperties appProperties, ChatClient chatClient) {
        this.appProperties = appProperties;
        this.chatClient = chatClient;
    }

    /**
     * The class is instantiated with a configured client and handles all functionality for calling the provider.
     *
     * @param completionRequest the llm request
     * @param llmProvider       the llm provider, only used for logging
     * @return the answer from the provider with response time
     */
    public ChatResponseTimer callProvider(CreateCompletionsByAllProvidersRequest completionRequest, LlmProvider llmProvider) {
        Message message = createMessageAndToggleMessageType(completionRequest.createPrompt());
        ChatClient.ChatClientRequestSpec input = createRequestSpec(completionRequest, chatClient, message);
        return callProvider(llmProvider, input);
    }

    private ChatClient.ChatClientRequestSpec createRequestSpec(CreateCompletionsByAllProvidersRequest completionRequest, ChatClient chatClient, Message message) {
        ChatClient.ChatClientRequestSpec reqSpec = chatClient
                .prompt(Prompt.builder()
                        .messages(message)
                        .build());
        if (StringUtils.hasText(completionRequest.getChatId())) {
            reqSpec.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, completionRequest.getChatId()));
        }
        return reqSpec;
    }

    private ChatResponseTimer callProvider(LlmProvider llmProvider, ChatClient.ChatClientRequestSpec reqSpec) {
        try {
            long start = System.currentTimeMillis();
            log.info("Calling provider {}", llmProvider);
            ChatResponse chatResponse = reqSpec
                    .call()
                    .chatResponse();
            Long executionTime = System.currentTimeMillis() - start;
            log.info("Called provider {} using model {}, answered after {} ms", llmProvider,
                    chatResponse != null ? chatResponse.getMetadata().getModel() : "unknown", executionTime);
            return new ChatResponseTimer(chatResponse, executionTime);
        } catch (Exception e) {
            log.error("Error calling provider {}", llmProvider, e);
            throw e;
        }
    }

    private Message createMessageAndToggleMessageType(String prompt) {
        if (messageType == MessageType.USER) {
            if (Boolean.TRUE == appProperties.toggle().messageType()) {
                messageType = MessageType.ASSISTANT;
            }
            log.info("Sending user message to LLMs: {}", prompt);
            return new UserMessage(prompt);
        } else if (messageType == MessageType.ASSISTANT) {
            if (Boolean.TRUE == appProperties.toggle().messageType()) {
                messageType = MessageType.USER;
            }
            log.info("Sending assistant message to LLMs: {}", prompt);
            return new AssistantMessage(prompt);
        } else {
            throw new IllegalStateException("Unknown message type: " + messageType);
        }
    }

}
