package tn.iset.investplatformpfe.Dto;

import java.time.LocalDateTime;

public class ScheduleRequest {
    private String type;       // "PROSPECT" ou "CUSTOM"
    private String message;
    private String subject;
    private LocalDateTime scheduledAt;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
}