package tn.iset.investplatformpfe.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_schedule")
public class EmailSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // "PROSPECT" (broadcast IA) ou "CUSTOM" (message personnalisé)
    @Column(nullable = false)
    private String type; // "PROSPECT" | "CUSTOM"

    // Le message final à envoyer (ou rawMessage pour preview IA)
    @Column(columnDefinition = "TEXT")
    private String message;

    private String subject;

    // Date/heure planifiée d'envoi
    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    // Statut: PENDING | SENT | CANCELLED | FAILED
    @Column(nullable = false)
    private String status = "PENDING";

    private String resultMessage;
    private LocalDateTime executedAt;

    // Getters & Setters
    public Long getId() { return id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }
    public LocalDateTime getExecutedAt() { return executedAt; }
    public void setExecutedAt(LocalDateTime executedAt) { this.executedAt = executedAt; }
}