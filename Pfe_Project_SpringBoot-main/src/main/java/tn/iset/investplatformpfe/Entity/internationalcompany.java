package tn.iset.investplatformpfe.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.processing.Pattern;

import java.time.LocalDateTime;

@Table(name = "international_companies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class internationalcompany {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================
    // ATTRIBUTS OBLIGATOIRES POUR L'INSCRIPTION
    // ========================================

    @NotBlank(message = "Nom de l'entreprise est obligatoire")
    @Column(nullable = false)
    private String companyName;

    @NotBlank(message = "Nom du contact est obligatoire")
    @Column(nullable = false)
    private String contactLastName;

    @NotBlank(message = "Prénom du contact est obligatoire")
    @Column(nullable = false)
    private String contactFirstName;

    @Email(message = "Email doit être valide")
    @NotBlank(message = "Email est obligatoire")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Mot de passe est obligatoire")
    @Column(nullable = false)
    private String password;

    @NotBlank(message = "Numéro de téléphone est obligatoire")
    @Column(nullable = false)
    private String phone;

    @NotBlank(message = "Pays d'origine est obligatoire")
    @Column(nullable = false)
    private String originCountry;

    @NotBlank(message = "Secteur d'activité est obligatoire")
    @Column(nullable = false)
    private String activitySector;

    @NotBlank(message = "Numéro SIRET est obligatoire")
    @Column(nullable = false, unique = true)
    private String siret;

    @NotBlank(message = "Intérêt principal est obligatoire")
    @Column(nullable = false)

    private String interetPrincipal;
    @Column(nullable = false)
    private Boolean active = true;

    // ========================================
    // ATTRIBUTS OPTIONNELS
    // ========================================

    @Column(name = "site_web")
    private String website;

    @Column(name = "linkedin_profile")
    private String linkedinProfile;

    @Column(name = "profile_picture")
    private String profilePicture;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.INTERNATIONAL_COMPANY;  // Rôle avec valeur par défaut

    @Column(name = "registration_date")
    private LocalDateTime registrationDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotBlank(message = "Nom de l'entreprise est obligatoire") String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(@NotBlank(message = "Nom de l'entreprise est obligatoire") String companyName) {
        this.companyName = companyName;
    }

    public @NotBlank(message = "Nom du contact est obligatoire") String getContactLastName() {
        return contactLastName;
    }

    public void setContactLastName(@NotBlank(message = "Nom du contact est obligatoire") String contactLastName) {
        this.contactLastName = contactLastName;
    }

    public @NotBlank(message = "Prénom du contact est obligatoire") String getContactFirstName() {
        return contactFirstName;
    }

    public void setContactFirstName(@NotBlank(message = "Prénom du contact est obligatoire") String contactFirstName) {
        this.contactFirstName = contactFirstName;
    }

    public @Email(message = "Email doit être valide") @NotBlank(message = "Email est obligatoire") String getEmail() {
        return email;
    }

    public void setEmail(@Email(message = "Email doit être valide") @NotBlank(message = "Email est obligatoire") String email) {
        this.email = email;
    }

    public @NotBlank(message = "Mot de passe est obligatoire") String getPassword() {
        return password;
    }

    public void setPassword(@NotBlank(message = "Mot de passe est obligatoire") String password) {
        this.password = password;
    }

    public @NotBlank(message = "Numéro de téléphone est obligatoire") String getPhone() {
        return phone;
    }

    public void setPhone(@NotBlank(message = "Numéro de téléphone est obligatoire") String phone) {
        this.phone = phone;
    }

    public @NotBlank(message = "Pays d'origine est obligatoire") String getOriginCountry() {
        return originCountry;
    }

    public void setOriginCountry(@NotBlank(message = "Pays d'origine est obligatoire") String originCountry) {
        this.originCountry = originCountry;
    }

    public @NotBlank(message = "Secteur d'activité est obligatoire") String getActivitySector() {
        return activitySector;
    }

    public void setActivitySector(@NotBlank(message = "Secteur d'activité est obligatoire") String activitySector) {
        this.activitySector = activitySector;
    }

    public @NotBlank(message = "Numéro SIRET est obligatoire") String getSiret() {
        return siret;
    }

    public void setSiret(@NotBlank(message = "Numéro SIRET est obligatoire") String siret) {
        this.siret = siret;
    }

    public @NotBlank(message = "Intérêt principal est obligatoire") String getInteretPrincipal() {
        return interetPrincipal;
    }

    public void setInteretPrincipal(@NotBlank(message = "Intérêt principal est obligatoire") String interetPrincipal) {
        this.interetPrincipal = interetPrincipal;
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

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
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

    public LocalDateTime getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDateTime registrationDate) {
        this.registrationDate = registrationDate;
    }
}
