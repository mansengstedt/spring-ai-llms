package com.ment.chat.client.controller;

import com.ment.chat.client.aop.LogExecutionTime;
import com.ment.chat.client.config.LlmProvider;
import com.ment.chat.client.model.in.CreateConversationRequest;
import com.ment.chat.client.model.out.CreateCombinedConversationResponse;
import com.ment.chat.client.model.out.CreateConversationResponse;
import com.ment.chat.client.model.out.GetChatResponse;
import com.ment.chat.client.model.out.GetChatServiceStatusResponse;
import com.ment.chat.client.model.out.GetConversationResponse;
import com.ment.chat.client.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("chat")
@Tag(name = "LLM client", description = "client for connecting to several LLM providers")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(
            summary = "Get a haiku for given parameters",
            description = "A haiku with specific style and type."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Haiku was created",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateConversationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @GetMapping("/haiku")
    public ResponseEntity<CreateConversationResponse> haiku(
            @RequestParam LlmProvider provider,
            @Parameter(
                    description = "style of Haiku",
                    schema = @Schema(maxLength = 100)  // This shows in Swagger UI
            )
            @RequestParam(defaultValue = "funny") @Size(max = 100) String style,
            @Parameter(
                    description = "topic of Haiku",
                    schema = @Schema(maxLength = 100)  // This shows in Swagger UI
            )
            @RequestParam(defaultValue = "christmas") @Size(max = 100) String topic) {
        return ResponseEntity.ok(chatService.getChatResponse(
                CreateConversationRequest.builder()
                        .prompt(String.format("Write a %s Haiku about %s!", style, topic))
                        .build(),
                provider
        ));
    }

    @Operation(
            summary = "Prompt an LLM provider",
            description = "Retrieves an answer from the specified LLM provider based on the given prompt."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful answer to prompt",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateConversationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid prompt",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @PostMapping("/llm")
    @LogExecutionTime
    public ResponseEntity<CreateConversationResponse> chatWithLlm(
            @RequestParam LlmProvider provider,
            @RequestBody @Valid CreateConversationRequest conversationRequest) {
        return ResponseEntity.ok(chatService.getChatResponse(conversationRequest, provider));
    }

    @Operation(
            summary = "Prompt all LLM providers",
            description = "Retrieves the answers from all LLM providers based on the given prompt."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful answer to prompt",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateCombinedConversationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid prompt",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @PostMapping("/llm/all")
    @LogExecutionTime
    public ResponseEntity<CreateCombinedConversationResponse> chatWithAll(@RequestBody @Valid CreateConversationRequest conversationRequest) {
        return ResponseEntity.ok(chatService.getCombinedChatResponse(conversationRequest));
    }

    @Operation(
            summary = "Get the answer of an earlier prompt with given request id",
            description = "Retrieves the answers from the LLM providers based on the given request id."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful answer to prompt",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GetConversationResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid prompt",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Unknown request id",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @GetMapping("/request/{request-id}")
    public ResponseEntity<GetConversationResponse> getRequestWithResponses(@PathVariable("request-id") @NotNull String requestId) {
        return ResponseEntity.ok(chatService.getConversation(requestId));
    }

    @Operation(
            summary = "Get the answer of earlier prompts with given chat id",
            description = "Retrieves all the answers in the chat from the LLM providers based on the given chat id."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Answers to given chat id was found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GetChatResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Unknown chat id",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @GetMapping("/chat/{chat-id}")
    public ResponseEntity<GetChatResponse> getChat(@PathVariable("chat-id") @NotNull String chatId) {
        return ResponseEntity.ok(chatService.getChat(chatId));
    }

    @Operation(
            summary = "Get the status of all LLM providers",
            description = "The present status of all LLM providers."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Status was found for all LLM providers",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GetChatServiceStatusResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            )
    })
    @GetMapping("/status")
    public ResponseEntity<GetChatServiceStatusResponse> status() {
        return ResponseEntity.ok(chatService.getChatServiceStatus());
    }
}
