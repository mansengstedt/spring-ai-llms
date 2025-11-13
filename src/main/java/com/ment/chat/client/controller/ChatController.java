package com.ment.chat.client.controller;

import com.ment.chat.client.aop.LogExecutionTime;
import com.ment.chat.client.model.in.CreateConversationRequest;
import com.ment.chat.client.model.out.*;
import com.ment.chat.client.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/haiku")
    public ResponseEntity<CreateConversationResponse> haiku(@RequestParam(defaultValue = "funny") String style,
                                                            @RequestParam(defaultValue = "christmas") String topic) {
        return ResponseEntity.ok(chatService.getDockerChatResponse(
                CreateConversationRequest.builder()
                        .prompt(String.format("Write a %s Haiku about %s!", style, topic))
                        .build()
        ));
    }

    @PostMapping("/internal")
    @LogExecutionTime
    public ResponseEntity<CreateConversationResponse> chatInternal(@RequestBody @Valid CreateConversationRequest conversationRequest) {
        return ResponseEntity.ok(chatService.getInternalChatResponse(conversationRequest));
    }

    @PostMapping("/external")
    @LogExecutionTime
    public ResponseEntity<CreateConversationResponse> chatExternal(@RequestBody @Valid CreateConversationRequest conversationRequest) {
        return ResponseEntity.ok(chatService.getExternalChatResponse(conversationRequest));
    }

    @PostMapping("/docker")
    @LogExecutionTime
    public ResponseEntity<CreateConversationResponse> chatWithDocker(@RequestBody @Valid CreateConversationRequest conversationRequest) {
        return ResponseEntity.ok(chatService.getDockerChatResponse(conversationRequest));
    }

    @PostMapping("/combine")
    @LogExecutionTime
    public ResponseEntity<CreateCombinedConversationResponse> chatWithAll(@RequestBody @Valid CreateConversationRequest conversationRequest) {
        return ResponseEntity.ok(chatService.getCombinedChatResponse(conversationRequest));
    }

    @GetMapping("/request/{request-id}")
    public ResponseEntity<GetConversationResponse> getRequestWithResponses(@PathVariable("request-id") @NotNull String requestId) {
        return ResponseEntity.ok(chatService.getConversation(requestId));
    }

    @GetMapping("/chat/{chat-id}")
    public ResponseEntity<GetChatResponse> getChat(@PathVariable("chat-id") @NotNull String chatId) {
        return ResponseEntity.ok(chatService.getChat(chatId));
    }

    @GetMapping("/status")
    public ResponseEntity<GetChatServiceStatusResponse> status() {
        return ResponseEntity.ok(chatService.getChatServiceStatus());
    }
}
