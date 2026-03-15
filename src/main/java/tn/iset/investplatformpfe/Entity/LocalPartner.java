package tn.iset.investplatformpfe.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Table(name = "local_partner")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class LocalPartner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Last name is required")
    @Column(name = "nom", nullable = false)  // CORRIGÉ: "nom" au lieu de "last_name"
    private String lastName;

    @NotBlank(message = "First name is required")
    @Column(name = "prenom", nullable = false)  // CORRIGÉ: "prenom" au lieu de "first_name"
    private String firstName;

    @Email(message = "Email must be valid")
    @NotBlank(message = "Email is required")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(name = "mot_de_passe", nullable = false)
    private String password;

    @Column(name = "telephone")  // CORRIGÉ: "telephone" au lieu de "phone"
    private String phone;

    @Column(name = "photo_profil")
    private String profilePhoto;

    @Column(name = "date_inscription")
    private LocalDateTime registrationDate;

    @Column(name = "actif")  // CORRIGÉ: "actif" au lieu de "active"
    private Boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 50)
    private Role role = Role.LOCAL_PARTNER;

    @Column(name = "site_web")
    private String website;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "statut")
    private String status;
    @Enumerated(EnumType.STRING)
    @Column(name = "domaine_activite")
    private ActivityDomain activityDomain;

    @Column(name = "numero_registre_commerce")
    private String businessRegistrationNumber;

    @Column(name = "taxe_professionnelle")
    private String professionalTaxNumber;
    @Column(name = "linkedin_profile", nullable = false)  // NON NULL
    private String linkedinProfile;

    @Column(name = "adresse", columnDefinition = "TEXT")
    private String address;

    // OneToMany with TouristService
    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("provider")
    private List<TouristService> touristServices;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("provider")
    private List<CollaborationService> collaborationServices;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("provider")
    private List<InvestmentService> investmentServices;

    @ManyToOne
    @JoinColumn(name = "region_id")
    private Region region;

    // Getters and Setters (conservez les mêmes)
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

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public String getBusinessRegistrationNumber() {
        return businessRegistrationNumber;
    }

    public void setBusinessRegistrationNumber(String businessRegistrationNumber) {
        this.businessRegistrationNumber = businessRegistrationNumber;
    }

    public String getProfessionalTaxNumber() {
        return professionalTaxNumber;
    }

    public void setProfessionalTaxNumber(String professionalTaxNumber) {
        this.professionalTaxNumber = professionalTaxNumber;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public List<TouristService> getTouristServices() {
        return touristServices;
    }

    public void setTouristServices(List<TouristService> touristServices) {
        this.touristServices = touristServices;
    }

    public List<CollaborationService> getCollaborationServices() {
        return collaborationServices;
    }

    public void setCollaborationServices(List<CollaborationService> collaborationServices) {
        this.collaborationServices = collaborationServices;
    }

    public List<InvestmentService> getInvestmentServices() {
        return investmentServices;
    }

    public void setInvestmentServices(List<InvestmentService> investmentServices) {
        this.investmentServices = investmentServices;
    }

    public String getLinkedinProfile() {
        return linkedinProfile;
    }

    public void setLinkedinProfile(String linkedinProfile) {
        this.linkedinProfile = linkedinProfile;
    }

    public ActivityDomain getActivityDomain() {
        return activityDomain;
    }

    public void setActivityDomain(ActivityDomain activityDomain) {
        this.activityDomain = activityDomain;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
