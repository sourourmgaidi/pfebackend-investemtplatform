package tn.iset.investplatformpfe.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_requests")
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private InvestmentService service;

    @ManyToOne
    @JoinColumn(name = "partner_id")
    private LocalPartner partner;

    @Enumerated(EnumType.STRING)
    private RequestType requestType;     // EDIT ou DELETE

    private String reason;                // Raison de la demande

    @Column(length = 2000)
    private String requestedChanges;      // Changements demandés (pour EDIT)

    @Enumerated(EnumType.STRING)
    private ServiceStatus status;         // PENDING, APPROVED, REJECTED

    private LocalDateTime requestDate;
    private LocalDateTime responseDate;
    private LocalDateTime executionDate;

    @ManyToOne
    @JoinColumn(name = "admin_id")
    private Admin admin;

    // ===============================
    // CHAMPS POUR LE REJET (NOUVEAU)
    // ===============================
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejected_by_admin_id")
    private Long rejectedByAdminId;

    // Constructeurs
    public ServiceRequest() {}

    public ServiceRequest(InvestmentService service, LocalPartner partner,
                          RequestType requestType, String reason, String requestedChanges) {
        this.service = service;
        this.partner = partner;
        this.requestType = requestType;
        this.reason = reason;
        this.requestedChanges = requestedChanges;
        this.status = ServiceStatus.PENDING;
        this.requestDate = LocalDateTime.now();
    }

    // Getters et Setters existants
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public InvestmentService getService() { return service; }
    public void setService(InvestmentService service) { this.service = service; }

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

    // ===============================
    // NOUVEAUX GETTERS ET SETTERS
    // ===============================
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