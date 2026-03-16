package tn.iset.investplatformpfe.Dto;

public class RejectServiceRequest {
    private String rejectionReason;

    // Constructeur par défaut (obligatoire pour la désérialisation JSON)
    public RejectServiceRequest() {}

    // Getter et setter
    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
