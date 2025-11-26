package com.ment.chat.client.service;

import com.ment.chat.client.config.AppProperties;
import com.ment.chat.client.domain.LlmCompletion;
import com.ment.chat.client.domain.LlmPrompt;
import com.ment.chat.client.domain.exception.ChatNotFoundException;
import com.ment.chat.client.domain.exception.PromptNotFoundException;
import com.ment.chat.client.domain.repository.LlmCompletionRepository;
import com.ment.chat.client.domain.repository.LlmPromptRepository;
import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.model.enums.LlmStatus;
import com.ment.chat.client.model.in.CreateCompletionRequest;
import com.ment.chat.client.model.out.CreateCombinedCompletionResponse;
import com.ment.chat.client.model.out.CreateCompletionResponse;
import com.ment.chat.client.model.out.GetChatResponse;
import com.ment.chat.client.model.out.GetInteractionResponse;
import com.ment.chat.client.model.out.GetInteractionsResponse;
import com.ment.chat.client.model.out.GetLlmProviderStatusResponse;
import com.ment.chat.client.model.out.InteractionCompletion;
import com.ment.chat.client.model.out.InteractionPrompt;
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

    private static final String PING_STATUS_CHAT_ID = "ping-chat-service-status";
    private static final String PING_STATUS_PROMPT = "ping LLM to check status";

    private final LlmPromptRepository llmPromptRepository;

    private final LlmCompletionRepository llmCompletionRepository;

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
    public CreateCompletionResponse createCompletionByProvider(CreateCompletionRequest completionRequest, LlmProvider llmProvider) {
        return getChatResponse(createUniqueId(), completionRequest, llmProvider, chatClientMap.get(llmProvider));
    }

    @Override
    public CreateCombinedCompletionResponse createCompletionsByAllProviders(CreateCompletionRequest completionRequest) {
        return getChatResponses(createUniqueId(), completionRequest, chatClientMap);
    }

    @Override
    public GetInteractionResponse getInteractionByPromptId(String promptId) {
        return llmPromptRepository.findById(promptId)
                .map(llmPrompt -> GetInteractionResponse.builder()
                        .interactionPrompt(InteractionPrompt.builder()
                                .promptId(llmPrompt.getPromptId())
                                .prompt(llmPrompt.getPrompt())
                                .chatId(llmPrompt.getChatId())
                                .promptedAt(llmPrompt.getPromptedAt())
                                .build())
                        .interactionCompletions(transform(llmPrompt.getCompletions()))
                        .build())
                .orElseThrow(() -> new PromptNotFoundException(promptId));
    }

    @Override
    public GetChatResponse getChatByChatId(String chatId) {
        List<LlmPrompt> llmPrompts = llmPromptRepository.findByChatId(chatId);
        if (llmPrompts.isEmpty()) {
            throw new ChatNotFoundException(chatId);
        }

        return getGetChatResponse(llmPrompts);
    }

    @Override
    public GetChatResponse getChatByPrompt(String partOfPrompt) {
        List<LlmPrompt> llmPrompts = llmPromptRepository.findByPromptContains(partOfPrompt);
        if (llmPrompts.isEmpty()) {
            return GetChatResponse.builder()
                    .interactions(List.of())
                    .build();
        }

        return getGetChatResponse(llmPrompts);
    }

    @Override
    public GetInteractionsResponse getInteractionsByCompletion(String partOfCompletion) {
        List<LlmCompletion> llmCompletions = llmCompletionRepository.findByCompletionContains(partOfCompletion);
        if (llmCompletions.isEmpty()) {
            return GetInteractionsResponse.builder()
                    .interactions(List.of())
                    .build();
        }

        return GetInteractionsResponse.builder()
                .interactions(llmCompletions.stream()
                        .map(LlmCompletion::getPromptId)
                        .distinct()
                        .map(this::getInteractionByPromptId)
                        .sorted()
                        .toList())
                .build();
    }

    @Override
    public GetLlmProviderStatusResponse getAllProviderStatus() {
        return extractStatusFrom(createCompletionsByAllProviders(CreateCompletionRequest.builder()
                .prompt(PING_STATUS_PROMPT)
                .chatId(PING_STATUS_CHAT_ID)
                .build()));
    }

    private GetChatResponse getGetChatResponse(List<LlmPrompt> llmPrompts) {
        List<GetInteractionResponse> responses = llmPrompts
                .stream()
                .map(llmPrompt -> GetInteractionResponse.builder()
                        .interactionPrompt(InteractionPrompt.builder()
                                .promptId(llmPrompt.getPromptId())
                                .prompt(llmPrompt.getPrompt())
                                .chatId(llmPrompt.getChatId())
                                .promptedAt(llmPrompt.getPromptedAt())
                                .build())
                        .interactionCompletions(transform(llmPrompt.getCompletions()))
                        .build())
                .sorted()
                .toList();

        return GetChatResponse.builder()
                .interactions(responses)
                .build();
    }

    private GetLlmProviderStatusResponse extractStatusFrom(CreateCombinedCompletionResponse combinedChatResponse) {
        if (combinedChatResponse == null) {
            return GetLlmProviderStatusResponse.builder()
                    .statusList(List.of())
                    .build();
        }
        List<GetLlmProviderStatusResponse.ChatServiceStatus> statusList = combinedChatResponse.getInteractionCompletions().stream()
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

    private List<InteractionCompletion> transform(List<LlmCompletion> completions) {
        return completions.stream()
                .map(resp -> InteractionCompletion.builder()
                        .completionId(resp.getCompletionId())
                        .promptId(resp.getPromptId())
                        .completion(resp.getCompletion())
                        .llm(resp.getLlm())
                        .llmProvider(resp.getLlmProvider())
                        .tokenUsage(resp.getTokenUsage())
                        .executionTimeMs(resp.getExecutionTimeMs())
                        .completedAt(resp.getCompletedAt())
                        .build())
                .toList().stream()
                .sorted()
                .toList();
    }

    private CreateCompletionResponse getChatResponse(String id, CreateCompletionRequest completionRequest, LlmProvider llmProvider, ChatClient chatClient) {
        try {
            /* simpler call, not using chat memory
            String llmAnswer = defaultChatClient
                    .interactionPrompt(promptRequest.createPrompt())
                    .call()
                    .content();
            String model = getModelFromChatClient(defaultChatClient);
            */
            Message message = createSavePublishRequest(id, completionRequest);

            long start = System.currentTimeMillis();

            ChatClient.ChatClientRequestSpec reqSpec = createRequestSpec(completionRequest, chatClient, message);


            ChatResponse chatResponse = callProvider(chatClient, reqSpec);

            assert chatResponse != null;

            return createSavePublishResponse(System.currentTimeMillis() - start, id, OffsetDateTime.now(), llmProvider, chatResponse);

        } catch (Exception e) {
            log.error("Error in flow from {}", chatClient, e);
            throw e;
        }
    }

    private ChatResponse callProvider(ChatClient chatClient, ChatClient.ChatClientRequestSpec reqSpec) {
        long start = System.currentTimeMillis();
        log.info("Calling provider with {}", chatClient);
        ChatResponse chatResponse = reqSpec
                .call()
                .chatResponse();
        log.info("Called provider answered within {} ms", System.currentTimeMillis() - start);
        return chatResponse;
    }

    private CreateCombinedCompletionResponse getChatResponses(String id, CreateCompletionRequest completionRequest, Map<LlmProvider, ChatClient> chatClients) {
        try {
            Message message = createSavePublishRequest(id, completionRequest);

            long start = System.currentTimeMillis();

            List<Mono<Map.Entry<LlmProvider, ChatResponse>>> entryList = chatClients.entrySet().stream()
                    .map(entry -> callChatClient(completionRequest, entry.getKey(), entry.getValue(), message))
                    .toList();

            return combineResponses(start, id, entryList)
                    .block();
        } catch (Exception e) {
            log.error("Error in combined flow ", e);
            throw e;
        }
    }

    private Mono<Map.Entry<LlmProvider, ChatResponse>> callChatClient(CreateCompletionRequest completionRequest, LlmProvider llmProvider, ChatClient chatClient, Message message) {
        return Mono.fromSupplier(() -> Map.entry(llmProvider,
                        callProvider(chatClient, Objects.requireNonNull(createRequestSpec(completionRequest, chatClient, message)))))
                .onErrorResume(e -> {
                    log.error("Error in chat call", e);
                    // In case of error, return an empty ChatResponse with LlmProvider to continue processing other LLMs
                    return Mono.fromSupplier(() -> Map.entry(llmProvider, new ChatResponse(List.of())));
                });
    }

    private ChatClient.ChatClientRequestSpec createRequestSpec(CreateCompletionRequest completionRequest, ChatClient chatClient, Message message) {
        ChatClient.ChatClientRequestSpec reqSpec = chatClient
                .prompt(Prompt.builder()
                        .messages(message)
                        .build());
        if (StringUtils.hasText(completionRequest.getChatId())) {
            reqSpec.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, completionRequest.getChatId()));
        }
        return reqSpec;
    }

    private Mono<CreateCombinedCompletionResponse> combineResponses(long start, String promptId, List<Mono<Map.Entry<LlmProvider, ChatResponse>>> responses) {
        return Mono.zip(responses, tuples -> Arrays.stream(tuples)
                        .map(object ->
                                createCombinedSavePublishResponse(System.currentTimeMillis() - start, promptId, OffsetDateTime.now(), castMapEntry(object))
                        )
                        .toList())
                .map(savePublishResponses ->
                        CreateCombinedCompletionResponse.builder()
                                .interactionCompletions(transformCompletions(savePublishResponses))
                                .build()
                );
    }

    private List<InteractionCompletion> transformCompletions(List<CreateCompletionResponse> savePublishResponses) {
        return savePublishResponses.stream()
                .map(CreateCompletionResponse::getInteractionCompletion)
                .toList();
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

    private Message createSavePublishRequest(String promptId, CreateCompletionRequest completionRequest) {
        String prompt = completionRequest.createPrompt();
        log.info("Sending prompt to LLMs: {}", prompt);
        LlmPrompt llmPrompt = createLlmPrompt(promptId, prompt, completionRequest.getChatId());
        llmPromptRepository.save(llmPrompt);
        LlmPrompt savedPrompt = llmPromptRepository.findByPromptId(llmPrompt.getPromptId()); //should be present as just saved (not really needed)
        applicationEventPublisher.publishEvent(savedPrompt);
        return createMessageAndToggleMessageType(prompt);
    }

    private CreateCompletionResponse createCombinedSavePublishResponse(long execTime, String promptId, OffsetDateTime dateTime, Map.Entry<LlmProvider, ChatResponse> response) {
        return createSavePublishResponse(execTime, promptId, dateTime, response.getKey(), response.getValue());
    }

    @SuppressWarnings("ConstantConditions")
    private CreateCompletionResponse createSavePublishResponse(long execTime, String promptId, OffsetDateTime dateTime, LlmProvider llmProvider, ChatResponse response) {
        if (Objects.isNull(response.getResult())) {
            // No answer received which might happen if LLM is not available
            return CreateCompletionResponse.builder()
                    .interactionCompletion(
                            InteractionCompletion.builder()
                                    .completionId(createUniqueId())
                                    .promptId(promptId)
                                    .llmProvider(llmProvider)
                                    .build())
                    .build();
        }
        CreateCompletionResponse createCompletionResponse = CreateCompletionResponse.builder()
                .interactionCompletion(
                        InteractionCompletion.builder()
                                .completionId(createUniqueId())
                                .promptId(promptId)
                                .completion(response.getResults().getFirst().getOutput().getText())
                                .llm(response.getMetadata().getModel())
                                .llmProvider(llmProvider)
                                .tokenUsage(response.getMetadata().getUsage().toString())
                                .executionTimeMs(execTime)
                                .completedAt(dateTime)
                                .build())
                .build();
        savePublishResponse(promptId, createCompletionResponse);
        return createCompletionResponse;
    }

    private void savePublishResponse(String promptId, CreateCompletionResponse createCompletionResponse) {
        LlmCompletion llmCompletion = createLlmCompletion(promptId, createCompletionResponse);
        llmCompletionRepository.save(llmCompletion);
        applicationEventPublisher.publishEvent(llmCompletion);
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


    private LlmPrompt createLlmPrompt(String id, String prompt, String chatId) {
        return LlmPrompt.builder()
                .promptId(id)
                .prompt(prompt)
                .chatId(chatId)
                .promptedAt(OffsetDateTime.now())
                .build();
    }

    private LlmCompletion createLlmCompletion(String promptId, CreateCompletionResponse response) {
        return LlmCompletion.builder()
                .completionId(response.getInteractionCompletion().getCompletionId())
                .promptId(promptId)
                .completion(response.getInteractionCompletion().getCompletion())
                .llm(response.getInteractionCompletion().getLlm())
                .llmProvider(response.getInteractionCompletion().getLlmProvider())
                .tokenUsage(response.getInteractionCompletion().getTokenUsage())
                .executionTimeMs(response.getInteractionCompletion().getExecutionTimeMs())
                .completedAt(response.getInteractionCompletion().getCompletedAt())
                .build();
    }

    private String createUniqueId() {
        return UUID.randomUUID().toString();
    }


}
