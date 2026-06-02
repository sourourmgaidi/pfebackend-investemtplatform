package tn.iset.investplatformpfe.Dto;

import tn.iset.investplatformpfe.Entity.Prospect;
import java.util.List;

public class ProspectReviewDto {
    public Long id;
    public String name;
    public String email;
    public String company;
    public String city;
    public String priority;
    public String generatedMessage;
    public List<String> suggestions;

    public ProspectReviewDto(Prospect p, List<String> suggestions) {
        this.id = p.getId();
        this.name = p.getName();
        this.email = p.getEmail();
        this.company = p.getCompany();
        this.city = p.getCity();
        this.priority = p.getPriority();
        this.generatedMessage = p.getGeneratedMessage();
        this.suggestions = suggestions;
    }
}