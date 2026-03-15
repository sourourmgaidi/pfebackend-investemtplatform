package tn.iset.investplatformpfe.Repository;

import tn.iset.investplatformpfe.Entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CollaborationServiceRepository extends JpaRepository<CollaborationService, Long> {

    List<CollaborationService> findByRegion(Region region);

    List<CollaborationService> findByProvider(LocalPartner provider);

    List<CollaborationService> findByStatus(ServiceStatus status);

    List<CollaborationService> findByAvailability(Availability availability);

    // ✅ CORRIGÉ: Remplacer price par requestedBudget
    List<CollaborationService> findByRequestedBudgetBetween(BigDecimal min, BigDecimal max);

    // ✅ CORRIGÉ: findByRequestedBudgetLessThanEqual
    List<CollaborationService> findByRequestedBudgetLessThanEqual(BigDecimal maxBudget);

    // ✅ CORRIGÉ: Utiliser ActivityDomain au lieu de String
    List<CollaborationService> findByRegionAndActivityDomain(Region region, ActivityDomain activityDomain);

    // ✅ CORRIGÉ: Utiliser CollaborationType au lieu de String
    List<CollaborationService> findByCollaborationType(CollaborationType collaborationType);

    // ✅ CORRIGÉ: Utiliser ActivityDomain au lieu de String
    List<CollaborationService> findByActivityDomain(ActivityDomain activityDomain);

    List<CollaborationService> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String nameKeyword, String descriptionKeyword);

    // ✅ NOUVEAU: Pour les autorisations expirées
    List<CollaborationService> findByEditAuthorizedUntilBefore(LocalDateTime now);

    // ========================================
    // ✅ AJOUTER CES NOUVELLES MÉTHODES POUR LES FAVORIS
    // ========================================

    // ✅ Récupérer tous les services approuvés
    @Query("SELECT cs FROM CollaborationService cs WHERE cs.status = 'APPROVED'")
    List<CollaborationService> findAllApproved();

    // ✅ Vérifier si un service est dans les favoris d'une société internationale
    @Query("SELECT CASE WHEN COUNT(ic) > 0 THEN true ELSE false END FROM internationalcompany ic " +
            "JOIN ic.favoriteCollaborationServices cs WHERE ic.id = :companyId AND cs.id = :serviceId")
    boolean isCompanyFavorite(@Param("companyId") Long companyId, @Param("serviceId") Long serviceId);

    // ✅ Vérifier si un service est dans les favoris d'un partenaire économique
    @Query("SELECT CASE WHEN COUNT(ep) > 0 THEN true ELSE false END FROM EconomicPartner ep " +
            "JOIN ep.favoriteCollaborationServices cs WHERE ep.id = :partnerId AND cs.id = :serviceId")
    boolean isPartnerFavorite(@Param("partnerId") Long partnerId, @Param("serviceId") Long serviceId);

    // ✅ Compter le nombre de sociétés internationales qui ont mis ce service en favori
    @Query("SELECT COUNT(ic) FROM internationalcompany ic JOIN ic.favoriteCollaborationServices cs WHERE cs.id = :serviceId")
    long countCompaniesByFavoriteService(@Param("serviceId") Long serviceId);

    // ✅ Compter le nombre de partenaires économiques qui ont mis ce service en favori
    @Query("SELECT COUNT(ep) FROM EconomicPartner ep JOIN ep.favoriteCollaborationServices cs WHERE cs.id = :serviceId")
    long countPartnersByFavoriteService(@Param("serviceId") Long serviceId);
}