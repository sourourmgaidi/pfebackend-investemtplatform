package tn.iset.investplatformpfe.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "tourist_services")
@Data
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

    @Enumerated(EnumType.STRING)
    private Region region;


    // relation with User entity
    @ManyToOne
    @JoinColumn(name = "provider_id")
    private PartenaireLocal provider;


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


    // =========================
    // Admin status
    // =========================
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ServiceStatus status = ServiceStatus.PENDING;


    // =========================
    // System fields
    // =========================
    @Column(updatable = false)
    private LocalDateTime createdAt;


    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}

