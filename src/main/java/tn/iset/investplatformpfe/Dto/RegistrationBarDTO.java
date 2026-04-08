package tn.iset.investplatformpfe.Dto;



public class RegistrationBarDTO {

    private String dayLabel;   // "7", "11", "15", "19", "23", "27", "31"
    private String fullDate;   // "2025-03-07"
    private int count;         // nombre d'inscrits ce jour

    public RegistrationBarDTO() {}

    public RegistrationBarDTO(String dayLabel, String fullDate, int count) {
        this.dayLabel = dayLabel;
        this.fullDate = fullDate;
        this.count = count;
    }

    public String getDayLabel() { return dayLabel; }
    public void setDayLabel(String dayLabel) { this.dayLabel = dayLabel; }

    public String getFullDate() { return fullDate; }
    public void setFullDate(String fullDate) { this.fullDate = fullDate; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
