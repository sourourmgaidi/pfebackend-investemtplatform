package tn.iset.investplatformpfe.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "regions")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Region {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;  // Tunis, Sousse, Djerba...

    @Column(length = 1000)
    private String economicDescription;

    @ManyToMany
    @JoinTable(
            name = "region_economic_sectors",
            joinColumns = @JoinColumn(name = "region_id"),
            inverseJoinColumns = @JoinColumn(name = "sector_id")
    )
    @JsonIgnore
    private List<EconomicSector> potentialSectors;

    @Column(length = 1000)
    private String taxIncentives;

    @Column(length = 1000)
    private String infrastructure;

    @ManyToMany
    @JoinTable(
            name = "region_business_opportunities",
            joinColumns = @JoinColumn(name = "region_id"),
            inverseJoinColumns = @JoinColumn(name = "opportunity_id")
    )
    @JsonIgnore
    private List<BusinessOpportunity> businessOpportunities;

    @OneToMany(mappedBy = "region")
    @JsonIgnore
    private List<CollaborationService> collaborations;

    @OneToMany(mappedBy = "region")
    @JsonIgnore
    private List<LocalPartner> localPartners;  // Changé de PartenaireLocal à LocalPartner

    @ManyToMany
    @JoinTable(
            name = "region_local_products",
            joinColumns = @JoinColumn(name = "region_id"),
            inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    @JsonIgnore
    private List<LocalProduct> localProducts;

    @OneToMany(mappedBy = "region")
    @JsonIgnore
    private List<InvestmentService> investmentServices;

    @OneToMany(mappedBy = "region", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<RegionEconomicSector> regionEconomicSectors = new ArrayList<>();
    // Optional fields from the enum
    private String code;  // TUNIS, ARIANA, BEN_AROUS, etc.

    private String geographicalZone;  // Grand Tunis, Nord-Est, etc.

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEconomicDescription() {
        return economicDescription;
    }

    public void setEconomicDescription(String economicDescription) {
        this.economicDescription = economicDescription;
    }

    public List<EconomicSector> getPotentialSectors() {
        return potentialSectors;
    }

    public void setPotentialSectors(List<EconomicSector> potentialSectors) {
        this.potentialSectors = potentialSectors;
    }

    public String getTaxIncentives() {
        return taxIncentives;
    }

    public void setTaxIncentives(String taxIncentives) {
        this.taxIncentives = taxIncentives;
    }

    public String getInfrastructure() {
        return infrastructure;
    }

    public void setInfrastructure(String infrastructure) {
        this.infrastructure = infrastructure;
    }

    public List<BusinessOpportunity> getBusinessOpportunities() {
        return businessOpportunities;
    }

    public void setBusinessOpportunities(List<BusinessOpportunity> businessOpportunities) {
        this.businessOpportunities = businessOpportunities;
    }

    public List<CollaborationService> getCollaborations() {
        return collaborations;
    }

    public void setCollaborations(List<CollaborationService> collaborations) {
        this.collaborations = collaborations;
    }

    public List<LocalPartner> getLocalPartners() {  // Type de retour changé
        return localPartners;
    }

    public void setLocalPartners(List<LocalPartner> localPartners) {  // Type de paramètre changé
        this.localPartners = localPartners;
    }

    public List<LocalProduct> getLocalProducts() {
        return localProducts;
    }

    public void setLocalProducts(List<LocalProduct> localProducts) {
        this.localProducts = localProducts;
    }

    public List<InvestmentService> getInvestmentServices() {
        return investmentServices;
    }

    public void setInvestmentServices(List<InvestmentService> investmentServices) {
        this.investmentServices = investmentServices;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getGeographicalZone() {
        return geographicalZone;
    }

    public void setGeographicalZone(String geographicalZone) {
        this.geographicalZone = geographicalZone;
    }
    public List<RegionEconomicSector> getRegionEconomicSectors() {
        return regionEconomicSectors;
    }

    public void setRegionEconomicSectors(List<RegionEconomicSector> regionEconomicSectors) {
        this.regionEconomicSectors = regionEconomicSectors;
    }
}