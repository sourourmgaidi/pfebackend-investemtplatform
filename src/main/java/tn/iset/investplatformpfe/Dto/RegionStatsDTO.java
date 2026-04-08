package tn.iset.investplatformpfe.Dto;
public class RegionStatsDTO {
    private Long id;
    private String name;
    private String code;
    private String geographicalZone;
    private String economicDescription;
    private String taxIncentives;
    private String infrastructure;
    private long approvedInvestmentServices;
    private long approvedCollaborationServices;
    private long approvedTouristServices;

    public long getTotalApprovedServices() {
        return approvedInvestmentServices + approvedCollaborationServices + approvedTouristServices;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getGeographicalZone() { return geographicalZone; }
    public void setGeographicalZone(String z) { this.geographicalZone = z; }
    public String getEconomicDescription() { return economicDescription; }
    public void setEconomicDescription(String d) { this.economicDescription = d; }
    public String getTaxIncentives() { return taxIncentives; }
    public void setTaxIncentives(String t) { this.taxIncentives = t; }
    public String getInfrastructure() { return infrastructure; }
    public void setInfrastructure(String i) { this.infrastructure = i; }
    public long getApprovedInvestmentServices() { return approvedInvestmentServices; }
    public void setApprovedInvestmentServices(long v) { this.approvedInvestmentServices = v; }
    public long getApprovedCollaborationServices() { return approvedCollaborationServices; }
    public void setApprovedCollaborationServices(long v) { this.approvedCollaborationServices = v; }
    public long getApprovedTouristServices() { return approvedTouristServices; }
    public void setApprovedTouristServices(long v) { this.approvedTouristServices = v; }
}
