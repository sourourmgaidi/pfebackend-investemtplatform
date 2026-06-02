package tn.iset.investplatformpfe.Dto;

// src/main/java/tn/iset/investplatformpfe/DTO/ChatResponse.java

public class ChatResponse {
    private String reply;

    public ChatResponse(String reply) { this.reply = reply; }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
}