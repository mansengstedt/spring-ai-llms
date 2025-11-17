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
import com.ment.chat.client.model.out.InteractionCompletion;
import com.ment.chat.client.model.out.CreateCombinedCompletionResponse;
import com.ment.chat.client.model.out.CreateCompletionResponse;
import com.ment.chat.client.model.out.GetChatResponse;
import com.ment.chat.client.model.out.GetInteractionResponse;
import com.ment.chat.client.model.out.GetLlmProviderStatusResponse;
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
    public CreateCompletionResponse createCompletion(CreateCompletionRequest completionRequest, LlmProvider llmProvider) {
        return getChatResponse(createUniqueId(), completionRequest, llmProvider, chatClientMap.get(llmProvider));
    }

    @Override
    public CreateCombinedCompletionResponse createCombinedCompletion(CreateCompletionRequest completionRequest) {
        return getChatResponses(createUniqueId(), completionRequest, chatClientMap);
    }

    @Override
    public GetInteractionResponse getInteraction(String promptId) {
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
    public GetChatResponse getChat(String chatId) {
        List<LlmPrompt> llmPrompts = llmPromptRepository.findByChatId(chatId);
        if (llmPrompts.isEmpty()) {
            throw new ChatNotFoundException(chatId);
        }

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
                .toList().stream()
                .sorted()
                .toList();

        return GetChatResponse.builder()
                .interactions(responses)
                .build();
    }

    @Override
    public GetLlmProviderStatusResponse getLlmProviderStatus() {
        return extractStatusFrom(createCombinedCompletion(CreateCompletionRequest.builder()
                .prompt("ping LLM to check status")
                .chatId("ping-chat-service-status")
                .build()));
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
                        Objects.requireNonNull(createRequestSpec(completionRequest, chatClient, message)
                                .call()
                                .chatResponse())))
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
        applicationEventPublisher.publishEvent(llmPrompt);
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
                                    .llmProvider(llmProvider)
                                    .build())
                    .build();
        }
        CreateCompletionResponse createCompletionResponse = CreateCompletionResponse.builder()
                .interactionCompletion(
                        InteractionCompletion.builder()
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
                .completionId(createUniqueId())
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
