package com.ment.chat.client.service;

import com.ment.chat.client.model.in.ConversationRequest;
import com.ment.chat.client.model.out.ConversationResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;

@Aspect
@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    private MessageType messageType = MessageType.USER;

    @Qualifier("internalChatClient")
    ChatClient internalChatClient;

    @Qualifier("externalChatClient")
    ChatClient externalChatClient;

    @Qualifier("dockerChatClient")
    ChatClient dockerChatClient;

    private final ChatClient defaultChatClient;

    public ChatServiceImpl(ChatClient.Builder chatClientBuilder,
                           ChatClient internalChatClient,
                           ChatClient externalChatClient,
                           ChatClient dockerChatClient) {
        this.defaultChatClient = chatClientBuilder.build();
        this.internalChatClient = internalChatClient;
        this.externalChatClient = externalChatClient;
        this.dockerChatClient = dockerChatClient;
    }

    @Override
    public ConversationResponse getExternalChatResponse(ConversationRequest conversationRequest) {
        return getChatResponse(externalChatClient, conversationRequest);
    }

    @Override
    public ConversationResponse getDockerChatResponse(ConversationRequest conversationRequest) {
        return getChatResponse(dockerChatClient, conversationRequest);
    }

    @Override
    public ConversationResponse getInternalChatResponse(ConversationRequest conversationRequest) {
        return getChatResponse(internalChatClient, conversationRequest);
    }

    private ConversationResponse getChatResponse(ChatClient chatClient, ConversationRequest conversationRequest) {
        try {
            long start = System.currentTimeMillis();
            /* simpler call, not using chat memory
            String llmAnswer = defaultChatClient
                    .prompt(promptRequest.createPrompt())
                    .call()
                    .content();
            String model = getModelFromChatClient(defaultChatClient);
            */
            Message message = createMessageAndToggleMessageType(conversationRequest.getPrompt());
            ChatClient.ChatClientRequestSpec reqSpec = chatClient
                    .prompt(Prompt.builder()
                            .messages(message)
                            .build());
            if (StringUtils.hasText(conversationRequest.getChatId())) {
                reqSpec.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationRequest.getChatId()));
            }

            ChatResponse chatResponse = reqSpec
                    .call()
                    .chatResponse();

            String model = chatResponse.getMetadata().getModel();
            String tokenUsage = chatResponse.getMetadata().getUsage().toString();
            String llmAnswer = chatResponse.getResults().get(0).getOutput().getText();
            long stop = System.currentTimeMillis();

            ConversationResponse response =
                    ConversationResponse.builder()
                            .answer(llmAnswer)
                            .llm(model)
                            .tokenUsage(tokenUsage)
                            .executionTimeMs(stop - start)
                            .answeredAt(OffsetDateTime.now())
                            .build();

            log.info("LLM answer: {}", llmAnswer);
            return response;
        } catch (Exception e) {
            log.error("Error in flow from {}", chatClient, e);
            throw e;
        }
    }

    private Message createMessageAndToggleMessageType(String prompt) {
        if (messageType == MessageType.USER) {
            messageType = MessageType.ASSISTANT;
            return new UserMessage(prompt);
        } else if (messageType == MessageType.ASSISTANT) {
            messageType = MessageType.USER;
            return new AssistantMessage(prompt);
        } else {
            throw new IllegalStateException("Unknown message type: " + messageType);
        }
    }

}
