package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.iset.investplatformpfe.Entity.BusinessOpportunity;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BusinessOpportunityRepository extends JpaRepository<BusinessOpportunity, Long> {

    List<BusinessOpportunity> findByType(String type);

    List<BusinessOpportunity> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String title, String description);

    List<BusinessOpportunity> findByCreatedAtAfter(LocalDateTime date);

    List<BusinessOpportunity> findByTypeAndTitleContainingIgnoreCase(String type, String keyword);
}