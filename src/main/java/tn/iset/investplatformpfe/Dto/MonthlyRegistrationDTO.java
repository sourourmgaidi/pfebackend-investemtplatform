package tn.iset.investplatformpfe.Dto;
public class MonthlyRegistrationDTO {
    private String monthLabel;      // "Janvier", "Février", etc.
    private String month;           // "2025-03"
    private int count;              // nombre total d'inscrits
    private int previousMonthCount; // nombre du mois précédent
    private int percentageChange;   // pourcentage de changement

    public MonthlyRegistrationDTO() {}

    public MonthlyRegistrationDTO(String monthLabel, String month, int count) {
        this.monthLabel = monthLabel;
        this.month = month;
        this.count = count;
    }

    // Getters
    public String getMonthLabel() {
        return monthLabel;
    }

    public String getMonth() {
        return month;
    }

    public int getCount() {
        return count;
    }

    public int getPreviousMonthCount() {
        return previousMonthCount;
    }

    public int getPercentageChange() {
        return percentageChange;
    }

    // Setters
    public void setMonthLabel(String monthLabel) {
        this.monthLabel = monthLabel;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setPreviousMonthCount(int previousMonthCount) {
        this.previousMonthCount = previousMonthCount;
    }

    public void setPercentageChange(int percentageChange) {
        this.percentageChange = percentageChange;
    }
}