package com.ment.chat.client.service;

import com.ment.chat.client.config.AppProperties;
import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.model.in.CreateCompletionRequest;
import com.ment.chat.client.model.out.CreateCompletionResponse;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@RequiredArgsConstructor
class ChatServiceImplToggleTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private final ChatClient externalClient;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private final ChatClient internalClient;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private final ChatClient dockerClient;

    @Mock
    private ChatClient defaultClient;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private final AppProperties appProperties;

    private final ChatServiceImpl service;

    @Captor
    private ArgumentCaptor<Prompt> promptCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Toggle enabled so message type flips each call
        when(appProperties.toggle().messageType()).thenReturn(true);
    }

    @Test
    void testExternalResponseAndMessageTypeToggle() {
        ChatResponse resp = chatResponse("ext-model", "answer-1");
        when(externalClient.prompt(any(Prompt.class)).call().chatResponse()).thenReturn(resp);

        CreateCompletionRequest req = CreateCompletionRequest.builder()
                .prompt("Hi")
                .chatId("c1")
                .build();

        CreateCompletionResponse r1 = service.createCompletionByProvider(req, LlmProvider.OPENAI);
        CreateCompletionResponse r2 = service.createCompletionByProvider(req, LlmProvider.OPENAI);

        assertEquals("answer-1", r1.getInteractionCompletion());
        assertEquals("ext-model", r1.getInteractionCompletion().getLlm());
        assertNotNull(r1.getInteractionCompletion().getTokenUsage());

        // Capture both prompts to assert alternating message types
        verify(externalClient, times(3)).prompt(promptCaptor.capture());
        List<Prompt> prompts = promptCaptor.getAllValues();
        assertEquals(3, prompts.size());

        Message first = prompts.get(1).getUserMessages().getFirst();
        Message second = prompts.get(2).getSystemMessage();
        assertTrue(first instanceof UserMessage, "First should be UserMessage");
        assertTrue(second instanceof SystemMessage, "Second should be AssistantMessage");
    }

    @Test
    void testInternalResponseNoToggleWhenDisabled() {
        when(appProperties.toggle().messageType()).thenReturn(false); // disable flip
        ChatResponse resp = chatResponse("int-model", "internal");
        when(internalClient.prompt(any(Prompt.class)).call().chatResponse()).thenReturn(resp);

        CreateCompletionRequest req = CreateCompletionRequest.builder().prompt("Ping").build();
        CreateCompletionResponse r = service.createCompletionByProvider(req, LlmProvider.OLLAMA);

        assertEquals("internal", r.getInteractionCompletion());
        verify(internalClient).prompt(promptCaptor.capture());
        Message m = promptCaptor.getValue().getUserMessages().getFirst();
        assertTrue(m instanceof UserMessage);
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
