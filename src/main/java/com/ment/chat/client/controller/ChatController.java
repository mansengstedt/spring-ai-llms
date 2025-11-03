package com.ment.chat.client.controller;

import com.ment.chat.client.aop.LogExecutionTime;
import com.ment.chat.client.model.in.ConversationRequest;
import com.ment.chat.client.model.out.ConversationResponse;
import com.ment.chat.client.model.out.FindConversationResponse;
import com.ment.chat.client.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/haiku")
    public ResponseEntity<ConversationResponse> haiku(@RequestParam(defaultValue = "funny") String style,
                                                      @RequestParam(defaultValue = "christmas") String topic) {
        return ResponseEntity.ok(chatService.getDockerChatResponse(
                ConversationRequest.builder()
                        .prompt(String.format("Write a %s Haiku about %s!", style, topic))
                        .build()
        ));
    }

    @PostMapping("/internal")
    @LogExecutionTime
    public ResponseEntity<ConversationResponse> chatInternal(@RequestBody @Valid ConversationRequest conversationRequest) {
        return ResponseEntity.ok(chatService.getInternalChatResponse(conversationRequest));
    }

    @PostMapping("/external")
    @LogExecutionTime
    public ResponseEntity<ConversationResponse> chatExternal(@RequestBody @Valid ConversationRequest conversationRequest) {
        return ResponseEntity.ok(chatService.getExternalChatResponse(conversationRequest));
    }

    @PostMapping("/docker")
    @LogExecutionTime
    public ResponseEntity<ConversationResponse> chatWithDocker(@RequestBody @Valid ConversationRequest conversationRequest) {
        return ResponseEntity.ok(chatService.getDockerChatResponse(conversationRequest));
    }

    @PostMapping("/combine")
    @LogExecutionTime
    public ResponseEntity<ConversationResponse> chatWithAll(@RequestBody @Valid ConversationRequest conversationRequest) {
        return ResponseEntity.ok(chatService.getChatResponses(conversationRequest));
    }

    @GetMapping("/request/{request-id}")
    public ResponseEntity<FindConversationResponse> getRequestWithResponses(@PathVariable("request-id") String requestId) {
        return ResponseEntity.ok(chatService.getRequestWithResponses(requestId));
    }

    @GetMapping("/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok("Chat Service is running.");
    }
}
