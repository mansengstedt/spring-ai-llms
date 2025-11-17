package com.ment.chat.client.service;

import com.ment.chat.client.config.AppProperties;
import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.domain.Request;
import com.ment.chat.client.domain.Response;
import com.ment.chat.client.domain.exception.ChatNotFoundException;
import com.ment.chat.client.domain.exception.RequestNotFoundException;
import com.ment.chat.client.domain.repository.RequestRepository;
import com.ment.chat.client.domain.repository.ResponseRepository;
import com.ment.chat.client.model.enums.LlmStatus;
import com.ment.chat.client.model.in.CreateConversationRequest;
import com.ment.chat.client.model.out.CreateCombinedConversationResponse;
import com.ment.chat.client.model.out.CreateConversationResponse;
import com.ment.chat.client.model.out.GetChatResponse;
import com.ment.chat.client.model.out.GetLlmProviderStatusResponse;
import com.ment.chat.client.model.out.GetConversationResponse;
import jakarta.annotation.PostConstruct;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Qualifier("ollamaChatClient")
    private final ChatClient ollamaChatClient;

    @Qualifier("openAiChatClient")
    private final ChatClient openAiChatClient;

    @Qualifier("anthropicChatClient")
    private final ChatClient anthropicChatClient;

    @Qualifier("dockerChatClient")
    private final ChatClient dockerChatClient;

    private final AppProperties appProperties;

    private Map<LlmProvider, ChatClient> chatClientMap;

    @PostConstruct
    private void chatClientMap() {
        chatClientMap = new HashMap<>();
        chatClientMap.put(LlmProvider.OLLAMA, ollamaChatClient);
        chatClientMap.put(LlmProvider.DOCKER, dockerChatClient);
        chatClientMap.put(LlmProvider.OPENAI, openAiChatClient);
        chatClientMap.put(LlmProvider.ANTHROPIC, anthropicChatClient);
    }

    @Override
    public CreateConversationResponse getChatResponse(CreateConversationRequest conversationRequest, LlmProvider llmProvider) {
        return getChatResponse(createUniqueId(), conversationRequest, llmProvider, chatClientMap.get(llmProvider));
    }

    @Override
    public CreateCombinedConversationResponse getCombinedChatResponse(CreateConversationRequest conversationRequest) {
        return getChatResponses(createUniqueId(), conversationRequest, chatClientMap);
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
    public GetLlmProviderStatusResponse getLlmProviderStatus() {
        return extractStatusFrom(getCombinedChatResponse(CreateConversationRequest.builder()
                .prompt("ping LLM to check status")
                .chatId("ping-chat-service-status")
                .build()));
    }

    private GetLlmProviderStatusResponse extractStatusFrom(CreateCombinedConversationResponse combinedChatResponse) {
        if (combinedChatResponse == null) {
            return GetLlmProviderStatusResponse.builder()
                    .statusList(List.of())
                    .build();
        }
        List<GetLlmProviderStatusResponse.ChatServiceStatus> statusList = combinedChatResponse.getConversationResponses().stream()
                .map(response -> GetLlmProviderStatusResponse.ChatServiceStatus.builder()
                        .status(response.getLlm() == null ? LlmStatus.UNAVAILABLE : LlmStatus.AVAILABLE)
                        .llm(response.getLlm() == null ? "Unknown" : response.getLlm()) //TODO: how to handle llm when no answer by using chat client data
                        .provider(response.getLlmProvider())
                        .build())
                .toList();
        return GetLlmProviderStatusResponse.builder()
                .statusList(statusList)
                .build();

    }

    private List<CreateConversationResponse> transform(List<Response> responses) {
        return responses.stream()
                .map(resp -> CreateConversationResponse.builder()
                        .id(resp.getResponseId())
                        .answer(resp.getAnswer())
                        .llm(resp.getLlm())
                        .llmProvider(resp.getLlmProvider())
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

    private CreateConversationResponse getChatResponse(String id, CreateConversationRequest conversationRequest, LlmProvider llmProvider, ChatClient chatClient) {
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

            return createSavePublishResponse(System.currentTimeMillis() - start, id, OffsetDateTime.now(), llmProvider, chatResponse);

        } catch (Exception e) {
            log.error("Error in flow from {}", chatClient, e);
            throw e;
        }
    }

    private CreateCombinedConversationResponse getChatResponses(String id, CreateConversationRequest conversationRequest, Map<LlmProvider, ChatClient> chatClients) {
        try {
            Message message = createSavePublishRequest(id, conversationRequest);

            long start = System.currentTimeMillis();

            List<Mono<Map.Entry<LlmProvider, ChatResponse>>> entryList = chatClients.entrySet().stream()
                    .map(entry -> callChatClient(conversationRequest, entry.getKey(), entry.getValue(), message))
                    .toList();

            return combineResponses(start, id, entryList)
                    .block();
        } catch (Exception e) {
            log.error("Error in combined flow ", e);
            throw e;
        }
    }

    private Mono<Map.Entry<LlmProvider, ChatResponse>> callChatClient(CreateConversationRequest conversationRequest, LlmProvider llmProvider, ChatClient chatClient, Message message) {
        return Mono.fromSupplier(() -> Map.entry(llmProvider,
                        Objects.requireNonNull(createRequestSpec(conversationRequest, chatClient, message)
                                .call()
                                .chatResponse())))
                .onErrorResume(e -> {
                    log.error("Error in chat call", e);
                    // In case of error, return an empty ChatResponse with LlmProvider to continue processing other LLMs
                    return Mono.fromSupplier(() -> Map.entry(llmProvider, new ChatResponse(List.of())));
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

    private Mono<CreateCombinedConversationResponse> combineResponses(long start, String requestId, List<Mono<Map.Entry<LlmProvider, ChatResponse>>> responses) {
        return Mono.zip(responses, tuples -> Arrays.stream(tuples)
                        .map(object ->
                                createCombinedSavePublishResponse(System.currentTimeMillis() - start, requestId, OffsetDateTime.now(), castMapEntry(object))
                        )
                        .toList())
                .map(savePublishResponses ->
                        CreateCombinedConversationResponse.builder()
                                .conversationResponses(savePublishResponses)
                                .build()
                );
    }

    @SuppressWarnings("unchecked")
    private Map.Entry<LlmProvider, ChatResponse> castMapEntry(Object entry) {
        if (entry instanceof Map.Entry<?, ?> castEntry) {
            //castEntry can't be null here due to the instanceof check
            if ((castEntry.getKey() instanceof LlmProvider) && (castEntry.getValue() instanceof ChatResponse)) {
                // Now safe to cast both Key and Value but compiler can't see that type check is done unless Map.entry is created with correct types
                return (Map.Entry<LlmProvider, ChatResponse>) castEntry;
            }
        }
        throw new IllegalArgumentException("Null value(s) or invalid entry type(s): " + entry);
    }

    private Message createSavePublishRequest(String requestId, CreateConversationRequest conversationRequest) {
        String prompt = conversationRequest.createPrompt();
        log.info("Sending prompt to LLMs: {}", prompt);
        Request request = createRequest(requestId, prompt, conversationRequest.getChatId());
        requestRepository.save(request);
        applicationEventPublisher.publishEvent(request);
        return createMessageAndToggleMessageType(prompt);
    }

    private CreateConversationResponse createCombinedSavePublishResponse(long execTime, String requestId, OffsetDateTime dateTime, Map.Entry<LlmProvider, ChatResponse> response) {
        return createSavePublishResponse(execTime, requestId, dateTime, response.getKey(), response.getValue());
    }

    @SuppressWarnings("ConstantConditions")
    private CreateConversationResponse createSavePublishResponse(long execTime, String requestId, OffsetDateTime dateTime, LlmProvider llmProvider, ChatResponse response) {
        if (Objects.isNull(response.getResult())) {
            // No answer received which might happen if LLM is not available
            return CreateConversationResponse.builder()
                    .llmProvider(llmProvider)
                    .build();
        }
        CreateConversationResponse createConversationResponse = CreateConversationResponse.builder()
                .answer(response.getResults().getFirst().getOutput().getText())
                .llm(response.getMetadata().getModel())
                .llmProvider(llmProvider)
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
                .responseId(createUniqueId())
                .requestId(requestId)
                .answer(response.getAnswer())
                .llm(response.getLlm())
                .llmProvider(response.getLlmProvider())
                .tokenUsage(response.getTokenUsage())
                .executionTimeMs(response.getExecutionTimeMs())
                .answeredAt(response.getAnsweredAt())
                .build();
    }

    private String createUniqueId() {
        return UUID.randomUUID().toString();
    }


}
