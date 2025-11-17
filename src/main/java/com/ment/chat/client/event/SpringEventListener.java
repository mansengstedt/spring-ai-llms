package com.ment.chat.client.event;

import com.ment.chat.client.domain.LlmPrompt;
import com.ment.chat.client.domain.LlmCompletion;
import com.ment.chat.client.domain.repository.LlmPromptRepository;
import com.ment.chat.client.domain.repository.LlmCompletionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SpringEventListener {

    private final LlmPromptRepository llmPromptRepository;

    private final LlmCompletionRepository llmCompletionRepository;

    @EventListener
    public void handlePrompt(LlmPrompt llmPrompt) {
        llmPromptRepository.findById(llmPrompt.getPromptId())
                .ifPresentOrElse(
                        existingLlmPrompt -> log.info("LlmPrompt with ID {} saved.", existingLlmPrompt.getPromptId()),
                        () -> log.info("LlmPrompt with ID {} not saved yet.", llmPrompt.getPromptId())
                );
    }

    @EventListener
    public void handleCompletion(LlmCompletion llmCompletion) {
        llmCompletionRepository.findById(llmCompletion.getCompletionId())
                .ifPresentOrElse(
                        existingLlmCompletion -> log.info("LlmCompletion with ID {} answering LlmPrompt {} saved.", existingLlmCompletion.getCompletionId(), existingLlmCompletion.getPromptId()),
                        () -> log.info("LlmCompletion with ID {} not saved yet.", llmCompletion.getCompletionId())
                );
    }
}
