package tn.iset.investplatformpfe.Entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name = "partenaires_locaux")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class PartenaireLocal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Nom est obligatoire")
    private String nom;

    @NotBlank(message = "Prénom est obligatoire")
    private String prenom;

    @Email(message = "Email doit être valide")
    @NotBlank(message = "Email est obligatoire")
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank(message = "Mot de passe est obligatoire")
    private String motDePasse;

    private String telephone;

    @Column(name = "photo_profil")
    private String photoProfil;

    @Column(name = "date_inscription")
    private LocalDateTime dateInscription;

    private Boolean actif = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)

    private Role role = Role.LOCAL_PARTNER;

    @Column(name = "site_web")
    private String siteWeb;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "statut")
    private String statut; // "actif", "inactif", "en attente"

    @Column(name = "domaine_activite")
    private String domaineActivite;

    @Column(name = "numero_registre_commerce")
    private String numeroRegistreCommerce;

    @Column(name = "taxe_professionnelle")
    private String taxeProfessionnelle;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotBlank(message = "Nom est obligatoire") String getNom() {
        return nom;
    }

    public void setNom(@NotBlank(message = "Nom est obligatoire") String nom) {
        this.nom = nom;
    }

    public @NotBlank(message = "Prénom est obligatoire") String getPrenom() {
        return prenom;
    }

    public void setPrenom(@NotBlank(message = "Prénom est obligatoire") String prenom) {
        this.prenom = prenom;
    }

    public @Email(message = "Email doit être valide") @NotBlank(message = "Email est obligatoire") String getEmail() {
        return email;
    }

    public void setEmail(@Email(message = "Email doit être valide") @NotBlank(message = "Email est obligatoire") String email) {
        this.email = email;
    }

    public @NotBlank(message = "Mot de passe est obligatoire") String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(@NotBlank(message = "Mot de passe est obligatoire") String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getPhotoProfil() {
        return photoProfil;
    }

    public void setPhotoProfil(String photoProfil) {
        this.photoProfil = photoProfil;
    }

    public LocalDateTime getDateInscription() {
        return dateInscription;
    }

    public void setDateInscription(LocalDateTime dateInscription) {
        this.dateInscription = dateInscription;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getSiteWeb() {
        return siteWeb;
    }

    public void setSiteWeb(String siteWeb) {
        this.siteWeb = siteWeb;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getDomaineActivite() {
        return domaineActivite;
    }

    public void setDomaineActivite(String domaineActivite) {
        this.domaineActivite = domaineActivite;
    }

    public String getNumeroRegistreCommerce() {
        return numeroRegistreCommerce;
    }

    public void setNumeroRegistreCommerce(String numeroRegistreCommerce) {
        this.numeroRegistreCommerce = numeroRegistreCommerce;
    }

    public String getTaxeProfessionnelle() {
        return taxeProfessionnelle;
    }

    public void setTaxeProfessionnelle(String taxeProfessionnelle) {
        this.taxeProfessionnelle = taxeProfessionnelle;
    }
}
