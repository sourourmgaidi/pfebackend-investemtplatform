package tn.iset.investplatformpfe.Dto;

public class StatsSummaryDTO {

    private long totalInvestors;
    private long totalEconomicPartners;
    private long totalInternationalCompanies;
    private long totalTourists;
    private long totalLocalPartners;

    private long approvedInvestmentServices;
    private long approvedCollaborationServices;
    private long approvedTouristServices;

    public long getTotalUsers() {
        return totalInvestors + totalEconomicPartners
                + totalInternationalCompanies + totalTourists + totalLocalPartners;
    }

    public long getTotalApprovedServices() {
        return approvedInvestmentServices + approvedCollaborationServices + approvedTouristServices;
    }

    public long getTotalInvestors() { return totalInvestors; }
    public void setTotalInvestors(long v) { this.totalInvestors = v; }

    public long getTotalEconomicPartners() { return totalEconomicPartners; }
    public void setTotalEconomicPartners(long v) { this.totalEconomicPartners = v; }

    public long getTotalInternationalCompanies() { return totalInternationalCompanies; }
    public void setTotalInternationalCompanies(long v) { this.totalInternationalCompanies = v; }

    public long getTotalTourists() { return totalTourists; }
    public void setTotalTourists(long v) { this.totalTourists = v; }

    public long getTotalLocalPartners() { return totalLocalPartners; }
    public void setTotalLocalPartners(long v) { this.totalLocalPartners = v; }

    public long getApprovedInvestmentServices() { return approvedInvestmentServices; }
    public void setApprovedInvestmentServices(long v) { this.approvedInvestmentServices = v; }

    public long getApprovedCollaborationServices() { return approvedCollaborationServices; }
    public void setApprovedCollaborationServices(long v) { this.approvedCollaborationServices = v; }

    public long getApprovedTouristServices() { return approvedTouristServices; }
    public void setApprovedTouristServices(long v) { this.approvedTouristServices = v; }
}
