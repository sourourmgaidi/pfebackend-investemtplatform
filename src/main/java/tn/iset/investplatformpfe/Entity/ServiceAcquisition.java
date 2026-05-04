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



    @Column(name = "acquired_at")
    private LocalDateTime acquiredAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    private LocalDateTime approvedAt;   // date où le partner a approuvé
    private boolean reminderSent = false;


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

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public boolean isReminderSent() {
        return reminderSent;
    }

    public void setReminderSent(boolean reminderSent) {
        this.reminderSent = reminderSent;
    }
}