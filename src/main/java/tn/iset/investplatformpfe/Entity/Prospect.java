package tn.iset.investplatformpfe.Entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class Prospect {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String category;
    private String company;
    private String city;
    private String interestLevel;

    private int score;
    private String priority; // HIGH, MEDIUM, LOW

    private String status; // PENDING, SENT, FAILED

    @Column(columnDefinition = "TEXT")
    private String generatedMessage;

    @Column(columnDefinition = "TEXT")
    private String generatedSuggestions;

    private LocalDateTime sentAt;


    public String getGeneratedSuggestions() { return generatedSuggestions; }
    public void setGeneratedSuggestions(String s) { this.generatedSuggestions = s; }


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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getInterestLevel() {
        return interestLevel;
    }

    public void setInterestLevel(String interestLevel) {
        this.interestLevel = interestLevel;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGeneratedMessage() {
        return generatedMessage;
    }

    public void setGeneratedMessage(String generatedMessage) {
        this.generatedMessage = generatedMessage;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}