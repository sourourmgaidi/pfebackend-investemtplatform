package tn.iset.investplatformpfe.Dto;


import java.time.LocalDateTime;

public class SessionDetailDTO {
    private Long sessionId;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private long durationSeconds;
    private String formattedDuration;

    public SessionDetailDTO() {}

    // Getters et Setters
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }

    public LocalDateTime getLogoutTime() { return logoutTime; }
    public void setLogoutTime(LocalDateTime logoutTime) { this.logoutTime = logoutTime; }

    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getFormattedDuration() { return formattedDuration; }
    public void setFormattedDuration(String formattedDuration) { this.formattedDuration = formattedDuration; }
}
