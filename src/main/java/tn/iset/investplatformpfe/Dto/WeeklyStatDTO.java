package tn.iset.investplatformpfe.Dto;


public class WeeklyStatDTO {
    private int weekNumber;
    private int year;
    private String weekLabel;
    private long totalSeconds;
    private String formattedTime;

    public WeeklyStatDTO() {}

    public WeeklyStatDTO(int weekNumber, int year, long totalSeconds) {
        this.weekNumber = weekNumber;
        this.year = year;
        this.weekLabel = "Semaine " + weekNumber + " (" + year + ")";
        this.totalSeconds = totalSeconds;
        this.formattedTime = formatSeconds(totalSeconds);
    }

    private String formatSeconds(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "min";
        } else if (minutes > 0 && remainingSeconds > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " " + remainingSeconds + " seconde" + (remainingSeconds > 1 ? "s" : "");
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        } else {
            return seconds + " seconde" + (seconds > 1 ? "s" : "");
        }
    }

    // Getters et Setters
    public int getWeekNumber() { return weekNumber; }
    public void setWeekNumber(int weekNumber) { this.weekNumber = weekNumber; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public String getWeekLabel() { return weekLabel; }
    public void setWeekLabel(String weekLabel) { this.weekLabel = weekLabel; }

    public long getTotalSeconds() { return totalSeconds; }
    public void setTotalSeconds(long totalSeconds) { this.totalSeconds = totalSeconds; }

    public String getFormattedTime() { return formattedTime; }
    public void setFormattedTime(String formattedTime) { this.formattedTime = formattedTime; }
}
