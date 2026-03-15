package tn.iset.investplatformpfe.Entity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Table(name = "economic_partners")  // Nouveau nom de table en anglais
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class EconomicPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Last name is required")
    @Column(name = "last_name", nullable = false)  // Mapping avec la colonne
    private String lastName;  // ancien: nom

    @NotBlank(message = "First name is required")
    @Column(name = "first_name", nullable = false)  // Mapping avec la colonne
    private String firstName;  // ancien: prenom

    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(name = "password", nullable = false)  // Mapping avec la colonne
    private String password;  // ancien: motDePasse (hashé)

    @Column(name = "phone")
    private String phone;  // ancien: telephone

    @Column(name = "profile_photo")
    private String profilePhoto;  // ancien: photoProfil

    @Column(name = "registration_date")
    private LocalDateTime registrationDate;  // ancien: dateInscription

    @Column(name = "active")
    private Boolean active = true;  // ancien: actif

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.PARTNER;

    @Column(name = "country_of_origin")
    private String countryOfOrigin;  // ancien: paysOrigine
    @Enumerated(EnumType.STRING)
    @Column(name = "business_sector")
    private ActivityDomain businessSector;  // ancien: secteurActivite

    @Column(name = "headquarters_address")
    private String headquartersAddress;  // ancien: adresseSiege

    @Column(name = "linkedin_profile", nullable = false)  // NON NULL
    private String linkedinProfile;
    @Column(name = "website")
    private String website;
    @ManyToMany
    @JoinTable(
            name = "partner_favorite_collaborations",
            joinColumns = @JoinColumn(name = "partner_id"),
            inverseJoinColumns = @JoinColumn(name = "collaboration_service_id")
    )
    @JsonIgnoreProperties("favoritedByPartners")
    private List<CollaborationService> favoriteCollaborationServices = new ArrayList<>();

    public List<CollaborationService> getFavoriteCollaborationServices() {
        return favoriteCollaborationServices;
    }

    public void setFavoriteCollaborationServices(List<CollaborationService> favoriteCollaborationServices) {
        this.favoriteCollaborationServices = favoriteCollaborationServices;
    }

    // Getters et Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotBlank(message = "Last name is required") String getLastName() {
        return lastName;
    }

    public void setLastName(@NotBlank(message = "Last name is required") String lastName) {
        this.lastName = lastName;
    }

    public @NotBlank(message = "First name is required") String getFirstName() {
        return firstName;
    }

    public void setFirstName(@NotBlank(message = "First name is required") String firstName) {
        this.firstName = firstName;
    }

    public @Email(message = "Email must be valid") @NotBlank(message = "Email is required") String getEmail() {
        return email;
    }

    public void setEmail(@Email(message = "Email must be valid") @NotBlank(message = "Email is required") String email) {
        this.email = email;
    }

    public @NotBlank(message = "Password is required") String getPassword() {
        return password;
    }

    public void setPassword(@NotBlank(message = "Password is required") String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
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

    public String getCountryOfOrigin() {
        return countryOfOrigin;
    }

    public void setCountryOfOrigin(String countryOfOrigin) {
        this.countryOfOrigin = countryOfOrigin;
    }


    public String getHeadquartersAddress() {
        return headquartersAddress;
    }

    public void setHeadquartersAddress(String headquartersAddress) {
        this.headquartersAddress = headquartersAddress;
    }



    public ActivityDomain getBusinessSector() {
        return businessSector;
    }

    public void setBusinessSector(ActivityDomain businessSector) {
        this.businessSector = businessSector;
    }

    public String getLinkedinProfile() {
        return linkedinProfile;
    }

    public void setLinkedinProfile(String linkedinProfile) {
        this.linkedinProfile = linkedinProfile;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }
}