package tn.iset.investplatformpfe.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RecommendationResponseDTO {

    private Object  service;        // TouristService / InvestmentService / CollaborationService
    private int     score;
    private String  aiExplanation;

    // ⚠️ isAIScored : Jackson sérialise par défaut "AIScored" (sans "is"),
    //    on force le nom pour que le front le reçoive bien comme "isAIScored"
    @JsonProperty("isAIScored")
    private boolean isAIScored;

    public RecommendationResponseDTO() {}

    public RecommendationResponseDTO(Object service, int score, String aiExplanation, boolean isAIScored) {
        this.service       = service;
        this.score         = score;
        this.aiExplanation = aiExplanation;
        this.isAIScored    = isAIScored;
    }

    public Object  getService()       { return service; }
    public int     getScore()         { return score; }
    public String  getAiExplanation() { return aiExplanation; }

    @JsonProperty("isAIScored")
    public boolean isAIScored()       { return isAIScored; }

    public void setService(Object service)             { this.service = service; }
    public void setScore(int score)                    { this.score = score; }
    public void setAiExplanation(String aiExplanation) { this.aiExplanation = aiExplanation; }
    public void setAIScored(boolean isAIScored)        { this.isAIScored = isAIScored; }
}
