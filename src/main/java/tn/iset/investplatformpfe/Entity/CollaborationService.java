package tn.iset.investplatformpfe.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "collaboration_services")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CollaborationService {

    // ===============================
    // ID
    // ===============================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ===============================
    // Informations principales
    // ===============================
    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @ManyToOne
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    @JsonIgnoreProperties({"collaborationServices", "investmentServices", "touristServices", "motDePasse", "password"})
    private LocalPartner provider;

    // Budget demandé par le partenaire
    @Column(nullable = false)
    private BigDecimal requestedBudget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Availability availability;

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    @Column(name = "contact_person", nullable = false)
    private String contactPerson;

    // ===============================
    // Détails collaboration avec ENUMS
    // ===============================
    @Enumerated(EnumType.STRING)
    @Column(name = "collaboration_type")
    private CollaborationType collaborationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_domain")
    private ActivityDomain activityDomain;

    @Column(length = 1000)
    private String expectedBenefits;

    @ElementCollection
    @CollectionTable(
            name = "collaboration_service_required_skills",
            joinColumns = @JoinColumn(name = "collaboration_service_id")
    )
    @Column(name = "skill")
    private List<String> requiredSkills;

    private String collaborationDuration;

    // ===============================
    // Adresse
    // ===============================
    private String address;

    // ===============================
    // Statut Admin
    // ===============================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceStatus status = ServiceStatus.PENDING;

    // ===============================
    // ✅ FAVORIS (À AJOUTER)
    // ===============================
    @ManyToMany(mappedBy = "favoriteCollaborationServices")
    @JsonIgnoreProperties("favoriteCollaborationServices")
    private List<internationalcompany> favoritedByCompanies = new ArrayList<>();

    @ManyToMany(mappedBy = "favoriteCollaborationServices")
    @JsonIgnoreProperties("favoriteCollaborationServices")
    private List<EconomicPartner> favoritedByPartners = new ArrayList<>();

    // ===============================
    // ✅ DOCUMENTS ET PHOTOS (déplacé ici pour meilleure organisation)
    // ===============================
    @OneToMany(mappedBy = "collaborationService", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("isPrimary DESC, uploadedAt ASC")
    @JsonIgnoreProperties({"collaborationService"})
    private List<CollaborationServiceDocument> documents = new ArrayList<>();

    // ===============================
    // CHAMPS POUR LES AUTORISATIONS (DEMANDES)
    // ===============================
    @Column(name = "edit_authorized_until")
    private LocalDateTime editAuthorizedUntil;

    @Column(name = "delete_authorized")
    private Boolean deleteAuthorized = false;

    @Column(name = "authorized_by_admin_id")
    private Long authorizedByAdminId;

    // ===============================
    // Champs système
    // ===============================
    @Column(updatable = false)
    private LocalDateTime createdAt;
    // ===============================
// CHAMPS POUR LE REJET
// ===============================
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejected_by_admin_id")
    private Long rejectedByAdminId;
    // ===============================
    // Constructeurs
    // ===============================
    public CollaborationService() {
    }

    public CollaborationService(Long id, String name, String description, Region region, LocalPartner provider, BigDecimal requestedBudget, Availability availability, LocalDate publicationDate, String contactPerson, CollaborationType collaborationType, ActivityDomain activityDomain, String expectedBenefits, List<String> requiredSkills, String collaborationDuration, String address, ServiceStatus status, List<internationalcompany> favoritedByCompanies, List<EconomicPartner> favoritedByPartners, List<CollaborationServiceDocument> documents, LocalDateTime editAuthorizedUntil, Boolean deleteAuthorized, Long authorizedByAdminId, LocalDateTime createdAt, String rejectionReason, LocalDateTime rejectedAt, Long rejectedByAdminId) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.region = region;
        this.provider = provider;
        this.requestedBudget = requestedBudget;
        this.availability = availability;
        this.publicationDate = publicationDate;
        this.contactPerson = contactPerson;
        this.collaborationType = collaborationType;
        this.activityDomain = activityDomain;
        this.expectedBenefits = expectedBenefits;
        this.requiredSkills = requiredSkills;
        this.collaborationDuration = collaborationDuration;
        this.address = address;
        this.status = status;
        this.favoritedByCompanies = favoritedByCompanies;
        this.favoritedByPartners = favoritedByPartners;
        this.documents = documents;
        this.editAuthorizedUntil = editAuthorizedUntil;
        this.deleteAuthorized = deleteAuthorized;
        this.authorizedByAdminId = authorizedByAdminId;
        this.createdAt = createdAt;
        this.rejectionReason = rejectionReason;
        this.rejectedAt = rejectedAt;
        this.rejectedByAdminId = rejectedByAdminId;
    }

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.publicationDate == null) {
            this.publicationDate = LocalDate.now();
        }
    }

    // ===============================
    // Méthodes utilitaires pour les autorisations
    // ===============================
    public boolean isEditAuthorized() {
        return editAuthorizedUntil != null && editAuthorizedUntil.isAfter(LocalDateTime.now());
    }

    public boolean isDeleteAuthorized() {
        return deleteAuthorized != null && deleteAuthorized;
    }

    // ===============================
    // ✅ MÉTHODES UTILITAIRES POUR LES DOCUMENTS
    // ===============================
    public void addDocument(CollaborationServiceDocument document) {
        documents.add(document);
        document.setCollaborationService(this);
    }

    public void removeDocument(CollaborationServiceDocument document) {
        documents.remove(document);
        document.setCollaborationService(null);
    }

    public CollaborationServiceDocument getPrimaryDocument() {
        return documents.stream()
                .filter(CollaborationServiceDocument::getIsPrimary)
                .findFirst()
                .orElse(documents.isEmpty() ? null : documents.get(0));
    }

    public List<CollaborationServiceDocument> getImages() {
        return documents.stream()
                .filter(doc -> doc.getFileType() != null && doc.getFileType().startsWith("image/"))
                .collect(Collectors.toList());
    }

    public List<CollaborationServiceDocument> getOtherDocuments() {
        return documents.stream()
                .filter(doc -> doc.getFileType() == null || !doc.getFileType().startsWith("image/"))
                .collect(Collectors.toList());
    }

    public String getFirstImageUrl() {
        CollaborationServiceDocument primary = getPrimaryDocument();
        if (primary != null) {
            return primary.getDownloadUrl();
        }
        List<CollaborationServiceDocument> images = getImages();
        return images.isEmpty() ? null : images.get(0).getDownloadUrl();
    }

    // ===============================
    // Getters et Setters
    // ===============================
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }

    public LocalPartner getProvider() { return provider; }
    public void setProvider(LocalPartner provider) { this.provider = provider; }

    public BigDecimal getRequestedBudget() { return requestedBudget; }
    public void setRequestedBudget(BigDecimal requestedBudget) {
        this.requestedBudget = requestedBudget;
    }

    public Availability getAvailability() { return availability; }
    public void setAvailability(Availability availability) { this.availability = availability; }

    public LocalDate getPublicationDate() { return publicationDate; }
    public void setPublicationDate(LocalDate publicationDate) { this.publicationDate = publicationDate; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public CollaborationType getCollaborationType() { return collaborationType; }
    public void setCollaborationType(CollaborationType collaborationType) {
        this.collaborationType = collaborationType;
    }

    public ActivityDomain getActivityDomain() { return activityDomain; }
    public void setActivityDomain(ActivityDomain activityDomain) {
        this.activityDomain = activityDomain;
    }

    public String getExpectedBenefits() { return expectedBenefits; }
    public void setExpectedBenefits(String expectedBenefits) { this.expectedBenefits = expectedBenefits; }

    public List<String> getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(List<String> requiredSkills) { this.requiredSkills = requiredSkills; }

    public String getCollaborationDuration() { return collaborationDuration; }
    public void setCollaborationDuration(String collaborationDuration) { this.collaborationDuration = collaborationDuration; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public ServiceStatus getStatus() { return status; }
    public void setStatus(ServiceStatus status) { this.status = status; }

    // ✅ Getter/Setter pour les documents
    public List<CollaborationServiceDocument> getDocuments() { return documents; }
    public void setDocuments(List<CollaborationServiceDocument> documents) { this.documents = documents; }

    // Getters/Setters pour les champs d'autorisation
    public LocalDateTime getEditAuthorizedUntil() { return editAuthorizedUntil; }
    public void setEditAuthorizedUntil(LocalDateTime editAuthorizedUntil) {
        this.editAuthorizedUntil = editAuthorizedUntil;
    }

    public Boolean getDeleteAuthorized() {
        return deleteAuthorized != null ? deleteAuthorized : false;
    }
    public void setDeleteAuthorized(Boolean deleteAuthorized) {
        this.deleteAuthorized = deleteAuthorized;
    }

    public Long getAuthorizedByAdminId() { return authorizedByAdminId; }
    public void setAuthorizedByAdminId(Long authorizedByAdminId) {
        this.authorizedByAdminId = authorizedByAdminId;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<internationalcompany> getFavoritedByCompanies() {
        return favoritedByCompanies;
    }

    public void setFavoritedByCompanies(List<internationalcompany> favoritedByCompanies) {
        this.favoritedByCompanies = favoritedByCompanies;
    }

    public List<EconomicPartner> getFavoritedByPartners() {
        return favoritedByPartners;
    }

    public void setFavoritedByPartners(List<EconomicPartner> favoritedByPartners) {
        this.favoritedByPartners = favoritedByPartners;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public Long getRejectedByAdminId() {
        return rejectedByAdminId;
    }

    public void setRejectedByAdminId(Long rejectedByAdminId) {
        this.rejectedByAdminId = rejectedByAdminId;
    }
}