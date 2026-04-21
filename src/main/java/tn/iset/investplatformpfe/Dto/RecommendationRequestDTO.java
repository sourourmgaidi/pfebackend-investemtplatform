package tn.iset.investplatformpfe.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import tn.iset.investplatformpfe.Entity.ActivityDomain;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Entity.TargetAudience;
import java.math.BigDecimal;
import java.util.List;

public class RecommendationRequestDTO {

    // ── userType reçu comme String depuis Angular → converti en Role ────────
    private Role         userType;
    private Long         regionId;
    private ActivityDomain activityDomain;
    private BigDecimal   budget;
    private String       availability;

    // Touriste
    private Integer      groupSize;
    private List<String> preferredLanguages;
    private String  targetAudienceRaw;

    // Investisseur
    private String       investmentHorizon;
    private String       preferredSector;
    private BigDecimal   minimumReturn;
    private String       riskLevel;
    private String       projectDescription;
    private String       specificRequirements;

    // Partenaire / Collaboration
    private String       collaborationGoal;
    private List<String> offeredSkills;
    private String       collaborationType;
    private String       partnershipDuration;
    private String       partnerCriteria;

    // Société Internationale
    private String       serviceTypeFilter;
    private String       companyPresentation;
    private String       originCountry;
    private String       companySize;
    private String       strategicGoal;
    private String       legalConstraints;

    // ════════════════════════════════════════════════════════════════════════
    //  DÉSÉRIALISATION ROBUSTE — String → Enum (vient du front Angular)
    // ════════════════════════════════════════════════════════════════════════

    @JsonSetter("userType")
    public void setUserTypeFromString(String value) {
        if (value == null || value.isBlank()) { this.userType = null; return; }
        try {
            this.userType = Role.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            this.userType = null;
        }
    }

    @JsonSetter("activityDomain")
    public void setActivityDomainFromString(String value) {
        if (value == null || value.isBlank()) { this.activityDomain = null; return; }
        try {
            this.activityDomain = ActivityDomain.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            this.activityDomain = null;
        }
    }


    @JsonSetter("targetAudience")
    public void setTargetAudienceFromString(String value) {
        if (value == null || value.isBlank()) { this.targetAudienceRaw = null; return; }
        this.targetAudienceRaw = value.trim().toUpperCase();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  GETTERS & SETTERS
    // ════════════════════════════════════════════════════════════════════════

    public Role getUserType() { return userType; }
    public void setUserType(Role userType) { this.userType = userType; }

    public Long getRegionId() { return regionId; }
    public void setRegionId(Long regionId) { this.regionId = regionId; }

    public ActivityDomain getActivityDomain() { return activityDomain; }
    public void setActivityDomain(ActivityDomain d) { this.activityDomain = d; }

    public BigDecimal getBudget() { return budget; }
    public void setBudget(BigDecimal budget) { this.budget = budget; }

    public String getAvailability() { return availability; }
    public void setAvailability(String availability) { this.availability = availability; }

    public Integer getGroupSize() { return groupSize; }
    public void setGroupSize(Integer groupSize) { this.groupSize = groupSize; }

    public List<String> getPreferredLanguages() { return preferredLanguages; }
    public void setPreferredLanguages(List<String> l) { this.preferredLanguages = l; }

    public TargetAudience getTargetAudience() {
        if (targetAudienceRaw == null) return null;
        try {
            return TargetAudience.valueOf(targetAudienceRaw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Garder ce setter pour compatibilité si besoin ailleurs
    public void setTargetAudience(TargetAudience t) {
        this.targetAudienceRaw = t != null ? t.name() : null;
    }

    public String getInvestmentHorizon() { return investmentHorizon; }
    public void setInvestmentHorizon(String s) { this.investmentHorizon = s; }

    public String getPreferredSector() { return preferredSector; }
    public void setPreferredSector(String s) { this.preferredSector = s; }

    public BigDecimal getMinimumReturn() { return minimumReturn; }
    public void setMinimumReturn(BigDecimal b) { this.minimumReturn = b; }

    public String getRiskLevel() { return riskLevel; }
    public void setRiskLevel(String s) { this.riskLevel = s; }

    public String getProjectDescription() { return projectDescription; }
    public void setProjectDescription(String s) { this.projectDescription = s; }

    public String getSpecificRequirements() { return specificRequirements; }
    public void setSpecificRequirements(String s) { this.specificRequirements = s; }

    public String getCollaborationGoal() { return collaborationGoal; }
    public void setCollaborationGoal(String s) { this.collaborationGoal = s; }

    public List<String> getOfferedSkills() { return offeredSkills; }
    public void setOfferedSkills(List<String> l) { this.offeredSkills = l; }

    public String getCollaborationType() { return collaborationType; }
    public void setCollaborationType(String s) { this.collaborationType = s; }

    public String getPartnershipDuration() { return partnershipDuration; }
    public void setPartnershipDuration(String s) { this.partnershipDuration = s; }

    public String getPartnerCriteria() { return partnerCriteria; }
    public void setPartnerCriteria(String s) { this.partnerCriteria = s; }

    public String getServiceTypeFilter() { return serviceTypeFilter; }
    public void setServiceTypeFilter(String s) { this.serviceTypeFilter = s; }

    public String getCompanyPresentation() { return companyPresentation; }
    public void setCompanyPresentation(String s) { this.companyPresentation = s; }

    public String getOriginCountry() { return originCountry; }
    public void setOriginCountry(String s) { this.originCountry = s; }

    public String getCompanySize() { return companySize; }
    public void setCompanySize(String s) { this.companySize = s; }

    public String getStrategicGoal() { return strategicGoal; }
    public void setStrategicGoal(String s) { this.strategicGoal = s; }

    public String getLegalConstraints() { return legalConstraints; }
    public void setLegalConstraints(String s) { this.legalConstraints = s; }
}
