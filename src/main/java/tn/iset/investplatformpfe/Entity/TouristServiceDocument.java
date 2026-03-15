package tn.iset.investplatformpfe.Entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;  // ← Changement ici
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tourist_service_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"touristService"})  // ← IGNORE LA RELATION AU NIVEAU CLASSE
public class TouristServiceDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    private String fileType;

    private Long fileSize;

    @Column(nullable = false, unique = true)
    private String filePath;

    private String downloadUrl;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tourist_service_id")
    private TouristService touristService;  // ← Plus d'annotation ici

    @Column(updatable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }
}