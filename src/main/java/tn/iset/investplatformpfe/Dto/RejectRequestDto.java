package tn.iset.investplatformpfe.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RejectRequestDto {

    @NotBlank(message = "La raison du rejet est obligatoire")
    @Size(min = 5, max = 1000, message = "La raison doit contenir entre 5 et 1000 caractères")
    private String rejectionReason;

    // Constructeur par défaut (obligatoire pour la désérialisation)
    public RejectRequestDto() {
    }

    // Getter
    public String getRejectionReason() {
        return rejectionReason;
    }

    // Setter
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
