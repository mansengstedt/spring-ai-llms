package com.ment.chat.client.service;

import com.ment.chat.client.config.AppPropererties;
import com.ment.chat.client.model.in.ConversationRequest;
import com.ment.chat.client.model.out.ConversationResponse;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private MessageType messageType = MessageType.USER;

    @Qualifier("internalChatClient")
    private final ChatClient internalChatClient;

    @Qualifier("externalChatClient")
    private final ChatClient externalChatClient;

    @Qualifier("dockerChatClient")
    private final ChatClient dockerChatClient;

    private final AppPropererties appProperties;

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
            String prompt = conversationRequest.createPrompt();
            log.info("Sending prompt to LLM: {}", prompt);
            Message message = createMessageAndToggleMessageType(prompt);
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

            assert chatResponse != null;
            String model = chatResponse.getMetadata().getModel();
            String tokenUsage = chatResponse.getMetadata().getUsage().toString();
            String llmAnswer = chatResponse.getResults().getFirst().getOutput().getText();
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
            if (Boolean.TRUE == appProperties.toggle().messageType()) {
                messageType = MessageType.ASSISTANT;
            }
            return new UserMessage(prompt);
        } else if (messageType == MessageType.ASSISTANT) {
            if (Boolean.TRUE == appProperties.toggle().messageType()) {
                messageType = MessageType.USER;
            }
            return new AssistantMessage(prompt);
        } else {
            throw new IllegalStateException("Unknown message type: " + messageType);
        }
    }

}
