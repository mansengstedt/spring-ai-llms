package com.ment.chat.client.model.out;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "The stored responses of a request")
@Value
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Jacksonized
public class GetConversationResponse implements Comparable<GetConversationResponse> {

    @Schema(description = "Original Request", requiredMode = Schema.RequiredMode.REQUIRED)
    ConversationRequest request;

    @Schema(description = "Responses of request", requiredMode = Schema.RequiredMode.REQUIRED)
    List<CreateConversationResponse> responses;

    @Override
    public int compareTo(GetConversationResponse other) {
        return this.request.getQueriedAt().compareTo(other.request.getQueriedAt());
    }

    @Schema(description = "Original Request of the Conversation")
    @Value
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @Jacksonized
    public static class ConversationRequest {

        @Schema(description = "Id", example = "f3ccfb64-95d7-4e6a-a9ef-1455a0f81f6e", requiredMode = Schema.RequiredMode.REQUIRED)
        String id;

        @Schema(description = "Prompt", example = "who is presently the best male golf player in Sweden", requiredMode = Schema.RequiredMode.REQUIRED)
        String prompt;

        @Schema(description = "chatId", example = "swedish-golf-player", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String chatId;

        @Schema(description = "Query time of the request", example = "2025-11-14T16:50:01.115667+01:00", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSxxx")
        OffsetDateTime queriedAt;

    }
}
