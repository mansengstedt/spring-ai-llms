package com.ment.chat.client.service;

import com.ment.chat.client.config.AppProperties;
import com.ment.chat.client.domain.LlmCompletion;
import com.ment.chat.client.domain.LlmPrompt;
import com.ment.chat.client.domain.repository.LlmCompletionRepository;
import com.ment.chat.client.domain.repository.LlmPromptRepository;
import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.model.in.CreateCompletionRequest;
import com.ment.chat.client.model.out.CreateCompletionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.ApplicationEventPublisher;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Disabled
@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    //mocking client, deep stubs make it easier
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
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

    @BeforeEach //to handle post construct annotation in test
    void setUp() throws Exception {
        var method = ChatServiceImpl.class.getDeclaredMethod("chatClientMap");
        method.setAccessible(true);
        method.invoke(chatService);
    }

    @Test
    void testChatResponse() {
        CreateCompletionRequest request = CreateCompletionRequest.builder()
                .prompt("Test interactionPrompt")
                .chatId("test-id")
                .build();

        ChatResponse chatResponse = mockChatResponse("Test model", new DefaultUsage(10, 20), "Test answer");

        when(appProperties.toggle()).thenReturn(new AppProperties.Toggle(Boolean.FALSE, null, null));
        when(chatClient.prompt(any(Prompt.class)).call().chatResponse()).thenReturn(chatResponse);

        CreateCompletionResponse response = chatService.createCompletionByProvider(request, LlmProvider.OPENAI);

        assertThat(response.getInteractionCompletion().getLlmProvider()).isEqualTo(LlmProvider.OPENAI);
        assertThat(response.getInteractionCompletion().getCompletion()).isEqualTo("Test answer");
        assertThat(response.getInteractionCompletion().getCompletedAt()).isBefore(OffsetDateTime.now());
        assertThat(response.getInteractionCompletion().getExecutionTimeMs()).isGreaterThanOrEqualTo(0L);
        assertThat(response.getInteractionCompletion().getLlm()).isEqualTo("Test model");
        assertThat(response.getInteractionCompletion().getTokenUsage()).contains("promptTokens=10");
        assertThat(response.getInteractionCompletion().getTokenUsage()).contains("completionTokens=20");

        verify(chatClient, times(1)).prompt(any(Prompt.class));
        verify(llmPromptRepository, times(1)).save(any());
        verify(llmCompletionRepository, times(1)).save(any());
        verify(applicationEventPublisher, times(1)).publishEvent(isNull(LlmPrompt.class));
        verify(applicationEventPublisher, times(1)).publishEvent(any(LlmCompletion.class));
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
                .from(result)
                .metadata(metadata)
                .build();
    }
}