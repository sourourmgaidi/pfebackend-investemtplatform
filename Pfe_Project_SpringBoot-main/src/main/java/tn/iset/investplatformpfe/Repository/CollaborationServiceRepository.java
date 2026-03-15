package tn.iset.investplatformpfe.Repository;
import tn.iset.investplatformpfe.Entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CollaborationServiceRepository extends JpaRepository<CollaborationService, Long> {
    List<CollaborationService> findByRegion(Region region);
    List<CollaborationService> findByProvider(PartenaireLocal provider);
    List<CollaborationService> findByStatus(ServiceStatus status);
    List<CollaborationService> findByAvailability(Availability availability);
    List<CollaborationService> findByPriceBetween(BigDecimal min, BigDecimal max);
    List<CollaborationService> findByRegionAndActivityDomain(Region region, String activityDomain);
    List<CollaborationService> findByCollaborationType(String collaborationType);
    List<CollaborationService> findByActivityDomain(String activityDomain);
    List<CollaborationService> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String nameKeyword, String descriptionKeyword);
}