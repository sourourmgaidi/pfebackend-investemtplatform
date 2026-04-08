package tn.iset.investplatformpfe.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sender_email", nullable = false)
    private String senderEmail;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "sent_date", nullable = false)
    private LocalDateTime sentDate;

    @Column(name = "is_read")
    private boolean read = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    @JsonIgnore
    private Conversation conversation;

    // ✅ Ajout des attachments
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("uploadedAt ASC")
    private List<MessageAttachment> attachments = new ArrayList<>();

    // Constructeur vide requis par JPA
    public Message() {}

    // Constructeur utilisé dans MessagerieService.sendMessage()
    public Message(String content, String senderEmail, String recipientEmail, Conversation conversation) {
        this.content = content;
        this.senderEmail = senderEmail;
        this.recipientEmail = recipientEmail;
        this.conversation = conversation;
        this.sentDate = LocalDateTime.now();
        this.read = false;
    }

    // Méthode utilitaire pour ajouter un attachment
    public void addAttachment(MessageAttachment attachment) {
        attachments.add(attachment);
        attachment.setMessage(this);
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public LocalDateTime getSentDate() { return sentDate; }
    public void setSentDate(LocalDateTime sentDate) { this.sentDate = sentDate; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Conversation getConversation() { return conversation; }
    public void setConversation(Conversation conversation) { this.conversation = conversation; }

    public List<MessageAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<MessageAttachment> attachments) { this.attachments = attachments; }
}