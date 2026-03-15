package tn.iset.investplatformpfe.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_read")
    private boolean read = false;

    // ✅ Utilise uniquement les champs role + recipientId pour identifier le destinataire
    @Enumerated(EnumType.STRING)
    @Column(name = "user_role", nullable = false)
    private Role recipientRole;

    @Column(name = "user_id")
    private Long recipientId; // null si broadcast

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "service_type")
    private String serviceType;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

}