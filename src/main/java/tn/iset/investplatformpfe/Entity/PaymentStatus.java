package tn.iset.investplatformpfe.Entity;

// PaymentStatus.java
public enum PaymentStatus {
    PENDING,                    // En attente de paiement (après approbation)
    PENDING_PARTNER_APPROVAL,   // En attente d'approbation du partenaire
    PARTNER_APPROVED,           // Approuvé par partenaire, en attente de paiement
    PARTNER_REJECTED,           // Rejeté par partenaire
    AWAITING_PAYMENT,           // En attente de paiement (après approbation)
    COMPLETED,                  // Payé et finalisé
    FAILED,                     // Paiement échoué
    CANCELLED,                  // Annulé
    EXPIRED                     //  NOUVEAU: Réservation expirée (non payé)
}