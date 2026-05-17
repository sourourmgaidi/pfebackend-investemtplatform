package tn.iset.investplatformpfe.Entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String paymentId;          // ID de la transaction Flouci

    @Column(nullable = false)
    private String subscriberEmail;    // Qui paie

    @Column(nullable = false)
    private Double amount;             // 40 TND / mois

    private String currency;           // TND

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;      // PENDING / COMPLETED / FAILED / EXPIRED

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime expiresAt;   // createdAt + 1 mois  ← CLÉ DU SYSTÈME

    private String transactionId;
    private String flouciPaymentId;
    @Column(name = "expiration_notified")
    private Boolean expirationNotified = false;
    @Column(name = "expired_notified")
    private Boolean expiredNotified = false;

    public Boolean getExpiredNotified() { return expiredNotified; }
    public void setExpiredNotified(Boolean expiredNotified) { this.expiredNotified = expiredNotified; }


    public Boolean getExpirationNotified() {
        return expirationNotified;
    }

    public void setExpirationNotified(Boolean expirationNotified) {
        this.expirationNotified = expirationNotified;
    }
}
