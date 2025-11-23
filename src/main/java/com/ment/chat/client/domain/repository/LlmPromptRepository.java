package com.ment.chat.client.domain.repository;

import com.ment.chat.client.domain.LlmPrompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LlmPromptRepository extends JpaRepository<LlmPrompt, String> {

    LlmPrompt findByPromptId(String promptId);

    List<LlmPrompt> findByChatId(String chatId);

    List<LlmPrompt> findByPromptContains(String partOfPrompt);
}
