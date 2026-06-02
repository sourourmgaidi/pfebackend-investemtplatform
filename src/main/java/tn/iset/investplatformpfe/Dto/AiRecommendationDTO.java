package tn.iset.investplatformpfe.Dto;

import java.math.BigDecimal;
import java.util.List;

public class AiRecommendationDTO {

    // ── Résumé d'un service (Investment, Collaboration ou Tourist) ────────
    public static class ServiceSummary {
        private Long id;
        private String serviceType;        // "INVESTMENT", "COLLABORATION" ou "TOURIST"
        private String name;
        private String description;
        private String activityDomain;
        private String region;
        private BigDecimal budget;
        private String availability;

        // Champs Investment / Collaboration
        private String collaborationType;
        private List<String> requiredSkills;
        private String expectedBenefits;

        // ── Champs spécifiques TOURIST ──────────────────────────────
        private String category;           // HOTEL, RESTAURANT, GUIDE, TRANSPORT...
        private String targetAudience;
        private Integer durationHours;
        private Integer maxCapacity;
        private List<String> availableLanguages;
        private List<String> includedServices;

        // Getters / Setters communs
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getServiceType() { return serviceType; }
        public void setServiceType(String serviceType) { this.serviceType = serviceType; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getActivityDomain() { return activityDomain; }
        public void setActivityDomain(String activityDomain) { this.activityDomain = activityDomain; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public BigDecimal getBudget() { return budget; }
        public void setBudget(BigDecimal budget) { this.budget = budget; }
        public String getAvailability() { return availability; }
        public void setAvailability(String availability) { this.availability = availability; }
        public String getCollaborationType() { return collaborationType; }
        public void setCollaborationType(String collaborationType) { this.collaborationType = collaborationType; }
        public List<String> getRequiredSkills() { return requiredSkills; }
        public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }
        public String getExpectedBenefits() { return expectedBenefits; }
        public void setExpectedBenefits(String expectedBenefits) { this.expectedBenefits = expectedBenefits; }

        // Getters / Setters Tourist
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getTargetAudience() { return targetAudience; }
        public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }
        public Integer getDurationHours() { return durationHours; }
        public void setDurationHours(Integer durationHours) { this.durationHours = durationHours; }
        public Integer getMaxCapacity() { return maxCapacity; }
        public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }
        public List<String> getAvailableLanguages() { return availableLanguages; }
        public void setAvailableLanguages(List<String> availableLanguages) { this.availableLanguages = availableLanguages; }
        public List<String> getIncludedServices() { return includedServices; }
        public void setIncludedServices(List<String> includedServices) { this.includedServices = includedServices; }
    }

    // ── Réponse renvoyée au frontend ─────────────────────────────
    public static class AiRecommendationResponse {
        private List<ScoredService> rankedServices;
        private String globalExplanation;

        public List<ScoredService> getRankedServices() { return rankedServices; }
        public void setRankedServices(List<ScoredService> rankedServices) { this.rankedServices = rankedServices; }
        public String getGlobalExplanation() { return globalExplanation; }
        public void setGlobalExplanation(String globalExplanation) { this.globalExplanation = globalExplanation; }
    }

    // ── Un service avec son score IA ─────────────────────────────
    public static class ScoredService {
        private Long serviceId;
        private String serviceType;   // "INVESTMENT", "COLLABORATION" ou "TOURIST"
        private int score;            // 0 à 100
        private String reason;

        public Long getServiceId() { return serviceId; }
        public void setServiceId(Long serviceId) { this.serviceId = serviceId; }
        public String getServiceType() { return serviceType; }
        public void setServiceType(String serviceType) { this.serviceType = serviceType; }
        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }
}