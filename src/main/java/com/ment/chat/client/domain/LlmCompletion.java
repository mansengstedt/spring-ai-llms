package com.ment.chat.client.domain;

import com.ment.chat.client.model.enums.LlmProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString

public class LlmCompletion {

    @Column(name = "COMPLETION_ID", nullable = false, updatable = false, length = 36)
    @Id
    private String completionId;

    @Column(name = "PROMPT_ID", nullable = false, updatable = false, length = 36)
    private String promptId;

    @Column(name = "COMPLETION", nullable = false, updatable = false)
    @Lob
    private String completion;

    @Column(name = "LLM", nullable = false, updatable = false, length = 100)
    private String llm;

    @Column(name = "LLM_PROVIDER", nullable = false, updatable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private LlmProvider llmProvider;

    @Column(name = "TOKEN_USAGE", nullable = false, updatable = false, length = 128)
    private String tokenUsage;

    @Column(name = "EXECUTION_TIME_MS", nullable = false, updatable = false)
    private Long executionTimeMs;

    @Column(name = "COMPLETED_AT", nullable = false, updatable = false)
    private OffsetDateTime completedAt;

    @ManyToOne
    @JoinColumn(name = "PROMPT_ID", referencedColumnName = "PROMPT_ID", nullable = false, insertable = false, updatable = false)
    // Needs to exclude , otherwise a toString() causes an infinite loop between InteractionPrompt and InteractionCompletion
    @ToString.Exclude
    private LlmPrompt llmPrompt;
}
