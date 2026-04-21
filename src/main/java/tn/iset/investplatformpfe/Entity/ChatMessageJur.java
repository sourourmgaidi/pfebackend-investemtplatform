package tn.iset.investplatformpfe.Entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class ChatMessageJur{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sender; // "USER" ou "BOT"

    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDateTime timestamp;

    private String  userId; // lié à investor / partner


    public ChatMessageJur() {}

    public ChatMessageJur(Long id, String sender, String message, LocalDateTime timestamp, String userId) {
        this.id = id;
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
