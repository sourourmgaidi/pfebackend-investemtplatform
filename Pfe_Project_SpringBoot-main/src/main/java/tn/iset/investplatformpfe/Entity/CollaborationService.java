package tn.iset.investplatformpfe.Entity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "collaboration_services")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Region region;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private PartenaireLocal provider;

    // BigDecimal = meilleur pour les prix
    @Column(nullable = false)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Availability availability;

    @Column(name = "publication_date")
    private LocalDate publicationDate;

    @Column(name = "contact_person", nullable = false)
    private String contactPerson;


    // ===============================
    // Détails collaboration
    // ===============================
    private String collaborationType;

    private String activityDomain;

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
    // Statut Admin (IMPORTANT)
    // ===============================
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ServiceStatus status = ServiceStatus.PENDING;


    // ===============================
    // Champs système
    // ===============================
    @Column(updatable = false)
    private LocalDateTime createdAt;


    // automatique à l'insertion
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

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



    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
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

    public String getCollaborationType() {
        return collaborationType;
    }

    public void setCollaborationType(String collaborationType) {
        this.collaborationType = collaborationType;
    }

    public String getActivityDomain() {
        return activityDomain;
    }

    public void setActivityDomain(String activityDomain) {
        this.activityDomain = activityDomain;
    }

    public String getExpectedBenefits() {
        return expectedBenefits;
    }

    public void setExpectedBenefits(String expectedBenefits) {
        this.expectedBenefits = expectedBenefits;
    }

    public List<String> getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(List<String> requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public String getCollaborationDuration() {
        return collaborationDuration;
    }

    public void setCollaborationDuration(String collaborationDuration) {
        this.collaborationDuration = collaborationDuration;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public PartenaireLocal getProvider() {
        return provider;
    }

    public void setProvider(PartenaireLocal provider) {
        this.provider = provider;
    }
}
