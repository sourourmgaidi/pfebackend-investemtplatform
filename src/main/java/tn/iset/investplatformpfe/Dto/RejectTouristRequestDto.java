package tn.iset.investplatformpfe.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RejectTouristRequestDto {
    @NotBlank(message = "Rejection reason is required")
    @Size(min = 5, max = 1000, message = "Reason must be between 5 and 1000 characters")
    private String rejectionReason;

    // Getters et setters
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
}
