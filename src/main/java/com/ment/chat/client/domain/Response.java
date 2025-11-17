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

public class Response {

    @Column(name = "RESPONSE_ID", nullable = false, updatable = false, length = 36)
    @Id
    private String responseId;

    @Column(name = "REQUEST_ID", nullable = false, updatable = false, length = 36)
    private String requestId;

    @Column(name = "ANSWER", nullable = false, updatable = false)
    @Lob
    private String answer;

    @Column(name = "LLM", nullable = false, updatable = false, length = 100)
    private String llm;

    @Column(name = "LLM_PROVIDER", nullable = false, updatable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private LlmProvider llmProvider;

    @Column(name = "TOKEN_USAGE", nullable = false, updatable = false, length = 128)
    private String tokenUsage;

    @Column(name = "EXECUTION_TIME_MS", nullable = false, updatable = false)
    private Long executionTimeMs;

    @Column(name = "ANSWERED_AT", nullable = false, updatable = false)
    private OffsetDateTime answeredAt;

    @ManyToOne
    @JoinColumn(name = "REQUEST_ID", referencedColumnName = "REQUEST_ID", nullable = false, insertable = false, updatable = false)
    // Needs to exclude , otherwise a toString() causes an infinite loop between Process and Participant
    @ToString.Exclude
    private Request request;
}
