package com.ment.chat.client.service;

import com.ment.chat.client.config.AppProperties;
import com.ment.chat.client.domain.Request;
import com.ment.chat.client.domain.Response;
import com.ment.chat.client.domain.exception.RequestNotFoundException;
import com.ment.chat.client.domain.repository.RequestRepository;
import com.ment.chat.client.domain.repository.ResponseRepository;
import com.ment.chat.client.model.in.ConversationRequest;
import com.ment.chat.client.model.out.ConversationResponse;
import com.ment.chat.client.model.out.FindConversationResponse;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Aspect
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final RequestRepository requestRepository;

    private final ResponseRepository responseRepository;

    private final ApplicationEventPublisher applicationEventPublisher;

    private MessageType messageType = MessageType.USER;

    @Qualifier("internalChatClient")
    private final ChatClient internalChatClient;

    @Qualifier("externalChatClient")
    private final ChatClient externalChatClient;

    @Qualifier("dockerChatClient")
    private final ChatClient dockerChatClient;

    private final AppProperties appProperties;

    @Override
    public ConversationResponse getExternalChatResponse(ConversationRequest conversationRequest) {
        return getChatResponse(createUniqueId(), conversationRequest, externalChatClient);
    }

    @Override
    public ConversationResponse getDockerChatResponse(ConversationRequest conversationRequest) {
        return getChatResponse(createUniqueId(), conversationRequest, dockerChatClient);
    }

    @Override
    public ConversationResponse getInternalChatResponse(ConversationRequest conversationRequest) {
        return getChatResponse(createUniqueId(), conversationRequest, internalChatClient);
    }

    @Override
    public ConversationResponse getChatResponses(ConversationRequest conversationRequest) {
        return getChatResponses(createUniqueId(), conversationRequest, internalChatClient, externalChatClient, dockerChatClient);
    }

    @Override
    public FindConversationResponse getRequestWithResponses(String requestId) {
        return requestRepository.findById(requestId)
                .map(request -> FindConversationResponse.builder()
                        .prompt(request.getPrompt())
                        .responses(transform(responseRepository.findByRequestId(requestId)))
                        .build())
                .orElseThrow(RequestNotFoundException::new);
    }

    private List<ConversationResponse> transform(List<Response> responses) {
        return responses.stream()
                .map(resp -> ConversationResponse.builder()
                        .answer(resp.getAnswer())
                        .llm(resp.getLlm())
                        .tokenUsage(resp.getTokenUsage())
                        .executionTimeMs(resp.getExecutionTimeMs())
                        .answeredAt(resp.getAnsweredAt())
                        .build())
                .toList();
    }

    private ConversationResponse getChatResponse(String id, ConversationRequest conversationRequest, ChatClient chatClient) {
        try {
            /* simpler call, not using chat memory
            String llmAnswer = defaultChatClient
                    .prompt(promptRequest.createPrompt())
                    .call()
                    .content();
            String model = getModelFromChatClient(defaultChatClient);
            */
            Message message = createSavePublishRequest(id, conversationRequest);

            long start = System.currentTimeMillis();

            ChatClient.ChatClientRequestSpec reqSpec = createRequestSpec(conversationRequest, chatClient, message);

            ChatResponse chatResponse = reqSpec
                    .call()
                    .chatResponse();

            assert chatResponse != null;

            return createSavePublishResponse(System.currentTimeMillis() - start, id, OffsetDateTime.now(), chatResponse);

        } catch (Exception e) {
            log.error("Error in flow from {}", chatClient, e);
            throw e;
        }
    }

    private ConversationResponse getChatResponses(String id, ConversationRequest conversationRequest, ChatClient chatClient1, ChatClient chatClient2, ChatClient chatClient3) {
        try {
            Message message = createSavePublishRequest(id, conversationRequest);

            long start = System.currentTimeMillis();

            Mono<ChatResponse> call1 = Mono.fromSupplier(() ->
                            createRequestSpec(conversationRequest, chatClient1, message)
                                    .call()
                                    .chatResponse())
                    .onErrorResume(e -> {
                        log.error("Error in first chat call", e);
                        return Mono.empty();
                    });

            Mono<ChatResponse> call2 = Mono.fromSupplier(() ->
                            createRequestSpec(conversationRequest, chatClient2, message)
                                    .call()
                                    .chatResponse())
                    .onErrorResume(e -> {
                        log.error("Error in second chat call", e);
                        return Mono.empty();
                    });

            Mono<ChatResponse> call3 = Mono.fromSupplier(() ->
                            createRequestSpec(conversationRequest, chatClient3, message)
                                    .call()
                                    .chatResponse())
                    .onErrorResume(e -> {
                        log.error("Error in third chat call", e);
                        return Mono.empty();
                    });

            return combineResponses(start, id, call1, call2, call3)
                    .block();
        } catch (Exception e) {
            log.error("Error in combined flow ", e);
            throw e;
        }
    }

    private ChatClient.ChatClientRequestSpec createRequestSpec(ConversationRequest conversationRequest, ChatClient chatClient, Message message) {
        ChatClient.ChatClientRequestSpec reqSpec = chatClient
                .prompt(Prompt.builder()
                        .messages(message)
                        .build());
        if (StringUtils.hasText(conversationRequest.getChatId())) {
            reqSpec.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationRequest.getChatId()));
        }
        return reqSpec;
    }


    private Mono<ConversationResponse> combineResponses(long start, String requestId, Mono<ChatResponse> response1, Mono<ChatResponse> response2, Mono<ChatResponse> response3) {
        return Mono.zip(response1, response2, response3)
                .map(tuple -> {
                    OffsetDateTime now = OffsetDateTime.now();
                    createSavePublishResponse(System.currentTimeMillis() - start, requestId, now, tuple.getT1());
                    createSavePublishResponse(System.currentTimeMillis() - start, requestId, now, tuple.getT2());
                    createSavePublishResponse(System.currentTimeMillis() - start, requestId, now, tuple.getT3());

                    String combinedAnswer = String.join("\n\n",
                            tuple.getT1().getMetadata().getModel() + ":" + tuple.getT1().getResults().getFirst().getOutput().getText(),
                            tuple.getT2().getMetadata().getModel() + ":" + tuple.getT2().getResults().getFirst().getOutput().getText(),
                            tuple.getT3().getMetadata().getModel() + ":" + tuple.getT3().getResults().getFirst().getOutput().getText());
                    return createConversationResponses(System.currentTimeMillis() - start, now, combinedAnswer);
                });
    }

    private Message createSavePublishRequest(String requestId, ConversationRequest conversationRequest) {
        String prompt = conversationRequest.createPrompt();
        log.info("Sending prompt to LLMs: {}", prompt);
        Request request = createRequest(requestId, prompt, conversationRequest.getChatId());
        requestRepository.save(request);
        applicationEventPublisher.publishEvent(request);
        return createMessageAndToggleMessageType(prompt);
    }

    private ConversationResponse createSavePublishResponse(long execTime, String requestId, OffsetDateTime dateTime, ChatResponse response) {
        ConversationResponse conversationResponse = ConversationResponse.builder()
                .answer(response.getResults().getFirst().getOutput().getText())
                .llm(response.getMetadata().getModel())
                .tokenUsage(response.getMetadata().getUsage().toString())
                .executionTimeMs(execTime)
                .answeredAt(dateTime)
                .build();
        savePublishResponse(requestId, conversationResponse);
        return conversationResponse;
    }

    private ConversationResponse createConversationResponses(long execTime, OffsetDateTime dateTime, String combinedAnswer) {
        return ConversationResponse.builder()
                .answer(combinedAnswer)
                .llm("Combined Models")
                .tokenUsage("N/A")
                .executionTimeMs(execTime) // Placeholder, actual execution time should be set elsewhere
                .answeredAt(dateTime)
                .build();
    }

    private void savePublishResponse(String requestId, ConversationResponse conversationResponse) {
        Response llmResponse = createResponse(requestId, conversationResponse);
        responseRepository.save(llmResponse);
        applicationEventPublisher.publishEvent(llmResponse);
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


    private Request createRequest(String id, String prompt, String chatId) {
        return new Request(id, prompt, chatId, null);
    }

    private Response createResponse(String requestId, ConversationResponse response) {
        return new Response(UUID.randomUUID().toString(),
                requestId,
                response.getAnswer(),
                response.getLlm(),
                response.getTokenUsage(),
                response.getExecutionTimeMs(),
                response.getAnsweredAt(),
                null);
    }

    private String createUniqueId() {
        return UUID.randomUUID().toString();
    }


}
