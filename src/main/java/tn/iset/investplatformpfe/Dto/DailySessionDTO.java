package tn.iset.investplatformpfe.Dto;

import java.time.LocalDate;
import java.util.List;

public class DailySessionDTO {
    private LocalDate date;
    private String dayLabel;
    private long totalSeconds;
    private String formattedTime;
    private List<SessionDetailDTO> sessions;

    public DailySessionDTO() {}

    // Getters et Setters
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getDayLabel() { return dayLabel; }
    public void setDayLabel(String dayLabel) { this.dayLabel = dayLabel; }

    public long getTotalSeconds() { return totalSeconds; }
    public void setTotalSeconds(long totalSeconds) { this.totalSeconds = totalSeconds; }

    public String getFormattedTime() { return formattedTime; }
    public void setFormattedTime(String formattedTime) { this.formattedTime = formattedTime; }

    public List<SessionDetailDTO> getSessions() { return sessions; }
    public void setSessions(List<SessionDetailDTO> sessions) { this.sessions = sessions; }
}
