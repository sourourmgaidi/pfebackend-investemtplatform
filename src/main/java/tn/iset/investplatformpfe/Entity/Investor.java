package tn.iset.investplatformpfe.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Table(name = "investor")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Investor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "firstName", nullable = false)
    private String firstName;

    @Column(name = "lastName", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "phone", nullable = false)
    private String phone;

    @Column(name = "profile_picture", nullable = true)
    private String profilePicture;

    @Column(name = "registration_date", nullable = false)
    private LocalDateTime registrationDate;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role = Role.INVESTOR;

    @Column(name = "company", nullable = false)
    private String company;

    @Column(name = "origin_country", nullable = false)
    private String originCountry;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_sector", nullable = false)
    private ActivityDomain activitySector;

    @Column(name = "website", nullable = false)
    private String website;

    @Column(name = "linkedin_profile", nullable = false)
    private String linkedinProfile;

    @Column(name = "nationality")
    private String nationality;

    // Relation existante : services dans lesquels l'investisseur a marqué son intérêt
    @ManyToMany(mappedBy = "interestedInvestors")
    @JsonIgnoreProperties("interestedInvestors")
    private List<InvestmentService> interestedInvestmentServices;

    // ✅ NOUVELLE RELATION : Services favoris (à consulter plus tard)
    @ManyToMany
    @JoinTable(
            name = "investor_favorite_services",
            joinColumns = @JoinColumn(name = "investor_id"),
            inverseJoinColumns = @JoinColumn(name = "investment_service_id")
    )
    @JsonIgnoreProperties("favoritedByInvestors")
    private List<InvestmentService> favoriteServices = new ArrayList<>();

    // Getters et Setters existants...

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = (profilePicture != null && profilePicture.trim().isEmpty()) ? null : profilePicture;
    }

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getOriginCountry() {
        return originCountry;
    }

    public void setOriginCountry(String originCountry) {
        this.originCountry = originCountry;
    }

    public ActivityDomain getActivitySector() {
        return activitySector;
    }

    public void setActivitySector(ActivityDomain activitySector) {
        this.activitySector = activitySector;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getLinkedinProfile() {
        return linkedinProfile;
    }

    public void setLinkedinProfile(String linkedinProfile) {
        this.linkedinProfile = linkedinProfile;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public List<InvestmentService> getInterestedInvestmentServices() {
        return interestedInvestmentServices;
    }

    public void setInterestedInvestmentServices(List<InvestmentService> interestedInvestmentServices) {
        this.interestedInvestmentServices = interestedInvestmentServices;
    }

    // ✅ NOUVEAUX GETTERS ET SETTERS pour favoriteServices
    public List<InvestmentService> getFavoriteServices() {
        return favoriteServices;
    }

    public void setFavoriteServices(List<InvestmentService> favoriteServices) {
        this.favoriteServices = favoriteServices;
    }
}