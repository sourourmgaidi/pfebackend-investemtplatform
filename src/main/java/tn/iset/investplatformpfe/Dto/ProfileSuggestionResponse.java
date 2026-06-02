package tn.iset.investplatformpfe.Dto;

import java.util.List;
import java.util.Map;

public class ProfileSuggestionResponse {
    private String answer;
    private String role;
    private String userIdentifier;
    private List<Map<String, Object>> recommendedServices;

    public ProfileSuggestionResponse() {}

    public ProfileSuggestionResponse(String answer, String role,
                                     String userIdentifier,
                                     List<Map<String, Object>> recommendedServices) {
        this.answer = answer;
        this.role = role;
        this.userIdentifier = userIdentifier;
        this.recommendedServices = recommendedServices;
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getUserIdentifier() { return userIdentifier; }
    public void setUserIdentifier(String userIdentifier) { this.userIdentifier = userIdentifier; }
    public List<Map<String, Object>> getRecommendedServices() { return recommendedServices; }
    public void setRecommendedServices(List<Map<String, Object>> recommendedServices) {
        this.recommendedServices = recommendedServices;
    }
}
