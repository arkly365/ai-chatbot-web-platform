package com.example.aichatbot.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ChatResponse {

    private String reply;
    private String role;
    private List<String> quickReplies;
    private LocalDateTime createdAt;

    public ChatResponse() {
    }

    public ChatResponse(String reply, String role, List<String> quickReplies, LocalDateTime createdAt) {
        this.reply = reply;
        this.role = role;
        this.quickReplies = quickReplies;
        this.createdAt = createdAt;
    }

    public String getReply() {
        return reply;
    }

    public String getRole() {
        return role;
    }

    public List<String> getQuickReplies() {
        return quickReplies;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setQuickReplies(List<String> quickReplies) {
        this.quickReplies = quickReplies;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}