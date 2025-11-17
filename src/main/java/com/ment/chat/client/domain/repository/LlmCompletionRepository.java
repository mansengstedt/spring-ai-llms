package com.ment.chat.client.domain.repository;

import com.ment.chat.client.domain.LlmCompletion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LlmCompletionRepository extends JpaRepository<LlmCompletion, String> {

    List<LlmCompletion> findByPromptId(String promptId);
}
