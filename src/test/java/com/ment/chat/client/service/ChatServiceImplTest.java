package com.ment.chat.client.service;

import com.ment.chat.client.model.in.ConversationRequest;
import com.ment.chat.client.model.out.ConversationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceImplTest {

    @Mock
    private ChatClient internalClient;

    @Mock
    private ChatClient externalClient;

    @Mock
    private ChatClient dockerClient;

    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        chatService = new ChatServiceImpl(null, null, null, null);
        chatService.internalChatClient = internalClient;
        chatService.externalChatClient = externalClient;
        chatService.dockerChatClient = dockerClient;
    }

    @Test
    void testGetExternalChatResponse() {
        ConversationRequest request = ConversationRequest.builder()
                .prompt("Test prompt")
                .style("Test style")
                .chatId("test-id")
                .build();

        ChatResponse chatResponse = mockChatResponse("Test model", new DefaultUsage(10, 20), "Test answer");
        when(externalClient.prompt(anyString()).call().chatResponse()).thenReturn(chatResponse);

        ConversationResponse response = chatService.getExternalChatResponse(request);

        assertEquals("Test answer", response.getAnswer());
        assertEquals("Test model", response.getLlm());
        assertEquals("Test tokenUsage", response.getTokenUsage());
        verify(externalClient, times(1)).prompt(request.createPrompt());
    }

    @Test
    void testGetInternalChatResponse() {
        ConversationRequest request = ConversationRequest.builder()
                .prompt("Internal prompt")
                .build();

        ChatResponse chatResponse = mockChatResponse("Internal model", new DefaultUsage(10, 20), "Internal answer");
        when(internalClient.prompt(anyString()).call().chatResponse()).thenReturn(chatResponse);

        ConversationResponse response = chatService.getInternalChatResponse(request);

        assertEquals("Internal answer", response.getAnswer());
        assertEquals("Internal model", response.getLlm());
        assertEquals("Internal tokenUsage", response.getTokenUsage());
        verify(internalClient, times(1)).prompt(request.createPrompt());
    }

    @Test
    void testGetDockerChatResponse() {
        ConversationRequest request = ConversationRequest.builder()
                .prompt("Docker prompt")
                .build();

        ChatResponse chatResponse = mockChatResponse("Docker model", new DefaultUsage(10, 20), "Docker answer");
        when(dockerClient.prompt(anyString()).call().chatResponse()).thenReturn(chatResponse);

        ConversationResponse response = chatService.getDockerChatResponse(request);

        assertEquals("Docker answer", response.getAnswer());
        assertEquals("Docker model", response.getLlm());
        assertEquals("Docker tokenUsage", response.getTokenUsage());
        verify(dockerClient, times(1)).prompt(request.createPrompt());
    }

    private ChatResponse mockChatResponse(String model, Usage usage, String answer) {
        ChatResponseMetadata metadata = ChatResponseMetadata.builder()
                .model(model)
                .usage(usage)
                .build();

        Generation gen = new Generation(new AssistantMessage(answer));
        ChatResponse result = ChatResponse.builder()
                .generations(List.of(gen))
                .build();

        return ChatResponse.builder()
                .metadata(metadata)
                .from(result)
                .generations(List.of(gen))
                .build();
    }
}