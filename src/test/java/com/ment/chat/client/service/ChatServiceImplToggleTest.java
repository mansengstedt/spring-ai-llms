package com.ment.chat.client.service;

import com.ment.chat.client.config.AppProperties;
import com.ment.chat.client.domain.repository.LlmCompletionRepository;
import com.ment.chat.client.domain.repository.LlmPromptRepository;
import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.model.in.CreateCompletionRequest;
import com.ment.chat.client.model.out.CreateCompletionResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceImplToggleTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient mockedChatClient;

    @Mock
    private LlmPromptRepository llmPromptRepository;

    @Mock
    private LlmCompletionRepository llmCompletionRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private AppProperties appProperties;

    @InjectMocks
    private ChatServiceImpl service;

    @Captor
    private ArgumentCaptor<Prompt> promptCaptor;

    @BeforeEach
    void setUp() throws Exception {
        try (AutoCloseable _ = MockitoAnnotations.openMocks(this)) {
            var method = ChatServiceImpl.class.getDeclaredMethod("chatClientMap");
            method.setAccessible(true);
            method.invoke(service);
        }
    }

    @ParameterizedTest
    @MethodSource("toggle")
    void testInternalResponseAndMessageTypeNoToggle(Boolean toggle, LlmProvider llmProvider) {
        String model = "model-1";
        String answer1 = "answer1";
        String prompt1 = "Ping";
        String prompt2 = "Pong";
        String chatId = "c1";
        ChatResponse resp1 = chatResponse(model, answer1);
        ChatClient chatClient = mockedChatClient;

        when(appProperties.toggle().messageType()).thenReturn(toggle);
        when(chatClient.prompt(any(Prompt.class)).call().chatResponse()).thenReturn(resp1);

        CreateCompletionRequest req1 = CreateCompletionRequest.builder()
                .prompt(prompt1)
                .chatId(chatId)
                .build();

        CreateCompletionRequest req2 = CreateCompletionRequest.builder()
                .prompt(prompt2)
                .chatId(chatId)
                .build();

        CreateCompletionResponse r1 = service.createCompletionByProvider(req1, llmProvider);
        CreateCompletionResponse r2 = service.createCompletionByProvider(req2, llmProvider);

        verify(chatClient, times(3)).prompt(promptCaptor.capture());

        Assertions.assertThat(r1.getInteractionCompletion().getCompletion()).isEqualTo(answer1);
        Assertions.assertThat(r2.getInteractionCompletion().getCompletion()).isEqualTo(answer1); //any prompt returns answer1
        Assertions.assertThat(r1.getInteractionCompletion().getLlm()).isEqualTo(model);
        Assertions.assertThat(r1.getInteractionCompletion().getTokenUsage()).isNotNull();


        List<Prompt> prompts = promptCaptor.getAllValues();
        Assertions.assertThat(prompts.size()).isEqualTo(3);

        Message first = prompts.get(1).getUserMessages().getFirst();
        Message second = prompts.get(2).getSystemMessage();
        Assertions.assertThat(first).isInstanceOf(UserMessage.class);
        Assertions.assertThat(second).isInstanceOf(SystemMessage.class);

        Assertions.assertThat(prompts.get(1).getUserMessage().getText()).isEqualTo(prompt1);
        if (toggle) {
            //toggling from MessageType.USER to MessageType.ASSISTANT
            Assertions.assertThat(prompts.get(2).getUserMessage().getText()).isEqualTo("");
            Assertions.assertThat(prompts.get(2).toString()).contains("messageType=ASSISTANT");
        } else {
            //no toggling: always MessageType.USER
            Assertions.assertThat(prompts.get(2).getUserMessage().getText()).isEqualTo(prompt2);
            Assertions.assertThat(prompts.get(2).toString()).contains("messageType=USER");
        }
    }

    static Stream<Arguments> toggle() {
        return Stream.of(
                Arguments.of(true, LlmProvider.OPENAI), // enable flip from USER to ASSISTANT
                Arguments.of(false, LlmProvider.OPENAI), // disable flip, always USER, NO ASSISTANT
                Arguments.of(true, LlmProvider.ANTHROPIC),
                Arguments.of(false, LlmProvider.ANTHROPIC),
                Arguments.of(true, LlmProvider.DOCKER),
                Arguments.of(false, LlmProvider.DOCKER),
                Arguments.of(true, LlmProvider.OLLAMA),
                Arguments.of(false, LlmProvider.OLLAMA)
        );
    }

    private ChatResponse chatResponse(String model, String answer) {
        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .model(model)
                .usage(new DefaultUsage(5, 10))
                .build();
        Generation gen = new Generation(new AssistantMessage(answer));
        return ChatResponse.builder()
                .metadata(metadata)
                .generations(List.of(gen))
                .build();
    }
}
