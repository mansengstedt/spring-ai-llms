package com.ment.chat.client.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
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
    private String participantId;

    @Column(name = "REQUEST_ID", nullable = false, updatable = false, length = 36)
    private String requestId;

    @Column(name = "ANSWER", nullable = false, updatable = false, length = 2048)
    private String answer;

    @Column(name = "LLM", nullable = false, updatable = false, length = 100)
    private String llm;

    @Column(name = "TOKEN_USAGE", nullable = false, updatable = false, length = 128)
    private String tokenUsage;

    @Column(name = "EXECUTION_TIME_MS", nullable = false, updatable = false, length = 128)
    private Long executionTimeMs;

    @Column(name = "ANSWERED_AT", nullable = false, updatable = false)
    private OffsetDateTime answeredAt;
}
