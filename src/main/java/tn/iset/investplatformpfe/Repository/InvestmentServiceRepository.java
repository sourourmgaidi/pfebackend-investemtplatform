package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.iset.investplatformpfe.Entity.CollaborationService;
import tn.iset.investplatformpfe.Entity.InvestmentService;
import tn.iset.investplatformpfe.Entity.ServiceStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InvestmentServiceRepository extends JpaRepository<InvestmentService, Long> {

    // ✅ Par fournisseur (provider)
    List<InvestmentService> findByProviderId(Long providerId);

    // ✅ Par région
    List<InvestmentService> findByRegionId(Long regionId);

    // ✅ Par statut
    List<InvestmentService> findByStatus(ServiceStatus status);

    // ✅ Par zone
    List<InvestmentService> findByZone(String zone);

    // ✅ Par montant minimum
    List<InvestmentService> findByMinimumAmountLessThanEqual(BigDecimal amount);

    // ✅ Par date limite (après une certaine date)
    List<InvestmentService> findByDeadlineDateAfter(LocalDate date);

    // ✅ Recherche par mot-clé (nom, description, titre)
    List<InvestmentService> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrTitleContainingIgnoreCase(
            String name, String description, String title);

    // ✅ Recherche avancée
    @Query("SELECT i FROM InvestmentService i WHERE " +
            "(:regionId IS NULL OR i.region.id = :regionId) AND " +
            "(:sectorId IS NULL OR i.economicSector.id = :sectorId) AND " +
            "(:status IS NULL OR i.status = :status) AND " +
            "(:maxAmount IS NULL OR i.minimumAmount <= :maxAmount)")
    List<InvestmentService> advancedSearch(
            @Param("regionId") Long regionId,
            @Param("sectorId") Long sectorId,
            @Param("status") ServiceStatus status,
            @Param("maxAmount") BigDecimal maxAmount
    );
    @Query("SELECT s FROM InvestmentService s WHERE s.editAuthorizedUntil < :now")
    List<InvestmentService> findByEditAuthorizedUntilBefore(@Param("now") LocalDateTime now);
}