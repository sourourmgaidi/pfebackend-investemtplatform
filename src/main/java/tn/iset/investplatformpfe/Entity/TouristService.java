package tn.iset.investplatformpfe.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "tourist_services")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TouristService {

    // =========================
    // ID
    // =========================
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================
    // Basic information
    // =========================
    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @ManyToOne
    @JoinColumn(name = "region_id", nullable = false)
    @JsonIgnoreProperties({"touristServices", "collaborationServices", "investmentServices"})
    private Region region;

    // relation with LocalPartner entity (changed from PartenaireLocal)
    @ManyToOne
    @JoinColumn(name = "provider_id")
    @JsonIgnoreProperties({"touristServices", "collaborationServices", "investmentServices", "password", "motDePasse"})
    private LocalPartner provider; // Changé de PartenaireLocal à LocalPartner

    // =========================
    // Pricing
    // =========================
    @Column(nullable = false)
    private BigDecimal price;

    private BigDecimal groupPrice;

    // =========================
    // Availability
    // =========================
    @Enumerated(EnumType.STRING)
    private Availability availability;

    private LocalDate publicationDate;

    // =========================
    // Details
    // =========================
    private String contactPerson;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Enumerated(EnumType.STRING)
    private TargetAudience targetAudience;

    // duration in hours
    private Integer durationHours;

    private Integer maxCapacity;

    // =========================
    // Lists
    // =========================
    @ElementCollection
    private List<String> includedServices;

    @ElementCollection
    private List<String> availableLanguages;

    @OneToMany(mappedBy = "touristService", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("isPrimary DESC, uploadedAt ASC")
    @JsonIgnoreProperties({"touristService"})
    private List<TouristServiceDocument> documents = new ArrayList<>();

    // =========================
    // Admin status
    // =========================

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ServiceStatus status = ServiceStatus.PENDING;
    @Column(name = "edit_authorized_until")
    private LocalDateTime editAuthorizedUntil;

    @Column(name = "delete_authorized")
    private Boolean deleteAuthorized = false;

    @Column(name = "authorized_by_admin_id")
    private Long authorizedByAdminId;

    // =========================
    // System fields
    // =========================
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    public void addDocument(TouristServiceDocument document) {
        documents.add(document);
        document.setTouristService(this);
    }

    public void removeDocument(TouristServiceDocument document) {
        documents.remove(document);
        document.setTouristService(null);
    }

    public TouristServiceDocument getPrimaryDocument() {
        return documents.stream()
                .filter(TouristServiceDocument::getIsPrimary)
                .findFirst()
                .orElse(documents.isEmpty() ? null : documents.get(0));
    }

    @JsonIgnore  // ← AJOUTEZ CETTE ANNOTATION
    public List<TouristServiceDocument> getImages() {
        return documents.stream()
                .filter(doc -> doc.getFileType() != null && doc.getFileType().startsWith("image/"))
                .collect(Collectors.toList());
    }

    @JsonIgnore
    public List<TouristServiceDocument> getOtherDocuments() {
        return documents.stream()
                .filter(doc -> doc.getFileType() == null || !doc.getFileType().startsWith("image/"))
                .collect(Collectors.toList());
    }

    public String getFirstImageUrl() {
        TouristServiceDocument primary = getPrimaryDocument();
        if (primary != null) {
            return primary.getDownloadUrl();
        }
        List<TouristServiceDocument> images = getImages();
        return images.isEmpty() ? null : images.get(0).getDownloadUrl();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getEditAuthorizedUntil() {
        return editAuthorizedUntil;
    }

    public void setEditAuthorizedUntil(LocalDateTime editAuthorizedUntil) {
        this.editAuthorizedUntil = editAuthorizedUntil;
    }

    public Boolean getDeleteAuthorized() {
        return deleteAuthorized;
    }

    public void setDeleteAuthorized(Boolean deleteAuthorized) {
        this.deleteAuthorized = deleteAuthorized;
    }

    public Long getAuthorizedByAdminId() {
        return authorizedByAdminId;
    }

    public void setAuthorizedByAdminId(Long authorizedByAdminId) {
        this.authorizedByAdminId = authorizedByAdminId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
    }

    public LocalPartner getProvider() {
        return provider;
    }

    public void setProvider(LocalPartner provider) {
        this.provider = provider;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getGroupPrice() {
        return groupPrice;
    }

    public void setGroupPrice(BigDecimal groupPrice) {
        this.groupPrice = groupPrice;
    }

    public Availability getAvailability() {
        return availability;
    }

    public void setAvailability(Availability availability) {
        this.availability = availability;
    }

    public LocalDate getPublicationDate() {
        return publicationDate;
    }

    public void setPublicationDate(LocalDate publicationDate) {
        this.publicationDate = publicationDate;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public TargetAudience getTargetAudience() {
        return targetAudience;
    }

    public void setTargetAudience(TargetAudience targetAudience) {
        this.targetAudience = targetAudience;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public List<String> getIncludedServices() {
        return includedServices;
    }

    public void setIncludedServices(List<String> includedServices) {
        this.includedServices = includedServices;
    }

    public List<String> getAvailableLanguages() {
        return availableLanguages;
    }

    public void setAvailableLanguages(List<String> availableLanguages) {
        this.availableLanguages = availableLanguages;
    }

    public ServiceStatus getStatus() {
        return status;
    }

    public void setStatus(ServiceStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public List<TouristServiceDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(List<TouristServiceDocument> documents) {
        this.documents = documents;
    }
    // =========================
// Méthodes d'autorisation
// =========================
    public boolean isEditAuthorized() {
        return editAuthorizedUntil != null && editAuthorizedUntil.isAfter(LocalDateTime.now());
    }

    public boolean isDeleteAuthorized() {
        return deleteAuthorized != null && deleteAuthorized;
    }

}