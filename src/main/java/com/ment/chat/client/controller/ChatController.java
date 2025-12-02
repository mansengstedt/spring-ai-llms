package com.ment.chat.client.controller;

import com.ment.chat.client.aop.LogExecutionTime;
import com.ment.chat.client.model.enums.LlmProvider;
import com.ment.chat.client.model.in.CreateCompletionRequest;
import com.ment.chat.client.model.out.CreateCombinedCompletionResponse;
import com.ment.chat.client.model.out.CreateCompletionResponse;
import com.ment.chat.client.model.out.GetChatResponse;
import com.ment.chat.client.model.out.GetInteractionResponse;
import com.ment.chat.client.model.out.GetInteractionsResponse;
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

import static com.ment.chat.client.controller.ChatController.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;

@RestController
@RequestMapping(
        value = BASE_PATH,
        produces = {APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE})
@Tag(name = "Client of different LLM providers", description = "Client for connecting to several LLM providers like OpenAI etc.")
@Validated
public class ChatController {

    public static final String BASE_PATH = "/chat";
    public static final String LLM_PATH = "/llm";
    public static final String LLM_ALL_PATH = LLM_PATH + "/all";
    public static final String LLM_HAIKU_PATH = LLM_PATH + "/haiku";
    public static final String PROMPT_PATH = "/prompt";
    public static final String PROMPT_CONTAINS_PATH = PROMPT_PATH + "/contains";
    public static final String COMPLETION_PATH = "/completion";
    public static final String COMPLETION_CONTAINS_PATH = COMPLETION_PATH + "/contains";
    public static final String CHAT_PATH = "chat";
    public static final String STATUS_PATH = "status";

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
    @PostMapping(value = LLM_HAIKU_PATH)
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
        return ResponseEntity.ok(chatService.createCompletionByProvider(
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
    @PostMapping(value = LLM_PATH, consumes = {APPLICATION_JSON_VALUE})
    @LogExecutionTime
    public ResponseEntity<CreateCompletionResponse> createCompletionByProvider(
            @RequestParam LlmProvider provider,
            @RequestBody @Valid CreateCompletionRequest completionRequest) {
        return ResponseEntity.ok(chatService.createCompletionByProvider(completionRequest, provider));
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
    @PostMapping(value = LLM_ALL_PATH, consumes = {APPLICATION_JSON_VALUE})
    @LogExecutionTime
    public ResponseEntity<CreateCombinedCompletionResponse> createCompletionsByAllProviders(@RequestBody @Valid CreateCompletionRequest completionRequest) {
        return ResponseEntity.ok(chatService.createCompletionsByAllProviders(completionRequest));
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
    @GetMapping(value =PROMPT_PATH + "/{prompt-id}")
    public ResponseEntity<GetInteractionResponse> getInteractionByPromptId(
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
        return ResponseEntity.ok(chatService.getInteractionByPromptId(promptId));
    }

    @Operation(
            summary = "Get the completions of an earlier prompt containing the sub-prompt",
            description = "Retrieves the completions from the LLM providers based on the given sub-prompt. If no match an empty list is returned"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful completions containing sub-prompt was found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GetChatResponse.class)
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
    @GetMapping(value = PROMPT_CONTAINS_PATH + "/{part-of-prompt}")
    public ResponseEntity<GetChatResponse> getInteractionByPrompt(
            @Parameter(
                    description = "part of prompt (free text)",
                    required = true,
                    example = "Donald Trump",
                    schema = @Schema(
                            type = "string",
                            maxLength = 1000,
                            minLength = 5
                    )
            )
            @PathVariable("part-of-prompt") @NotNull @Size(min = 5, max = 1000) String partOfPrompt) {
        return ResponseEntity.ok(chatService.getChatByPrompt(partOfPrompt));
    }

    @Operation(
            summary = "Get the interactions of earlier interactions containing the sub-completion",
            description = "Retrieves the interactions from the LLM providers based on the given sub-completions. If no match an empty list is returned"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful interactions to sub-completion was found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GetInteractionsResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid sub-completion",
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
    @GetMapping(value = COMPLETION_CONTAINS_PATH + "/{part-of-completion}")
    public ResponseEntity<GetInteractionsResponse> getInteractionsByCompletion(
            @Parameter(
                    description = "part of completion (free text)",
                    required = true,
                    example = "Donald Trump",
                    schema = @Schema(
                            type = "string",
                            maxLength = 1000,
                            minLength = 5
                    )
            )
            @PathVariable("part-of-completion") @NotNull @Size(min = 5, max = 1000) String partOfCompletion) {
        return ResponseEntity.ok(chatService.getInteractionsByCompletion(partOfCompletion));
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
    @GetMapping(value = CHAT_PATH + "/{chat-id}")
    public ResponseEntity<GetChatResponse> getChatByChatId(
            @Parameter(
                    description = "chat Id (free text)",
                    required = true,
                    example = "my-chat-id-123",
                    schema = @Schema(
                            type = "string",
                            maxLength = 36
                    )
            )
            @PathVariable("chat-id") @NotNull @Size(max = 36) String chatId) {
        return ResponseEntity.ok(chatService.getChatByChatId(chatId));
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
    @GetMapping(value = STATUS_PATH)
    public ResponseEntity<GetLlmProviderStatusResponse> getAllStatuses() {
        return ResponseEntity.ok(chatService.getAllProviderStatus());
    }
}
