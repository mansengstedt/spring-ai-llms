package com.ment.chat.client.controller;

import com.ment.chat.client.aop.LogExecutionTime;
import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.model.in.CreateCompletionRequest;
import com.ment.chat.client.model.out.CreateCombinedCompletionResponse;
import com.ment.chat.client.model.out.CreateCompletionResponse;
import com.ment.chat.client.model.out.GetChatResponse;
import com.ment.chat.client.model.out.GetInteractionResponse;
import com.ment.chat.client.model.out.GetLlmProviderStatusResponse;
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
@Tag(name = "Client of different LLM providers", description = "client for connecting to several LLM providers")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(
            summary = "Create a haiku for given parameters",
            description = "A haiku with specific style and topic."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "A haiku was created",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateCompletionResponse.class)
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
    @PostMapping("/llm/haiku")
    public ResponseEntity<CreateCompletionResponse> createHaiku(
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
        return ResponseEntity.ok(chatService.createCompletion(
                CreateCompletionRequest.builder()
                        .prompt(String.format("Write a %s Haiku about %s!", style, topic))
                        .build(),
                provider
        ));
    }

    @Operation(
            summary = "Create a completion for a specified prompt and given LLM provider",
            description = "Retrieves a interaction completion from the specified LLM provider based on the given interaction prompt."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful answer to interaction prompt",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateCompletionResponse.class)
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
    public ResponseEntity<CreateCompletionResponse> chatWithLlm(
            @RequestParam LlmProvider provider,
            @RequestBody @Valid CreateCompletionRequest completionRequest) {
        return ResponseEntity.ok(chatService.createCompletion(completionRequest, provider));
    }

    @Operation(
            summary = "Create completions for a specified prompt for all LLM providers",
            description = "Retrieves the completions from all LLM providers based on the given prompt."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful completions of prompt",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateCombinedCompletionResponse.class)
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
    public ResponseEntity<CreateCombinedCompletionResponse> chatWithAll(@RequestBody @Valid CreateCompletionRequest completionRequest) {
        return ResponseEntity.ok(chatService.createCombinedCompletion(completionRequest));
    }

    @Operation(
            summary = "Get the completions of an earlier prompt with given id",
            description = "Retrieves the completions from the LLM providers based on the given interaction prompt id."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful completions to prompt id was found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GetInteractionResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid prompt id",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Unknown prompt id",
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
    @GetMapping("/prompt/{prompt-id}")
    public ResponseEntity<GetInteractionResponse> getInteractionWithResponses(
            @Parameter(
                    description = "prompt ID (UUID format)",
                    required = true,
                    example = "550e8400-e29b-41d4-a716-446655440000",
                    schema = @Schema(
                            type = "string",
                            maxLength = 36,
                            minLength = 36,  // UUIDs are exactly 36 chars
                            pattern = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"
                    )
            )
            @PathVariable("prompt-id") @NotNull @Size(min = 36, max = 36) String promptId) {
        return ResponseEntity.ok(chatService.getInteraction(promptId));
    }

    @Operation(
            summary = "Get the completions of earlier prompts with a given chat id",
            description = "Retrieves all the completions in the chat from the LLM providers based on the given chat id."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Completions of given chat id was found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GetChatResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid chat id",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ProblemDetail.class)
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
    public ResponseEntity<GetChatResponse> getChat(
            @Parameter(
                    description = "chat ID (free text)",
                    required = true,
                    example = "my-chat-id-123",
                    schema = @Schema(
                            type = "string",
                            maxLength = 36
                    )
            )
            @PathVariable("chat-id") @NotNull @Size(max = 36) String chatId) {
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
                            schema = @Schema(implementation = GetLlmProviderStatusResponse.class)
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
    public ResponseEntity<GetLlmProviderStatusResponse> getAllStatuses() {
        return ResponseEntity.ok(chatService.getLlmProviderStatus());
    }
}
