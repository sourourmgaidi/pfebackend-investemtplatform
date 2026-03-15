package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.iset.investplatformpfe.Entity.LocalProduct;
import tn.iset.investplatformpfe.Entity.Region;
import java.util.List;

@Repository
public interface LocalProductRepository extends JpaRepository<LocalProduct, Long> {

    // Recherche par catégorie
    List<LocalProduct> findByCategory(String category);

    // Recherche par nom (contient)
    List<LocalProduct> findByNameContainingIgnoreCase(String keyword);

    // Recherche par catégorie et nom
    List<LocalProduct> findByCategoryAndNameContainingIgnoreCase(String category, String keyword);

    // Vérifier si un nom existe
    boolean existsByName(String name);

    // Récupérer toutes les catégories distinctes
    @Query("SELECT DISTINCT p.category FROM LocalProduct p WHERE p.category IS NOT NULL")
    List<String> findAllCategories();

    // Compter par catégorie
    @Query("SELECT COUNT(p) FROM LocalProduct p WHERE p.category = :category")
    Long countByCategory(@Param("category") String category);

    // Statistiques par catégorie
    @Query("SELECT p.category, COUNT(p) FROM LocalProduct p GROUP BY p.category")
    List<Object[]> getCategoryStatistics();

    // Trouver les produits par région (via la relation ManyToMany)
    @Query("SELECT p FROM LocalProduct p JOIN p.regions r WHERE r.id = :regionId")
    List<LocalProduct> findByRegionId(@Param("regionId") Long regionId);

    // Trouver les produits avec leurs régions
    @Query("SELECT p, r FROM LocalProduct p LEFT JOIN p.regions r")
    List<Object[]> findProductsWithRegions();
}
