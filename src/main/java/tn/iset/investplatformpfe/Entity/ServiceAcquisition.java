package tn.iset.investplatformpfe.Entity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_acquisitions")
public class ServiceAcquisition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String serviceType; // INVESTMENT ou COLLABORATION

    @Column(nullable = false)
    private Long serviceId;

    @Column(nullable = false)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role acquirerRole; // INVESTOR, PARTNER, INTERNATIONAL_COMPANY

    @Column(nullable = false)
    private Long acquirerId;

    @Column(nullable = false)
    private String acquirerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "flouci_payment_id")
    private String flouciPaymentId;

    @Column(name = "payment_url", length = 1000)
    private String paymentUrl;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "partner_id")
    private Long partnerId;

    @Column(name = "reservation_expires_at")
    private LocalDateTime reservationExpiresAt;  // Date d'expiration de la réservation

    // ✅ CHANGER boolean → Boolean
    @Column(name = "reminder_sent")
    private Boolean reminderSent = false;  // Rappel déjà envoyé ?

    @Column(name = "acquired_at")
    private LocalDateTime acquiredAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @PrePersist
    public void prePersist() {
        this.acquiredAt = LocalDateTime.now();
        if (this.reminderSent == null) {
            this.reminderSent = false;
        }
    }

    // Getters et Setters
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String r) { this.rejectionReason = r; }

    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long id) { this.partnerId = id; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getServiceType() { return serviceType; }
    public void setServiceType(String t) { this.serviceType = t; }

    public Long getServiceId() { return serviceId; }
    public void setServiceId(Long id) { this.serviceId = id; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String n) { this.serviceName = n; }

    public Role getAcquirerRole() { return acquirerRole; }
    public void setAcquirerRole(Role r) { this.acquirerRole = r; }

    public Long getAcquirerId() { return acquirerId; }
    public void setAcquirerId(Long id) { this.acquirerId = id; }

    public String getAcquirerEmail() { return acquirerEmail; }
    public void setAcquirerEmail(String e) { this.acquirerEmail = e; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus s) { this.paymentStatus = s; }

    public String getFlouciPaymentId() { return flouciPaymentId; }
    public void setFlouciPaymentId(String id) { this.flouciPaymentId = id; }

    public String getPaymentUrl() { return paymentUrl; }
    public void setPaymentUrl(String url) { this.paymentUrl = url; }

    public Double getAmount() { return amount; }
    public void setAmount(Double a) { this.amount = a; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String o) { this.orderId = o; }

    public LocalDateTime getAcquiredAt() { return acquiredAt; }
    public void setAcquiredAt(LocalDateTime t) { this.acquiredAt = t; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime t) { this.paidAt = t; }

    public LocalDateTime getReservationExpiresAt() {
        return reservationExpiresAt;
    }

    public void setReservationExpiresAt(LocalDateTime reservationExpiresAt) {
        this.reservationExpiresAt = reservationExpiresAt;
    }

    // ✅ Modifier les getters/setters pour Boolean
    public Boolean getReminderSent() {
        return reminderSent != null ? reminderSent : false;
    }

    public void setReminderSent(Boolean reminderSent) {
        this.reminderSent = reminderSent;
    }
}