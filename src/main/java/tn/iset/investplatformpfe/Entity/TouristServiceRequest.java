package tn.iset.investplatformpfe.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tourist_service_requests")
public class TouristServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tourist_service_id", nullable = false)
    private TouristService service;

    @ManyToOne
    @JoinColumn(name = "partner_id", nullable = false)
    private LocalPartner partner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType requestType; // EDIT ou DELETE

    @Column(length = 1000, nullable = false)
    private String reason;

    @Column(length = 2000)
    private String requestedChanges; // Pour les demandes EDIT

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceStatus status = ServiceStatus.PENDING;

    @Column(name = "request_date")
    private LocalDateTime requestDate;

    @Column(name = "response_date")
    private LocalDateTime responseDate;

    @Column(name = "execution_date")
    private LocalDateTime executionDate;
    // ===============================
// CHAMPS POUR LE REJET (PLUS COMPLETS)
// ===============================
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;  // Raison du rejet (plus spécifique que adminComment)

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;  // Date du rejet

    @Column(name = "rejected_by_admin_id")
    private Long rejectedByAdminId;  // ID de l'admin qui a rejeté

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Admin admin;

    @Column(name = "admin_comment", length = 1000)
    private String adminComment; // Commentaire de l'admin lors du rejet

    // Constructeurs
    public TouristServiceRequest() {
        this.requestDate = LocalDateTime.now();
    }

    public TouristServiceRequest(TouristService service, LocalPartner partner,
                                 RequestType requestType, String reason, String requestedChanges) {
        this.service = service;
        this.partner = partner;
        this.requestType = requestType;
        this.reason = reason;
        this.requestedChanges = requestedChanges;
        this.requestDate = LocalDateTime.now();
        this.status = ServiceStatus.PENDING;
    }

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TouristService getService() { return service; }
    public void setService(TouristService service) { this.service = service; }

    public LocalPartner getPartner() { return partner; }
    public void setPartner(LocalPartner partner) { this.partner = partner; }

    public RequestType getRequestType() { return requestType; }
    public void setRequestType(RequestType requestType) { this.requestType = requestType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getRequestedChanges() { return requestedChanges; }
    public void setRequestedChanges(String requestedChanges) { this.requestedChanges = requestedChanges; }

    public ServiceStatus getStatus() { return status; }
    public void setStatus(ServiceStatus status) { this.status = status; }

    public LocalDateTime getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDateTime requestDate) { this.requestDate = requestDate; }

    public LocalDateTime getResponseDate() { return responseDate; }
    public void setResponseDate(LocalDateTime responseDate) { this.responseDate = responseDate; }

    public LocalDateTime getExecutionDate() { return executionDate; }
    public void setExecutionDate(LocalDateTime executionDate) { this.executionDate = executionDate; }

    public Admin getAdmin() { return admin; }
    public void setAdmin(Admin admin) { this.admin = admin; }

    public String getAdminComment() { return adminComment; }
    public void setAdminComment(String adminComment) { this.adminComment = adminComment; }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public Long getRejectedByAdminId() {
        return rejectedByAdminId;
    }

    public void setRejectedByAdminId(Long rejectedByAdminId) {
        this.rejectedByAdminId = rejectedByAdminId;
    }
}