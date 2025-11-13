package com.ment.chat.client.service;

import com.ment.chat.client.config.AppProperties;
import com.ment.chat.client.domain.Request;
import com.ment.chat.client.domain.Response;
import com.ment.chat.client.domain.exception.ChatNotFoundException;
import com.ment.chat.client.domain.exception.RequestNotFoundException;
import com.ment.chat.client.domain.repository.RequestRepository;
import com.ment.chat.client.domain.repository.ResponseRepository;
import com.ment.chat.client.model.in.CreateConversationRequest;
import com.ment.chat.client.model.out.CreateCombinedConversationResponse;
import com.ment.chat.client.model.out.CreateConversationResponse;
import com.ment.chat.client.model.out.GetChatResponse;
import com.ment.chat.client.model.out.GetChatServiceStatusResponse;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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

    @Qualifier("openAiChatClient")
    private final ChatClient openAiChatClient;

    @Qualifier("anthropicChatClient")
    private final ChatClient anthropicChatClient;

    @Qualifier("dockerChatClient")
    private final ChatClient dockerChatClient;

    private final AppProperties appProperties;

    @Override
    public CreateConversationResponse getOpenAiChatResponse(CreateConversationRequest conversationRequest) {
        return getChatResponse(createUniqueId(), conversationRequest, openAiChatClient);
    }

    @Override
    public CreateConversationResponse getAnthropicChatResponse(CreateConversationRequest conversationRequest) {
        return getChatResponse(createUniqueId(), conversationRequest, anthropicChatClient);
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
    public CreateCombinedConversationResponse getCombinedChatResponse(CreateConversationRequest conversationRequest) {
        return getChatResponses(createUniqueId(), conversationRequest, internalChatClient, dockerChatClient, openAiChatClient, anthropicChatClient);
    }

    @Override
    public GetConversationResponse getConversation(String requestId) {
        return requestRepository.findById(requestId)
                .map(request -> GetConversationResponse.builder()
                        .request(GetConversationResponse.ConversationRequest.builder()
                                .id(request.getRequestId())
                                .prompt(request.getPrompt())
                                .chatId(request.getChatId())
                                .queriedAt(request.getQueriedAt())
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

        List<GetConversationResponse> responses = requests
                .stream()
                .map(request -> GetConversationResponse.builder()
                        .request(GetConversationResponse.ConversationRequest.builder()
                                .id(request.getRequestId())
                                .prompt(request.getPrompt())
                                .chatId(request.getChatId())
                                .queriedAt(request.getQueriedAt())
                                .build())
                        .responses(transform(request.getResponses()))
                        .build())
                .toList().stream()
                .sorted()
                .toList();

        return GetChatResponse.builder()
                .conversations(responses)
                .build();
    }

    @Override
    public GetChatServiceStatusResponse getChatServiceStatus() {
        return extractStatusFrom(getCombinedChatResponse(CreateConversationRequest.builder()
                .prompt("Hello")
                .chatId("ping-chat-service-status")
                .build()));
    }

    private GetChatServiceStatusResponse extractStatusFrom(CreateCombinedConversationResponse combinedChatResponse) {
        if (combinedChatResponse == null) {
            return GetChatServiceStatusResponse.builder()
                    .statusList(List.of())
                    .build();
        }
        List<GetChatServiceStatusResponse.ChatServiceStatus> statusList = combinedChatResponse.getConversationResponses().stream()
                .map(response -> GetChatServiceStatusResponse.ChatServiceStatus.builder()
                        .status(response.getLlm() == null ? GetChatServiceStatusResponse.LlmStatus.UNAVAILABLE : GetChatServiceStatusResponse.LlmStatus.AVAILABLE)
                        .llm(response.getLlm() == null ? "Unknown" : response.getLlm()) //TODO: how to dind llm when no anser by using chatclient data
                        .build())
                .toList();
        return GetChatServiceStatusResponse.builder()
                .statusList(statusList)
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
                .toList().stream()
                .sorted(Comparator.comparing(
                        CreateConversationResponse::getAnsweredAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                )).toList();
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

    private CreateCombinedConversationResponse getChatResponses(String id, CreateConversationRequest conversationRequest, ChatClient chatClient1, ChatClient chatClient2, ChatClient chatClient3, ChatClient chatClient4) {
        try {
            Message message = createSavePublishRequest(id, conversationRequest);

            long start = System.currentTimeMillis();

            Mono<ChatResponse> call1 = callChatClient(conversationRequest, chatClient1, message);

            Mono<ChatResponse> call2 = callChatClient(conversationRequest, chatClient2, message);

            Mono<ChatResponse> call3 = callChatClient(conversationRequest, chatClient3, message);

            Mono<ChatResponse> call4 = callChatClient(conversationRequest, chatClient4, message);

            return combineResponses(start, id, call1, call2, call3, call4)
                    .block();
        } catch (Exception e) {
            log.error("Error in combined flow ", e);
            throw e;
        }
    }

    private Mono<ChatResponse> callChatClient(CreateConversationRequest conversationRequest, ChatClient chatClient, Message message) {
        return Mono.fromSupplier(() ->
                        createRequestSpec(conversationRequest, chatClient, message)
                                .call()
                                .chatResponse())
                .onErrorResume(e -> {
                    log.error("Error in chat call", e);
                    return Mono.fromSupplier(() -> new ChatResponse(List.of()));
                    //return Mono.empty();
                });
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


    private Mono<CreateCombinedConversationResponse> combineResponses(long start, String requestId, Mono<ChatResponse> response1, Mono<ChatResponse> response2, Mono<ChatResponse> response3, Mono<ChatResponse> response4) {
        return Mono.zip(response1, response2, response3, response4)
                .map(tuple -> {
                    OffsetDateTime now = OffsetDateTime.now();

                    List<CreateConversationResponse> savePublishResponses = List.of(
                            createSavePublishResponse(System.currentTimeMillis() - start, requestId, now, (ChatResponse) Objects.requireNonNull(tuple.get(0))),
                            createSavePublishResponse(System.currentTimeMillis() - start, requestId, now, (ChatResponse) Objects.requireNonNull(tuple.get(1))),
                            createSavePublishResponse(System.currentTimeMillis() - start, requestId, now, (ChatResponse) Objects.requireNonNull(tuple.get(2))),
                            createSavePublishResponse(System.currentTimeMillis() - start, requestId, now, (ChatResponse) Objects.requireNonNull(tuple.get(3))));

                    return CreateCombinedConversationResponse.builder()
                            .conversationResponses(savePublishResponses)
                            .build();
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
        if (response.getResult() == null) {
            // No answer received which might happen if LLM is not available
            return CreateConversationResponse.builder().build();
        }
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
        return Request.builder()
                .requestId(id)
                .prompt(prompt)
                .chatId(chatId)
                .queriedAt(OffsetDateTime.now())
                .build();
    }

    private Response createResponse(String requestId, CreateConversationResponse response) {
        return Response.builder()
                .responseId(UUID.randomUUID().toString())
                .requestId(requestId)
                .answer(response.getAnswer())
                .llm(response.getLlm())
                .tokenUsage(response.getTokenUsage())
                .executionTimeMs(response.getExecutionTimeMs())
                .answeredAt(response.getAnsweredAt())
                .build();
    }

    private String createUniqueId() {
        return UUID.randomUUID().toString();
    }


}
