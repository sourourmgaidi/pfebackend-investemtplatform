package tn.iset.investplatformpfe.Entity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import tn.iset.investplatformpfe.Entity.Role;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "multirole_chats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class MultiRoleChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @Column(name = "sent_at", nullable = false)
    @CreatedDate
    private LocalDateTime sentAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    private Role senderType;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "receiver_type", nullable = false)
    private Role receiverType;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonIgnoreProperties({"chat"})
    @OrderBy("uploadedAt ASC")
    private List<MultiRoleChatAttachment> attachments = new ArrayList<>();

    @Column(name = "deleted_by_sender")
    private Boolean deletedBySender = false;

    @Column(name = "deleted_by_receiver")
    private Boolean deletedByReceiver = false;

    public void addAttachment(MultiRoleChatAttachment attachment) {
        attachments.add(attachment);
        attachment.setChat(this);
    }
}
