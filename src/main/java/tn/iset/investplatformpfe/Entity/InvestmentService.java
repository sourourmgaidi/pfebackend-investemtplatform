package tn.iset.investplatformpfe.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "investment_services")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvestmentService {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Availability availability;

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    @Column(name = "contact_person", nullable = false)
    private String contactPerson;

    // ===============================
    // Détails spécifiques à l'investissement
    // ===============================
    @Column(nullable = false)
    private String title;

    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ServiceStatus status = ServiceStatus.PENDING;

    private String zone;

    @ManyToOne
    @JoinColumn(name = "economic_sector_id")
    private EconomicSector economicSector;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "minimum_amount", precision = 15, scale = 2)
    private BigDecimal minimumAmount;

    @Column(name = "deadline_date")
    private LocalDate deadlineDate;

    @Column(name = "project_duration")
    private String projectDuration;

    // ===============================
    // ✅ DOCUMENTS ET PHOTOS (NOUVEAU - REMPLACE attachedDocuments)
    // ===============================
    @OneToMany(mappedBy = "investmentService", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("isPrimary DESC, uploadedAt ASC")
    @JsonIgnoreProperties({"investmentService"})
    @Builder.Default
    private List<InvestmentServiceDocument> documents = new ArrayList<>();

    // ===============================
    // Relations existantes
    // ===============================
    @ManyToMany
    @JoinTable(
            name = "investment_service_interested_investors",
            joinColumns = @JoinColumn(name = "investment_service_id"),
            inverseJoinColumns = @JoinColumn(name = "investor_id")
    )
    private List<Investor> interestedInvestors;

    // ===============================
    // Champs système
    // ===============================
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @ManyToMany(mappedBy = "favoriteServices")
    @JsonIgnoreProperties("favoriteServices")
    private List<Investor> favoritedByInvestors = new ArrayList<>();

    @ManyToMany(mappedBy = "favoriteServices")
    @JsonIgnoreProperties("favoriteServices")
    private List<internationalcompany> favoritedByCompanies = new ArrayList<>();

    // ✅ NOUVEAUX CHAMPS POUR LES AUTORISATIONS
    @Column(name = "edit_authorized_until")
    private LocalDateTime editAuthorizedUntil;

    @Column(name = "delete_authorized")
    private Boolean deleteAuthorized = false;

    @Column(name = "authorized_by_admin_id")
    private Long authorizedByAdminId;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.publicationDate == null) {
            this.publicationDate = LocalDate.now();
        }
        this.type = "INVESTMENT";
    }

    // ===============================
    // MÉTHODES UTILITAIRES POUR LES DOCUMENTS
    // ===============================

    /**
     * Ajouter un document à la liste
     */
    public void addDocument(InvestmentServiceDocument document) {
        documents.add(document);
        document.setInvestmentService(this);
    }

    /**
     * Supprimer un document
     */
    public void removeDocument(InvestmentServiceDocument document) {
        documents.remove(document);
        document.setInvestmentService(null);
    }

    /**
     * Récupérer le document principal (première image)
     */
    public InvestmentServiceDocument getPrimaryDocument() {
        return documents.stream()
                .filter(InvestmentServiceDocument::getIsPrimary)
                .findFirst()
                .orElse(documents.isEmpty() ? null : documents.get(0));
    }

    /**
     * Récupérer toutes les images
     */
    public List<InvestmentServiceDocument> getImages() {
        return documents.stream()
                .filter(doc -> doc.getFileType() != null && doc.getFileType().startsWith("image/"))
                .collect(Collectors.toList());
    }

    /**
     * Récupérer tous les documents non-images (PDF, Word, etc.)
     */
    public List<InvestmentServiceDocument> getOtherDocuments() {
        return documents.stream()
                .filter(doc -> doc.getFileType() == null || !doc.getFileType().startsWith("image/"))
                .collect(Collectors.toList());
    }

    /**
     * Obtenir l'URL de la première image (pour l'affichage)
     */
    public String getFirstImageUrl() {
        InvestmentServiceDocument primary = getPrimaryDocument();
        if (primary != null) {
            return primary.getDownloadUrl();
        }

        List<InvestmentServiceDocument> images = getImages();
        return images.isEmpty() ? null : images.get(0).getDownloadUrl();
    }

    // ===============================
    // GETTERS ET SETTERS CORRIGÉS
    // ===============================

    // ✅ Getter pour deleteAuthorized
    public Boolean getDeleteAuthorized() {
        return deleteAuthorized != null ? deleteAuthorized : false;
    }

    public boolean isDeleteAuthorized() {
        return deleteAuthorized != null ? deleteAuthorized : false;
    }

    public void setDeleteAuthorized(Boolean deleteAuthorized) {
        this.deleteAuthorized = deleteAuthorized;
    }

    public LocalDateTime getEditAuthorizedUntil() {
        return editAuthorizedUntil;
    }

    public void setEditAuthorizedUntil(LocalDateTime editAuthorizedUntil) {
        this.editAuthorizedUntil = editAuthorizedUntil;
    }

    public Long getAuthorizedByAdminId() {
        return authorizedByAdminId;
    }

    public void setAuthorizedByAdminId(Long authorizedByAdminId) {
        this.authorizedByAdminId = authorizedByAdminId;
    }

    public boolean isEditAuthorized() {
        return editAuthorizedUntil != null && editAuthorizedUntil.isAfter(LocalDateTime.now());
    }

    // ===============================
    // AUTRES GETTERS ET SETTERS
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

    public Availability getAvailability() { return availability; }
    public void setAvailability(Availability availability) { this.availability = availability; }

    public LocalDate getPublicationDate() { return publicationDate; }
    public void setPublicationDate(LocalDate publicationDate) { this.publicationDate = publicationDate; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public ServiceStatus getStatus() { return status; }
    public void setStatus(ServiceStatus status) { this.status = status; }

    public String getZone() { return zone; }
    public void setZone(String zone) { this.zone = zone; }

    public EconomicSector getEconomicSector() { return economicSector; }
    public void setEconomicSector(EconomicSector economicSector) { this.economicSector = economicSector; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getMinimumAmount() { return minimumAmount; }
    public void setMinimumAmount(BigDecimal minimumAmount) { this.minimumAmount = minimumAmount; }

    public LocalDate getDeadlineDate() { return deadlineDate; }
    public void setDeadlineDate(LocalDate deadlineDate) { this.deadlineDate = deadlineDate; }

    public String getProjectDuration() { return projectDuration; }
    public void setProjectDuration(String projectDuration) { this.projectDuration = projectDuration; }

    public List<InvestmentServiceDocument> getDocuments() { return documents; }
    public void setDocuments(List<InvestmentServiceDocument> documents) { this.documents = documents; }

    public List<Investor> getInterestedInvestors() { return interestedInvestors; }
    public void setInterestedInvestors(List<Investor> interestedInvestors) { this.interestedInvestors = interestedInvestors; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<Investor> getFavoritedByInvestors() { return favoritedByInvestors; }
    public void setFavoritedByInvestors(List<Investor> favoritedByInvestors) { this.favoritedByInvestors = favoritedByInvestors; }

    public List<internationalcompany> getFavoritedByCompanies() { return favoritedByCompanies; }
    public void setFavoritedByCompanies(List<internationalcompany> favoritedByCompanies) { this.favoritedByCompanies = favoritedByCompanies; }
}