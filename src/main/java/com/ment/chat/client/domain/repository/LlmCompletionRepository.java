package com.ment.chat.client.domain.repository;

import com.ment.chat.client.domain.LlmCompletion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LlmCompletionRepository extends JpaRepository<LlmCompletion, String> {

    List<LlmCompletion> findByCompletionContains(String partOfCompletion);
}
