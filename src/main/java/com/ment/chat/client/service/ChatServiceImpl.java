package com.ment.chat.client.service;

import com.ment.chat.client.config.AppProperties;
import com.ment.chat.client.domain.Request;
import com.ment.chat.client.domain.Response;
import com.ment.chat.client.domain.exception.ChatNotFoundException;
import com.ment.chat.client.domain.exception.RequestNotFoundException;
import com.ment.chat.client.domain.repository.RequestRepository;
import com.ment.chat.client.domain.repository.ResponseRepository;
import com.ment.chat.client.model.in.CreateConversationRequest;
import com.ment.chat.client.model.out.ConversationRequest;
import com.ment.chat.client.model.out.CreateConversationResponse;
import com.ment.chat.client.model.out.GetChatResponse;
import com.ment.chat.client.model.out.GetConversationResponse;
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
    public CreateConversationResponse getExternalChatResponse(CreateConversationRequest conversationRequest) {
        return getChatResponse(createUniqueId(), conversationRequest, externalChatClient);
    }

    @Override
    public CreateConversationResponse getDockerChatResponse(CreateConversationRequest conversationRequest) {
        return getChatResponse(createUniqueId(), conversationRequest, dockerChatClient);
    }

    @Override
    public CreateConversationResponse getInternalChatResponse(CreateConversationRequest conversationRequest) {
        return getChatResponse(createUniqueId(), conversationRequest, internalChatClient);
    }

    @Override
    public CreateConversationResponse getCombinedChatResponse(CreateConversationRequest conversationRequest) {
        return getChatResponses(createUniqueId(), conversationRequest, internalChatClient, externalChatClient, dockerChatClient);
    }

    @Override
    public GetConversationResponse getConversation(String requestId) {
        return requestRepository.findById(requestId)
                .map(request -> GetConversationResponse.builder()
                        .request(ConversationRequest.builder()
                                .id(request.getRequestId())
                                .prompt(request.getPrompt())
                                .chatId(request.getChatId())
                                .build())
                        .responses(transform(request.getResponses()))
                        .build())
                .orElseThrow(() -> new RequestNotFoundException(requestId));
    }

    @Override
    public GetChatResponse getChat(String chatId) {
        List<Request> requests = requestRepository.findByChatId(chatId);
        if (requests.isEmpty()) {
            throw new ChatNotFoundException(chatId);
        }

        return GetChatResponse.builder()
                .conversations(requests
                        .stream()
                        .map(request -> GetConversationResponse.builder()
                                .request(ConversationRequest.builder()
                                        .id(request.getRequestId())
                                        .prompt(request.getPrompt())
                                        .chatId(request.getChatId())
                                        .build())
                                .responses(transform(request.getResponses()))
                                .build())
                        .toList())
                .build();
    }

    private List<CreateConversationResponse> transform(List<Response> responses) {
        return responses.stream()
                .map(resp -> CreateConversationResponse.builder()
                        .id(resp.getResponseId())
                        .answer(resp.getAnswer())
                        .llm(resp.getLlm())
                        .tokenUsage(resp.getTokenUsage())
                        .executionTimeMs(resp.getExecutionTimeMs())
                        .answeredAt(resp.getAnsweredAt())
                        .build())
                .toList();
    }

    private CreateConversationResponse getChatResponse(String id, CreateConversationRequest conversationRequest, ChatClient chatClient) {
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

    private CreateConversationResponse getChatResponses(String id, CreateConversationRequest conversationRequest, ChatClient chatClient1, ChatClient chatClient2, ChatClient chatClient3) {
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

    private ChatClient.ChatClientRequestSpec createRequestSpec(CreateConversationRequest conversationRequest, ChatClient chatClient, Message message) {
        ChatClient.ChatClientRequestSpec reqSpec = chatClient
                .prompt(Prompt.builder()
                        .messages(message)
                        .build());
        if (StringUtils.hasText(conversationRequest.getChatId())) {
            reqSpec.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationRequest.getChatId()));
        }
        return reqSpec;
    }


    private Mono<CreateConversationResponse> combineResponses(long start, String requestId, Mono<ChatResponse> response1, Mono<ChatResponse> response2, Mono<ChatResponse> response3) {
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

    private Message createSavePublishRequest(String requestId, CreateConversationRequest conversationRequest) {
        String prompt = conversationRequest.createPrompt();
        log.info("Sending prompt to LLMs: {}", prompt);
        Request request = createRequest(requestId, prompt, conversationRequest.getChatId());
        requestRepository.save(request);
        applicationEventPublisher.publishEvent(request);
        return createMessageAndToggleMessageType(prompt);
    }

    private CreateConversationResponse createSavePublishResponse(long execTime, String requestId, OffsetDateTime dateTime, ChatResponse response) {
        CreateConversationResponse createConversationResponse = CreateConversationResponse.builder()
                .answer(response.getResults().getFirst().getOutput().getText())
                .llm(response.getMetadata().getModel())
                .tokenUsage(response.getMetadata().getUsage().toString())
                .executionTimeMs(execTime)
                .answeredAt(dateTime)
                .build();
        savePublishResponse(requestId, createConversationResponse);
        return createConversationResponse;
    }

    private CreateConversationResponse createConversationResponses(long execTime, OffsetDateTime dateTime, String combinedAnswer) {
        return CreateConversationResponse.builder()
                .answer(combinedAnswer)
                .llm("Combined Models")
                .tokenUsage("N/A")
                .executionTimeMs(execTime) // Placeholder, actual execution time should be set elsewhere
                .answeredAt(dateTime)
                .build();
    }

    private void savePublishResponse(String requestId, CreateConversationResponse createConversationResponse) {
        Response llmResponse = createResponse(requestId, createConversationResponse);
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

    private Response createResponse(String requestId, CreateConversationResponse response) {
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
