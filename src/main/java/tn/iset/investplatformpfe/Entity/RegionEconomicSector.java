package tn.iset.investplatformpfe.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "region_economic_sectors")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegionEconomicSector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "region_id", nullable = false)
    @JsonIgnore
    private Region region;

    @ManyToOne
    @JoinColumn(name = "sector_id", nullable = false)
    @JsonIgnore
    private EconomicSector economicSector;

    // Getters et Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Region getRegion() { return region; }
    public void setRegion(Region region) { this.region = region; }

    public EconomicSector getEconomicSector() { return economicSector; }
    public void setEconomicSector(EconomicSector economicSector) { this.economicSector = economicSector; }
}
