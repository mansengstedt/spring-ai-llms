package com.ment.chat.client.service;

import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.model.in.CreateCompletionRequest;
import com.ment.chat.client.model.out.CreateCompletionResponse;
import lombok.RequiredArgsConstructor;
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

@RequiredArgsConstructor
class ChatServiceImplTest {

    @Mock
    private final ChatClient internalClient;

    @Mock
    private final ChatClient externalClient;

    @Mock
    private final ChatClient dockerClient;

    private ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetOpenAiChatResponse() {
        CreateCompletionRequest request = CreateCompletionRequest.builder()
                .prompt("Test interactionPrompt")
                .style("Test style")
                .chatId("test-id")
                .build();

        ChatResponse chatResponse = mockChatResponse("Test model", new DefaultUsage(10, 20), "Test answer");
        when(externalClient.prompt(anyString()).call().chatResponse()).thenReturn(chatResponse);

        CreateCompletionResponse response = chatService.createCompletion(request, LlmProvider.OPENAI);

        assertEquals("Test answer", response.getInteractionCompletion());
        assertEquals("Test model", response.getInteractionCompletion().getLlm());
        assertEquals("Test tokenUsage", response.getInteractionCompletion().getTokenUsage());
        verify(externalClient, times(1)).prompt(request.createPrompt());
    }

    @Test
    void testGetOllamaChatResponse() {
        CreateCompletionRequest request = CreateCompletionRequest.builder()
                .prompt("Internal interactionPrompt")
                .build();

        ChatResponse chatResponse = mockChatResponse("Internal model", new DefaultUsage(10, 20), "Internal answer");
        when(internalClient.prompt(anyString()).call().chatResponse()).thenReturn(chatResponse);

        CreateCompletionResponse response = chatService.createCompletion(request, LlmProvider.OLLAMA);

        assertEquals("Internal answer", response.getInteractionCompletion());
        assertEquals("Internal model", response.getInteractionCompletion().getLlm());
        assertEquals("Internal tokenUsage", response.getInteractionCompletion().getTokenUsage());
        verify(internalClient, times(1)).prompt(request.createPrompt());
    }

    @Test
    void testGetDockerChatResponse() {
        CreateCompletionRequest request = CreateCompletionRequest.builder()
                .prompt("Docker interactionPrompt")
                .build();

        ChatResponse chatResponse = mockChatResponse("Docker model", new DefaultUsage(10, 20), "Docker answer");
        when(dockerClient.prompt(anyString()).call().chatResponse()).thenReturn(chatResponse);

        CreateCompletionResponse response = chatService.createCompletion(request, LlmProvider.DOCKER);

        assertEquals("Docker answer", response.getInteractionCompletion());
        assertEquals("Docker model", response.getInteractionCompletion().getLlm());
        assertEquals("Docker tokenUsage", response.getInteractionCompletion().getTokenUsage());
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