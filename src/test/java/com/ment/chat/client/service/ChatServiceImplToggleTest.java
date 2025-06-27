package com.ment.chat.client.service;

import com.ment.chat.client.config.AppPropererties;
import com.ment.chat.client.model.in.ConversationRequest;
import com.ment.chat.client.model.out.ConversationResponse;
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
    private final AppPropererties appProperties;

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

        ConversationRequest req = ConversationRequest.builder()
                .prompt("Hi")
                .chatId("c1")
                .build();

        ConversationResponse r1 = service.getExternalChatResponse(req);
        ConversationResponse r2 = service.getExternalChatResponse(req);

        assertEquals("answer-1", r1.getAnswer());
        assertEquals("ext-model", r1.getLlm());
        assertNotNull(r1.getTokenUsage());

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

        ConversationRequest req = ConversationRequest.builder().prompt("Ping").build();
        ConversationResponse r = service.getInternalChatResponse(req);

        assertEquals("internal", r.getAnswer());
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
