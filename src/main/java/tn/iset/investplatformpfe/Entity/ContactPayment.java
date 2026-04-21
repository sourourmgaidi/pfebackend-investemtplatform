package tn.iset.investplatformpfe.Entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contact_payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String paymentId;

    @Column(nullable = false)
    private String payerEmail;

    @Column(nullable = false)
    private String localPartnerEmail;

    @Column(nullable = false)
    private Double amount;

    private String currency;
    private String serviceId;

    @Column(length = 2000)
    private String draftMessage;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private String transactionId;
    private String flouciPaymentId;

    private boolean messageSent;
    private Long messageId;
}