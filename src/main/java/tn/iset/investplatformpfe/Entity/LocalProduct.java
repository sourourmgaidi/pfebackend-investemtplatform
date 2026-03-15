package tn.iset.investplatformpfe.Entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "local_products")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocalProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;  // Olive oil, Dates, Handicrafts, etc.

    @Column(length = 500)
    private String description;

    private String category;

    // CORRECTION ICI : Relation ManyToMany avec Region
    @ManyToMany(mappedBy = "localProducts")  // Référence le champ "localProducts" dans Region
    private List<Region> regions;

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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<Region> getRegions() {
        return regions;
    }

    public void setRegions(List<Region> regions) {
        this.regions = regions;
    }
}