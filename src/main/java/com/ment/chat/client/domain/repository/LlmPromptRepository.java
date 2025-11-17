package com.ment.chat.client.domain.repository;

import com.ment.chat.client.domain.LlmPrompt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LlmPromptRepository extends JpaRepository<LlmPrompt, String> {

    List<LlmPrompt> findByChatId(String chatId);
}
