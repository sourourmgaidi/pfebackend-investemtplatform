package tn.iset.investplatformpfe.Dto;

// src/main/java/tn/iset/investplatformpfe/DTO/ChatRequest.java

import java.util.List;

public class ChatRequest {
    private List<ChatMessage> messages;

    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }
}
