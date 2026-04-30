package com.example.aichatbot.controller;

import com.example.aichatbot.dto.ChatRequest;
import com.example.aichatbot.dto.ChatResponse;
import com.example.aichatbot.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    // Phase 11.1：Streaming API
    @PostMapping("/stream")
    public SseEmitter stream(@Valid @RequestBody ChatRequest request) {
        return chatService.streamChat(request);
    }

    @GetMapping("/health")
    public String health() {
        return "AI Chatbot Backend is running";
    }
}