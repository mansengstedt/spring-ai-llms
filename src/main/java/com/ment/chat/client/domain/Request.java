package com.ment.chat.client.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString

public class Request {

    @Id
    @Column(name = "REQUEST_ID", nullable = false, updatable = false, length = 36)
    private String requestId;

    @Column(name = "PROMPT", nullable = false, updatable = false, length = 2048)
    private String prompt;
}
