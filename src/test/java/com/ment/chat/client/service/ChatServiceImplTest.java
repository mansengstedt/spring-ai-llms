package com.ment.chat.client.service;

import com.ment.chat.client.client.ChatClientWIthChatMemory;
import com.ment.chat.client.domain.LlmCompletion;
import com.ment.chat.client.domain.LlmPrompt;
import com.ment.chat.client.domain.exception.ChatNotFoundException;
import com.ment.chat.client.domain.repository.LlmCompletionRepository;
import com.ment.chat.client.domain.repository.LlmPromptRepository;
import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.model.in.CreateCompletionByProviderRequest;
import com.ment.chat.client.model.out.CreateCompletionByProviderResponse;
import com.ment.chat.client.model.out.GetSessionMessagesResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.ApplicationEventPublisher;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.ment.chat.client.model.enums.LlmProvider.ANTHROPIC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    //mocking client, deep stubs make it easier
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock
    private ChatClientWIthChatMemory chatClientWIthChatMemory;

    @Mock
    private ChatMemory chatMemory;

    @InjectMocks
    private ChatServiceImpl chatService;

    @Mock
    private LlmPromptRepository llmPromptRepository;

    @Mock
    private LlmCompletionRepository llmCompletionRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;


    @BeforeEach
        //to handle post-construct annotation in the test
    void setUp() throws Exception {
        var method = ChatServiceImpl.class.getDeclaredMethod("chatClientMap");
        method.setAccessible(true);
        method.invoke(chatService);
    }

    @Test
    void testChatClientCall() {
        CreateCompletionByProviderRequest request = CreateCompletionByProviderRequest.builder()
                .prompt("Test interactionPrompt")
                .chatId("test-id")
                .llmProvider(LlmProvider.OPENAI)
                .build();

        ChatResponse chatResponse = mockChatResponse("Test model", new DefaultUsage(10, 20), "Test answer");

        when(chatClientWIthChatMemory.chatClient()).thenReturn(chatClient);
        when(chatClient.prompt(any(Prompt.class)).call().chatResponse()).thenReturn(chatResponse);
        when(llmPromptRepository.findById(any())).thenReturn(Optional.of(LlmPrompt.builder().build()));

        CreateCompletionByProviderResponse response = chatService.createCompletionByProvider(request);

        assertThat(response.getInteractionCompletion().getLlmProvider()).isEqualTo(LlmProvider.OPENAI);
        assertThat(response.getInteractionCompletion().getCompletion()).isEqualTo("Test answer");
        assertThat(response.getInteractionCompletion().getCompletedAt()).isBefore(OffsetDateTime.now());
        assertThat(response.getInteractionCompletion().getExecutionTimeMs()).isGreaterThanOrEqualTo(0L);
        assertThat(response.getInteractionCompletion().getLlm()).isEqualTo("Test model");
        assertThat(response.getInteractionCompletion().getTokenUsage()).contains("promptTokens=10");
        assertThat(response.getInteractionCompletion().getTokenUsage()).contains("completionTokens=20");

        verify(chatClientWIthChatMemory, times(1)).chatClient();
        verify(chatClient, times(1)).prompt(any(Prompt.class));
        verify(llmPromptRepository, times(1)).save(any());
        verify(llmCompletionRepository, times(1)).save(any());
        verify(applicationEventPublisher, times(1)).publishEvent(any(LlmPrompt.class));
        verify(applicationEventPublisher, times(1)).publishEvent(any(LlmCompletion.class));
    }

    @Test
    void testChatMemoryCalls() {
        List<Message> actualMessages = List.of(new AssistantMessage("Test answer"));
        String chatId = "chatId";

        when(chatClientWIthChatMemory.chatMemory()).thenReturn(chatMemory);
        when(chatMemory.get(ArgumentMatchers.contains(chatId))).thenReturn(actualMessages);

        GetSessionMessagesResponse response = chatService.getSessionMessages(chatId, ANTHROPIC);
        chatService.clearSessionHistory(chatId, ANTHROPIC);

        assertThat(response.getSessionMessages()).isEqualTo(actualMessages);

        verify(chatClientWIthChatMemory, times(2)).chatMemory();
        verify(chatMemory, times(1)).get(ArgumentMatchers.contains(chatId));
        verify(chatMemory, times(1)).clear(ArgumentMatchers.contains(chatId));

    }

    @Test
    void testChatMemoryGetCallEmptyList() {
        List<Message> actualMessages = List.of();
        String chatId = "chatId";

        when(chatClientWIthChatMemory.chatMemory()).thenReturn(chatMemory);
        when(chatMemory.get(ArgumentMatchers.contains(chatId))).thenReturn(actualMessages);

        ChatNotFoundException cnfe = assertThrows(ChatNotFoundException.class, () ->
                chatService.getSessionMessages(chatId, ANTHROPIC)
        );

        assertThat(cnfe.getMessage()).isEqualTo(chatId);

        verify(chatClientWIthChatMemory, times(1)).chatMemory();
        verify(chatMemory, times(1)).get(ArgumentMatchers.contains(chatId));

    }

        @SuppressWarnings("SameParameterValue")
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