package tn.iset.investplatformpfe.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_send_records")
public class EmailSendRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long prospectId;
    private String prospectName;
    private String prospectEmail;

    @Column(columnDefinition = "TEXT")
    private String message;

    private String subject;
    private String type; // "AI" ou "CUSTOM"
    private LocalDateTime sentAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProspectId() { return prospectId; }
    public void setProspectId(Long prospectId) { this.prospectId = prospectId; }
    public String getProspectName() { return prospectName; }
    public void setProspectName(String prospectName) { this.prospectName = prospectName; }
    public String getProspectEmail() { return prospectEmail; }
    public void setProspectEmail(String prospectEmail) { this.prospectEmail = prospectEmail; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}