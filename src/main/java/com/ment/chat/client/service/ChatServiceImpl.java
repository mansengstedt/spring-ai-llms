package com.ment.chat.client.service;

import com.ment.chat.client.client.ProviderClient;
import com.ment.chat.client.config.AppProperties;
import com.ment.chat.client.domain.ChatResponseTimer;
import com.ment.chat.client.domain.LlmCompletion;
import com.ment.chat.client.domain.LlmPrompt;
import com.ment.chat.client.domain.exception.ChatNotFoundException;
import com.ment.chat.client.domain.exception.CompletionNotFoundException;
import com.ment.chat.client.domain.exception.PromptNotFoundException;
import com.ment.chat.client.domain.repository.LlmCompletionRepository;
import com.ment.chat.client.domain.repository.LlmPromptRepository;
import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.model.enums.LlmStatus;
import com.ment.chat.client.model.in.CreateCompletionByProviderRequest;
import com.ment.chat.client.model.in.CreateCompletionsByAllProvidersRequest;
import com.ment.chat.client.model.in.CreateCompletionsByProvidersRequest;
import com.ment.chat.client.model.out.CreateCompletionByProviderResponse;
import com.ment.chat.client.model.out.CreateCompletionsByProvidersResponse;
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
import org.springframework.ai.chat.model.ChatResponse;
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

    public static final String UNKNOWN_MODEL_NAME = "Unknown";
    private static final String PING_STATUS_CHAT_ID = "ping-chat-service-status";
    private static final String PING_STATUS_PROMPT = "ping LLM to check status";
    private static final Integer MAX_NO_PROVIDERS = 10;

    private final AppProperties appProperties;

    private static Scheduler scheduler;

    private final LlmPromptRepository llmPromptRepository;

    private final LlmCompletionRepository llmCompletionRepository;

    private final ApplicationEventPublisher applicationEventPublisher;


    @Qualifier("ollamaChatClient")
    private final ChatClient ollamaChatClient;

    @Qualifier("dockerChatClient")
    private final ChatClient dockerChatClient;

    @Qualifier("openAiChatClient")
    private final ChatClient openAiChatClient;

    @Qualifier("anthropicChatClient")
    private final ChatClient anthropicChatClient;

    @Qualifier("geminiChatClient")
    private final ChatClient geminiChatClient;

    private Map<LlmProvider, ProviderClient> chatClientMap;

    @PostConstruct
    private void chatClientMap() {
        chatClientMap = new HashMap<>();
        chatClientMap.put(LlmProvider.OLLAMA, new ProviderClient(appProperties, ollamaChatClient));
        chatClientMap.put(LlmProvider.DOCKER, new ProviderClient(appProperties, dockerChatClient));
        chatClientMap.put(LlmProvider.OPENAI, new ProviderClient(appProperties, openAiChatClient));
        chatClientMap.put(LlmProvider.ANTHROPIC, new ProviderClient(appProperties, anthropicChatClient));
        chatClientMap.put(LlmProvider.GEMINI, new ProviderClient(appProperties, geminiChatClient));
        checkProviders();
    }

    private void checkProviders() {
        if (chatClientMap.size() > MAX_NO_PROVIDERS) {
            //config error
            System.exit(3);
        }
        if (chatClientMap.size() > LlmProvider.values().length) {
            scheduler = Schedulers.boundedElastic();
        } else {
            scheduler = Schedulers.parallel();
        }
    }

    @Override
    public CreateCompletionByProviderResponse createCompletionByProvider(CreateCompletionByProviderRequest createCompletionByProviderRequest) {
        return getCompletionResponse(createUniqueId(), createCompletionByProviderRequest);
    }

    @Override
    public CreateCompletionsByProvidersResponse createCompletionsByProviders(CreateCompletionsByProvidersRequest createCompletionsByProvidersRequest) {
        return getCompletionsResponse(createUniqueId(), createCompletionsByProvidersRequest, createCompletionsByProvidersRequest.getLlmProviders());
    }

    @Override
    public CreateCompletionsByProvidersResponse createCompletionsByAllProviders(CreateCompletionsByAllProvidersRequest createCompletionsByAllProvidersRequest) {
        return getCompletionsResponse(createUniqueId(), createCompletionsByAllProvidersRequest, EnumSet.allOf(LlmProvider.class));
    }

    @Override
    public GetInteractionResponse getInteractionByPromptId(String promptId) {
        return llmPromptRepository.findById(promptId)
                .map(llmPrompt -> GetInteractionResponse.builder()
                        .interactionPrompt(transform(llmPrompt))
                        .interactionCompletions(transform(llmPrompt.getCompletions()))
                        .build())
                .orElseThrow(() -> new PromptNotFoundException(promptId));
    }

    @Override
    public GetInteractionResponse getInteractionByCompletionId(String completionId) {
        return llmCompletionRepository.findById(completionId)
                .map(llmCompletion ->
                        GetInteractionResponse.builder()
                                .interactionPrompt(transform(llmCompletion.getLlmPrompt()))
                                .interactionCompletions(List.of(transform(llmCompletion)))
                                .build())
                .orElseThrow(() -> new CompletionNotFoundException(completionId));
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
        return extractStatusFrom(createCompletionsByAllProviders(CreateCompletionsByAllProvidersRequest.builder()
                .prompt(PING_STATUS_PROMPT)
                .chatId(PING_STATUS_CHAT_ID)
                .build()));
    }

    private GetChatResponse getGetChatResponse(List<LlmPrompt> llmPrompts) {
        List<GetInteractionResponse> responses = llmPrompts
                .stream()
                .map(llmPrompt -> GetInteractionResponse.builder()
                        .interactionPrompt(transform(llmPrompt))
                        .interactionCompletions(transform(llmPrompt.getCompletions()))
                        .build())
                .sorted()
                .toList();

        return GetChatResponse.builder()
                .interactions(responses)
                .build();
    }

    private InteractionPrompt transform(LlmPrompt llmPrompt) {
        return InteractionPrompt.builder()
                        .promptId(llmPrompt.getPromptId())
                        .prompt(llmPrompt.getPrompt())
                        .chatId(llmPrompt.getChatId())
                        .promptedAt(llmPrompt.getPromptedAt())
                        .build();
    }

    private GetLlmProviderStatusResponse extractStatusFrom(CreateCompletionsByProvidersResponse combinedChatResponse) {
        if (combinedChatResponse == null) {
            return GetLlmProviderStatusResponse.builder()
                    .statusList(List.of())
                    .build();
        }
        List<GetLlmProviderStatusResponse.ChatServiceStatus> statusList = combinedChatResponse.getInteractionCompletions().stream()
                .map(response -> GetLlmProviderStatusResponse.ChatServiceStatus.builder()
                        .status(response.getLlm() == null ? LlmStatus.UNAVAILABLE : LlmStatus.AVAILABLE)
                        .llm(response.getLlm() == null ? UNKNOWN_MODEL_NAME : response.getLlm())
                        .provider(response.getLlmProvider())
                        .build())
                .toList();
        return GetLlmProviderStatusResponse.builder()
                .statusList(statusList)
                .build();

    }

    private InteractionCompletion transform(LlmCompletion completion) {
        return InteractionCompletion.builder()
                .completionId(completion.getCompletionId())
                .promptId(completion.getPromptId())
                .completion(completion.getCompletion())
                .llm(completion.getLlm())
                .llmProvider(completion.getLlmProvider())
                .tokenUsage(completion.getTokenUsage())
                .executionTimeMs(completion.getExecutionTimeMs())
                .completedAt(completion.getCompletedAt())
                .build();
    }

    private List<InteractionCompletion> transform(List<LlmCompletion> completions) {
        return completions.stream()
                .map(this::transform)
                .sorted()
                .toList();
    }

    private CreateCompletionByProviderResponse getCompletionResponse(String id, CreateCompletionByProviderRequest completionRequest) {
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

    private CreateCompletionsByProvidersResponse getCompletionsResponse(String id, CreateCompletionsByAllProvidersRequest completionRequest, Set<LlmProvider> providers) {
        try {
            long start = System.currentTimeMillis();
            log.info("Start combined calling");

            createSavePublishRequest(id, completionRequest);

            Map<LlmProvider, ChatResponseTimer> chatResponses =
                    getChatResponsesInParallel(completionRequest, providers)
                            .block();

            CreateCompletionsByProvidersResponse response = combineResponses(id, Objects.requireNonNull(chatResponses));

            log.info("Created combined completion response after {} ms", System.currentTimeMillis() - start);
            return response;
        } catch (Exception e) {
            log.error("Error in combined flow ", e);
            throw e;
        }
    }

    // Parallel execution with Mono
    public Mono<Map<LlmProvider, ChatResponseTimer>> getChatResponsesInParallel(CreateCompletionsByAllProvidersRequest completionRequest,
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
     * This is the interface function calling the class handling the provider functionality.
     *
     * @param completionRequest the llm request
     * @param llmProvider       the llm provider
     * @return the answer from the provider with response time
     */
    private ChatResponseTimer callProvider(CreateCompletionsByAllProvidersRequest completionRequest, LlmProvider llmProvider) {
        return chatClientMap.get(llmProvider).callProvider(completionRequest, llmProvider);
    }

    /**
     * When the call to the provider fails, we still return an answer with the LlmProvider and an empty ChatResponse.
     * The alternative is to return Mono.empty() to skip failed calls,
     * but then the provider info is lost, which is needed further up in the call chain.
     *
     * @param llmProvider the used provider
     * @return an entry with the provider and the resulting ChatResponseTimer
     */
    private Mono<Map.Entry<LlmProvider, ChatResponseTimer>> errorResponse(LlmProvider llmProvider) {
        return Mono.just(Map.entry(llmProvider, new ChatResponseTimer(new ChatResponse(List.of()), 0L)));
    }


    private CreateCompletionsByProvidersResponse combineResponses(String promptId, Map<LlmProvider, ChatResponseTimer> responses) {
        List<CreateCompletionByProviderResponse> completionResponses = responses.entrySet().stream()
                .map(entry -> createSavePublishResponse(promptId, OffsetDateTime.now(), entry.getKey(), entry.getValue()))
                .toList();

        return CreateCompletionsByProvidersResponse.builder()
                .interactionCompletions(transformCompletions(completionResponses))
                .build();
    }


    private List<InteractionCompletion> transformCompletions(List<CreateCompletionByProviderResponse> savePublishResponses) {
        return savePublishResponses.stream()
                .map(CreateCompletionByProviderResponse::getInteractionCompletion)
                .sorted()
                .toList();
    }

    private void createSavePublishRequest(String promptId, CreateCompletionsByAllProvidersRequest completionRequest) {
        String prompt = completionRequest.createPrompt();
        log.info("Save and publish prompt to be sent: {}", prompt);
        LlmPrompt llmPrompt = createLlmPrompt(promptId, prompt, completionRequest.getChatId());
        llmPromptRepository.save(llmPrompt);
        LlmPrompt savedPrompt = llmPromptRepository.findById(llmPrompt.getPromptId())
                .orElseThrow(() -> new PromptNotFoundException(llmPrompt.getPromptId())); //should be present as just saved (not really needed)
        applicationEventPublisher.publishEvent(savedPrompt);
    }

    @SuppressWarnings("ConstantConditions")
    private CreateCompletionByProviderResponse createSavePublishResponse(String promptId, OffsetDateTime dateTime, LlmProvider llmProvider, ChatResponseTimer response) {
        if (Objects.isNull(response.chatResponse().getResult())) {
            // No answer received that might happen if LLM is not available
            return CreateCompletionByProviderResponse.builder()
                    .interactionCompletion(
                            InteractionCompletion.builder()
                                    .completionId(createUniqueId())
                                    .promptId(promptId)
                                    .llmProvider(llmProvider)
                                    .build())
                    .build();
        }
        //vertex sometimes answers with an empty model value which is not accepted by db constraints
        String llm = lookupModel(llmProvider, response);
        CreateCompletionByProviderResponse createCompletionByProviderResponse = CreateCompletionByProviderResponse.builder()
                .interactionCompletion(
                        InteractionCompletion.builder()
                                .completionId(createUniqueId())
                                .promptId(promptId)
                                .completion(response.chatResponse().getResults().getFirst().getOutput().getText())
                                .llm(llm)
                                .llmProvider(llmProvider)
                                .tokenUsage(response.chatResponse().getMetadata().getUsage().toString())
                                .executionTimeMs(response.executionTimeMs())
                                .completedAt(dateTime)
                                .build())
                .build();
        savePublishResponse(promptId, createCompletionByProviderResponse);
        return createCompletionByProviderResponse;
    }

    private String lookupModel(LlmProvider llmProvider, ChatResponseTimer response) {
        if (StringUtils.hasText(response.chatResponse().getMetadata().getModel())) {
            //prefer the real-time provided name
            return response.chatResponse().getMetadata().getModel();
        } else if (StringUtils.hasText(appProperties.models().get(llmProvider).llmModelName())) {
            //use non-null configured name (GEMINI case)
            return appProperties.models().get(llmProvider).llmModelName();
        }
        //else unknown
        return UNKNOWN_MODEL_NAME;
    }

    private void savePublishResponse(String promptId, CreateCompletionByProviderResponse createCompletionByProviderResponse) {
        LlmCompletion llmCompletion = createLlmCompletion(promptId, createCompletionByProviderResponse);
        llmCompletionRepository.save(llmCompletion);
        applicationEventPublisher.publishEvent(llmCompletion);
    }

    private LlmPrompt createLlmPrompt(String id, String prompt, String chatId) {
        return LlmPrompt.builder()
                .promptId(id)
                .prompt(prompt)
                .chatId(chatId)
                .promptedAt(OffsetDateTime.now())
                .build();
    }

    private LlmCompletion createLlmCompletion(String promptId, CreateCompletionByProviderResponse response) {
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
