package tn.iset.investplatformpfe.Dto;

public class ApprovalRequest {
    private Long prospectId;
    private String finalMessage; // null = garder le message généré original
    // NOUVEAU : pour l'envoi broadcast à tous les PENDING
    private String broadcastMessage; // le message unique adapté pour tous
    private boolean sendOwn;

    public Long getProspectId() { return prospectId; }
    public void setProspectId(Long prospectId) { this.prospectId = prospectId; }
    public String getFinalMessage() { return finalMessage; }
    public void setFinalMessage(String finalMessage) { this.finalMessage = finalMessage; }
    public String getBroadcastMessage() { return broadcastMessage; }
    public void setBroadcastMessage(String broadcastMessage) { this.broadcastMessage = broadcastMessage; }
    public boolean isSendOwn() { return sendOwn; }
    public void setSendOwn(boolean sendOwn) { this.sendOwn = sendOwn; }
}