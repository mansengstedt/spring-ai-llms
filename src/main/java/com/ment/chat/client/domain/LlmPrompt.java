package com.ment.chat.client.domain;


import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString

public class LlmPrompt {

    @Id
    @Column(name = "PROMPT_ID", nullable = false, updatable = false, length = 36)
    private String promptId;

    @Column(name = "PROMPT", nullable = false, updatable = false)
    @Lob
    private String prompt;

    @Column(name = "SESSION_ID", nullable = false, updatable = false, length = 64)
    private String sessionId;

    @Column(name = "CHAT_ID", updatable = false, length = 128)
    private String chatId;

    @Column(name = "PROMPTED_AT", nullable = false, updatable = false)
    private OffsetDateTime promptedAt;

    @OneToMany(mappedBy = "llmPrompt", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, fetch = FetchType.EAGER)
    @OrderBy("llm asc")
    private List<LlmCompletion> completions;

}
