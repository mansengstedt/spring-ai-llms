package com.ment.chat.client.domain;



import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "CHAT_ID", updatable = false, length = 128)
    private String chatId;

    @Column(name = "PROMPTED_AT", nullable = false, updatable = false)
    private OffsetDateTime promptedAt;

    @OneToMany(mappedBy = "llmPrompt", cascade = {CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE}, fetch = FetchType.EAGER)
    @OrderBy("llm asc")
    private List<LlmCompletion> completions;

}
