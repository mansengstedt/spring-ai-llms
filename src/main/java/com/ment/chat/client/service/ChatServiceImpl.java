package com.ment.chat.client.service;

import com.ment.chat.client.config.AppProperties;
import com.ment.chat.client.domain.ChatResponseTimer;
import com.ment.chat.client.domain.LlmCompletion;
import com.ment.chat.client.domain.LlmPrompt;
import com.ment.chat.client.domain.exception.ChatNotFoundException;
import com.ment.chat.client.domain.exception.PromptNotFoundException;
import com.ment.chat.client.domain.repository.LlmCompletionRepository;
import com.ment.chat.client.domain.repository.LlmPromptRepository;
import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.model.enums.LlmStatus;
import com.ment.chat.client.model.in.CreateCompletionByProviderRequest;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Aspect
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String PING_STATUS_CHAT_ID = "ping-chat-service-status";
    private static final String PING_STATUS_PROMPT = "ping LLM to check status";
    private static final Integer MAX_NO_PROVIDERS = 10;

    private static Scheduler scheduler;

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
        checkProviders();
    }

    private void checkProviders() {
        if (chatClientMap.size() > MAX_NO_PROVIDERS) {
            //config error
            System.exit(3);
        }
        if (chatClientMap.size() > 4) {
            scheduler = Schedulers.boundedElastic();
        } else {
            scheduler = Schedulers.parallel();
        }
    }

    @Override
    public CreateCompletionResponse createCompletionByProvider(CreateCompletionByProviderRequest completionRequest) {
        return getCompletionResponse(createUniqueId(), completionRequest);
    }

    @Override
    public CreateCombinedCompletionResponse createCompletionsByAllProviders(CreateCompletionRequest completionRequest) {
        return getCompletionResponses(createUniqueId(), completionRequest, EnumSet.allOf(LlmProvider.class));
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
                        .llm(response.getLlm() == null ? "Unknown" : response.getLlm())
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
                .sorted()
                .toList();
    }

    private CreateCompletionResponse getCompletionResponse(String id, CreateCompletionByProviderRequest completionRequest) {
        try {
            /* simpler call, not using chat memory
            String llmAnswer = defaultChatClient
                    .interactionPrompt(promptRequest.createPrompt())
                    .call()
                    .content();
            String model = getModelFromChatClient(defaultChatClient);
            */
            createSavePublishRequest(id, completionRequest);

            ChatResponseTimer chatResponse = callProvider(completionRequest, completionRequest.getLlmProvider());

            return createSavePublishResponse(id, OffsetDateTime.now(), completionRequest.getLlmProvider(), chatResponse);

        } catch (Exception e) {
            log.error("Error in flow from {}", completionRequest.getLlmProvider(), e);
            throw e;
        }
    }

    private ChatResponseTimer callProvider(LlmProvider llmProvider, ChatClient.ChatClientRequestSpec reqSpec) {
        try {
            long start = System.currentTimeMillis();
            log.info("Calling provider {}", llmProvider);
            ChatResponse chatResponse = reqSpec
                    .call()
                    .chatResponse();
            Long executionTime = System.currentTimeMillis() - start;
            log.info("Called provider {} answered after {} ms", llmProvider, executionTime);
            return new ChatResponseTimer(chatResponse, executionTime);
        } catch (Exception e) {
            log.error("Error calling provider {}", llmProvider, e);
            throw e;
        }
    }

    private CreateCombinedCompletionResponse getCompletionResponses(String id, CreateCompletionRequest completionRequest, Set<LlmProvider> providers) {
        try {
            long start = System.currentTimeMillis();
            log.info("Start combined calling");

            createSavePublishRequest(id, completionRequest);

            Map<LlmProvider, ChatResponseTimer> chatResponses =
                    getChatResponsesInParallel(completionRequest, providers)
                            .block();

            CreateCombinedCompletionResponse response = combineResponses(id, Objects.requireNonNull(chatResponses));

            log.info("Created combined completion response after {} ms", System.currentTimeMillis() - start);
            return response;
        } catch (Exception e) {
            log.error("Error in combined flow ", e);
            throw e;
        }
    }

    // Parallel execution with Mono
    public Mono<Map<LlmProvider, ChatResponseTimer>> getChatResponsesInParallel(CreateCompletionRequest completionRequest,
                                                                                Set<LlmProvider> providers) {

        return Flux.fromIterable(providers)
                .zipWith(Flux.fromIterable(providers))
                .flatMap(tupleClientProvider -> {
                    LlmProvider llmProvider = tupleClientProvider.getT1();

                    return Mono.fromCallable(() -> {
                                ChatResponseTimer result = callProvider(completionRequest, llmProvider);
                                return Map.entry(llmProvider, result);
                            })
                            .subscribeOn(scheduler)
                            .onErrorResume(ex -> {
                                log.error("Error calling provider {} with client {}, error: {}", llmProvider, chatClientMap.get(llmProvider), ex.getMessage());
                                return errorResponse(llmProvider);
                            });
                }, MAX_NO_PROVIDERS)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    /**
     * This is the interface function that can be put in another class/bean.
     * In that case the class is instantiated with the configured clients in a map and handles all provider related stuff.
     * This service will only handle storage of prompts, completions and internal messages.
     *
     * @param completionRequest the llm request
     * @param llmProvider the llm provider
     * @return the answer from the provider with response time
     */
    private ChatResponseTimer callProvider(CreateCompletionRequest completionRequest, LlmProvider llmProvider) {
        Message message = createMessageAndToggleMessageType(completionRequest.createPrompt());
        ChatClient.ChatClientRequestSpec input = createRequestSpec(completionRequest, chatClientMap.get(llmProvider), message);
        return callProvider(llmProvider, input);
    }

    /**
     * When the call to the provider fails, we still return an answer with the LlmProvider and an empty ChatResponse.
     * The alternative is to return Mono.empty() to skip failed calls but then the provider info is lost which is needed further up in call chain.
     *
     * @param llmProvider the used provider
     * @return an entry with the provider and the resulting ChatResponseTimer
     */
    private Mono<Map.Entry<LlmProvider, ChatResponseTimer>> errorResponse(LlmProvider llmProvider) {
        return Mono.just(Map.entry(llmProvider, new ChatResponseTimer(new ChatResponse(List.of()), 0L)));
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

    private CreateCombinedCompletionResponse combineResponses(String promptId, Map<LlmProvider, ChatResponseTimer> responses) {
        List<CreateCompletionResponse> completionResponses = responses.entrySet().stream()
                .map(entry -> createSavePublishResponse(promptId, OffsetDateTime.now(), entry.getKey(), entry.getValue()))
                .toList();

        return CreateCombinedCompletionResponse.builder()
                .interactionCompletions(transformCompletions(completionResponses))
                .build();
    }


    private List<InteractionCompletion> transformCompletions(List<CreateCompletionResponse> savePublishResponses) {
        return savePublishResponses.stream()
                .map(CreateCompletionResponse::getInteractionCompletion)
                .sorted()
                .toList();
    }

    private void createSavePublishRequest(String promptId, CreateCompletionRequest completionRequest) {
        String prompt = completionRequest.createPrompt();
        log.info("Save and publish prompt to be sent: {}", prompt);
        LlmPrompt llmPrompt = createLlmPrompt(promptId, prompt, completionRequest.getChatId());
        llmPromptRepository.save(llmPrompt);
        LlmPrompt savedPrompt = llmPromptRepository.findByPromptId(llmPrompt.getPromptId()); //should be present as just saved (not really needed)
        applicationEventPublisher.publishEvent(savedPrompt);
    }

    @SuppressWarnings("ConstantConditions")
    private CreateCompletionResponse createSavePublishResponse(String promptId, OffsetDateTime dateTime, LlmProvider llmProvider, ChatResponseTimer response) {
        if (Objects.isNull(response.chatResponse().getResult())) {
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
                                .completion(response.chatResponse().getResults().getFirst().getOutput().getText())
                                .llm(response.chatResponse().getMetadata().getModel())
                                .llmProvider(llmProvider)
                                .tokenUsage(response.chatResponse().getMetadata().getUsage().toString())
                                .executionTimeMs(response.executionTimeMs())
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
