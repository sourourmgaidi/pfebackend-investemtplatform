package tn.iset.investplatformpfe.Dto;

import java.util.List;

public class SuggestionResponse {
    private boolean valid;
    private String errorMessage;        // si invalide
    private List<String> suggestions;   // les 3 reformulations si valide

    // Constructeurs
    public static SuggestionResponse invalid(String error) {
        SuggestionResponse r = new SuggestionResponse();
        r.valid = false;
        r.errorMessage = error;
        return r;
    }

    public static SuggestionResponse valid(List<String> suggestions) {
        SuggestionResponse r = new SuggestionResponse();
        r.valid = true;
        r.suggestions = suggestions;
        return r;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }
}