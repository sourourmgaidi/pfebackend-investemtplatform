package tn.iset.investplatformpfe.Dto;

import java.util.List;
import java.util.Map;

public class UserTimeStatsDTO {
    private String userEmail;
    private String userRole;
    private String userFullName;
    private String profilePhoto;
    private Map<String, Long> dailySeconds;      // Jour -> secondes
    private List<WeeklyStatDTO> weeklyStats;     // Statistiques par semaine
    private long totalSecondsThisWeek;
    private long totalSecondsLastWeek;
    private long differenceSeconds;
    private String formattedThisWeek;
    private String formattedLastWeek;
    private String formattedDifference;
    private String notificationMessage;
    private List<DailySessionDTO> dailySessions; // Sessions détaillées par jour

    // Constructeurs
    public UserTimeStatsDTO() {}

    // Getters et Setters
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }

    public String getProfilePhoto() { return profilePhoto; }
    public void setProfilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; }

    public Map<String, Long> getDailySeconds() { return dailySeconds; }
    public void setDailySeconds(Map<String, Long> dailySeconds) { this.dailySeconds = dailySeconds; }

    public List<WeeklyStatDTO> getWeeklyStats() { return weeklyStats; }
    public void setWeeklyStats(List<WeeklyStatDTO> weeklyStats) { this.weeklyStats = weeklyStats; }

    public long getTotalSecondsThisWeek() { return totalSecondsThisWeek; }
    public void setTotalSecondsThisWeek(long totalSecondsThisWeek) { this.totalSecondsThisWeek = totalSecondsThisWeek; }

    public long getTotalSecondsLastWeek() { return totalSecondsLastWeek; }
    public void setTotalSecondsLastWeek(long totalSecondsLastWeek) { this.totalSecondsLastWeek = totalSecondsLastWeek; }

    public long getDifferenceSeconds() { return differenceSeconds; }
    public void setDifferenceSeconds(long differenceSeconds) { this.differenceSeconds = differenceSeconds; }

    public String getFormattedThisWeek() { return formattedThisWeek; }
    public void setFormattedThisWeek(String formattedThisWeek) { this.formattedThisWeek = formattedThisWeek; }

    public String getFormattedLastWeek() { return formattedLastWeek; }
    public void setFormattedLastWeek(String formattedLastWeek) { this.formattedLastWeek = formattedLastWeek; }

    public String getFormattedDifference() { return formattedDifference; }
    public void setFormattedDifference(String formattedDifference) { this.formattedDifference = formattedDifference; }

    public String getNotificationMessage() { return notificationMessage; }
    public void setNotificationMessage(String notificationMessage) { this.notificationMessage = notificationMessage; }

    public List<DailySessionDTO> getDailySessions() { return dailySessions; }
    public void setDailySessions(List<DailySessionDTO> dailySessions) { this.dailySessions = dailySessions; }

    // Méthodes utilitaires
    public String getFormattedThisWeekHours() {
        return formatSecondsToHoursMinutes(totalSecondsThisWeek);
    }

    public String getFormattedLastWeekHours() {
        return formatSecondsToHoursMinutes(totalSecondsLastWeek);
    }

    public String getFormattedDifferenceHours() {
        return formatSecondsToHoursMinutes(Math.abs(differenceSeconds));
    }

    private String formatSecondsToHoursMinutes(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0) {
            return hours + "h " + minutes + "min";
        }
        return minutes + " minutes";
    }
}
