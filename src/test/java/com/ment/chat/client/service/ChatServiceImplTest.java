package com.ment.chat.client.service;

import com.ment.chat.client.config.AppProperties;
import com.ment.chat.client.domain.repository.LlmCompletionRepository;
import com.ment.chat.client.domain.repository.LlmPromptRepository;
import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.model.in.CreateCompletionRequest;
import com.ment.chat.client.model.out.CreateCompletionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.DefaultChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Disabled
@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    private ChatClient chatClient;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Mock
    private LlmPromptRepository llmPromptRepository;

    @Mock
    private LlmCompletionRepository llmCompletionRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private AppProperties appProperties;

    @BeforeEach //to handle postconstruct in test
    void setUp() throws Exception {
        var method = ChatServiceImpl.class.getDeclaredMethod("chatClientMap");
        method.setAccessible(true);
        method.invoke(chatService);
    }

    @Test
    void testChatResponse() {
        CreateCompletionRequest request = CreateCompletionRequest.builder()
                .prompt("Test interactionPrompt")
                .style("Test style")
                .chatId("test-id")
                .build();

        when(appProperties.toggle()).thenReturn(new AppProperties.Toggle(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE));
        when(chatClient.prompt(any(Prompt.class))).thenReturn(
                new DefaultChatClient.DefaultChatClientRequestSpec(
                        null, //can't be null
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null));

        ChatResponse chatResponse = mockChatResponse("Test model", new DefaultUsage(10, 20), "Test answer");
        when(chatClient.prompt(any(Prompt.class)).call().chatResponse()).thenReturn(chatResponse);

        CreateCompletionResponse response = chatService.createCompletionByProvider(request, LlmProvider.OPENAI);

        assertEquals("Test answer", response.getInteractionCompletion());
        assertEquals("Test model", response.getInteractionCompletion().getLlm());
        assertEquals("Test tokenUsage", response.getInteractionCompletion().getTokenUsage());
        verify(chatClient, times(1)).prompt(request.createPrompt());
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
                //.generations(List.of(gen))
                .build();
    }
}