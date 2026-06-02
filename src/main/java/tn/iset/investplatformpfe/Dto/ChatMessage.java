package tn.iset.investplatformpfe.Dto;

// src/main/java/tn/iset/investplatformpfe/DTO/ChatMessage.java

public class ChatMessage {
    private String role;    // "user" ou "assistant"
    private String content;

    public ChatMessage() {}

    public ChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}