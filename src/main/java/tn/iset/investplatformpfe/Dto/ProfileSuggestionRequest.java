package tn.iset.investplatformpfe.Dto;


public class ProfileSuggestionRequest {
    private String question;

    public ProfileSuggestionRequest() {}
    public ProfileSuggestionRequest(String question) { this.question = question; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
}
