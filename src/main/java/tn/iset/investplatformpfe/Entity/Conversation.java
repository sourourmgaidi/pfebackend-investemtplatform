package tn.iset.investplatformpfe.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversation")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String senderRole;

    @Column(nullable = false)
    private String senderEmail;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "recipient_role", nullable = false)
    private String recipientRole;

    @Column(name = "last_message")
    private String lastMessage;

    @Column(name = "last_message_date")
    private LocalDateTime lastMessageDate;

    @Column(name = "sender_viewed")
    private boolean senderViewed = true;

    @Column(name = "partner_viewed")
    private boolean partnerViewed = true;



    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<Message> messages = new ArrayList<>();

    // Constructeur vide (requis par JPA)
    public Conversation() {}

    // ✅ Constructeur 4 paramètres - celui utilisé dans MessagerieService
    public Conversation(String senderRole, String senderEmail,
                        String recipientEmail, String recipientRole) {
        this.senderRole = senderRole;
        this.senderEmail = senderEmail;
        this.recipientEmail = recipientEmail;
        this.recipientRole = recipientRole;
        this.lastMessageDate = LocalDateTime.now();
        this.senderViewed = true;
        this.partnerViewed = false;
    }

    // Constructeur complet (optionnel)
    public Conversation(Long id, String senderRole, String senderEmail,
                        String recipientEmail, String recipientRole,
                        String lastMessage, LocalDateTime lastMessageDate,
                        boolean senderViewed, boolean partnerViewed,
                        List<Message> messages) {
        this.id = id;
        this.senderRole = senderRole;
        this.senderEmail = senderEmail;
        this.recipientEmail = recipientEmail;
        this.recipientRole = recipientRole;
        this.lastMessage = lastMessage;
        this.lastMessageDate = lastMessageDate;
        this.senderViewed = senderViewed;
        this.partnerViewed = partnerViewed;
        this.messages = messages;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public String getSenderEmail() { return senderEmail; }
    public void setSenderEmail(String senderEmail) { this.senderEmail = senderEmail; }

    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String recipientEmail) { this.recipientEmail = recipientEmail; }

    public String getRecipientRole() { return recipientRole; }
    public void setRecipientRole(String recipientRole) { this.recipientRole = recipientRole; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public LocalDateTime getLastMessageDate() { return lastMessageDate; }
    public void setLastMessageDate(LocalDateTime lastMessageDate) { this.lastMessageDate = lastMessageDate; }

    public boolean isSenderViewed() { return senderViewed; }
    public void setSenderViewed(boolean senderViewed) { this.senderViewed = senderViewed; }

    public boolean isPartnerViewed() { return partnerViewed; }
    public void setPartnerViewed(boolean partnerViewed) { this.partnerViewed = partnerViewed; }

    public List<Message> getMessages() { return messages; }
    public void setMessages(List<Message> messages) { this.messages = messages; }
}