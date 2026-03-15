package tn.iset.investplatformpfe.Repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.iset.investplatformpfe.Entity.Region;

import java.util.List;
import java.util.Optional;

@Repository
public interface RegionRepository extends JpaRepository<Region, Long> {

    // Find by name
    Optional<Region> findByName(String name);

    // Find by code
    Optional<Region> findByCode(String code);

    // Find by geographical zone
    List<Region> findByGeographicalZone(String geographicalZone);

    // Check if exists by name
    boolean existsByName(String name);

    // Custom query to find regions with most collaborations
    @Query("SELECT r, COUNT(c) as collaborationCount FROM Region r LEFT JOIN r.collaborations c GROUP BY r ORDER BY collaborationCount DESC")
    List<Object[]> findRegionsWithCollaborationCount();

    // Find regions with specific economic sector
    @Query("SELECT DISTINCT r FROM Region r JOIN r.potentialSectors s WHERE s.id = :sectorId")
    List<Region> findByPotentialSectorId(@Param("sectorId") Long sectorId);

    // Find regions with specific local product
    @Query("SELECT DISTINCT r FROM Region r JOIN r.localProducts p WHERE p.id = :productId")
    List<Region> findByLocalProductId(@Param("productId") Long productId);

    // Search regions by name or zone
    List<Region> findByNameContainingIgnoreCaseOrGeographicalZoneContainingIgnoreCase(String name, String zone);
}
