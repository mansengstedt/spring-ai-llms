package com.ment.chat.client.client;

import com.ment.chat.client.domain.ChatResponseTimer;
import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.model.in.CreateCompletionsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Optional;


@Slf4j
public class ProviderClient {

    private final MessageType messageType = MessageType.USER;

    private final ChatClientWithChatMemory chatClient;

    private final String sessionId;

    public ProviderClient(ChatClientWithChatMemory chatClient, String sessionId) {
        this.chatClient = chatClient;
        this.sessionId = sessionId;
    }

    /**
     * The class is instantiated with a configured client and handles all functionality for calling the provider.
     *
     * @param completionRequest the llm request
     * @param llmProvider       the llm provider, only used for logging
     * @return the answer from the provider with response time
     */
    public ChatResponseTimer callProvider(CreateCompletionsRequest completionRequest, LlmProvider llmProvider) {
        Message message = createMessage(completionRequest.createPrompt());
        ChatClient.ChatClientRequestSpec input = createRequestSpec(completionRequest, chatClient.chatClient(), message);
        return callProvider(llmProvider, input, completionRequest.getSystem());
    }

    public void clearSessionHistory(String chatId) {
        chatClient.chatMemory().clear(sessionChatId(chatId));
    }

    public List<Message> getSessionMessages(String chatId) {
        return chatClient.chatMemory().get(sessionChatId(chatId));
    }

    private ChatClient.ChatClientRequestSpec createRequestSpec(CreateCompletionsRequest completionRequest, ChatClient chatClient, Message message) {
        ChatClient.ChatClientRequestSpec reqSpec = chatClient
                .prompt(Prompt.builder()
                        .messages(message)
                        .build());
        reqSpec.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionChatId(completionRequest.getChatId())));
        return reqSpec;
    }

    private String sessionChatId(String chatId) {
        return Optional.ofNullable(chatId)
                .map(id -> sessionId + "-" + id)
                .orElse(sessionId);
    }

    private ChatResponseTimer callProvider(LlmProvider llmProvider, ChatClient.ChatClientRequestSpec reqSpec, String system) {
        try {
            long start = System.currentTimeMillis();
            log.info("Calling provider {} with system {}", llmProvider, system);
            if (system != null) {
                reqSpec
                        //dynamic system value overrides default system from config
                        .system(system);
            }

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

    private Message createMessage(String prompt) {
        log.info("Sending {} message to LLMs: {}", messageType, prompt);
        return switch (messageType) {
            case USER -> new UserMessage(prompt);
            case ASSISTANT -> new AssistantMessage(prompt);
            case SYSTEM -> new SystemMessage(prompt);
            default -> throw new IllegalStateException("Illegal message type: " + messageType);
        };
    }

}
