package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.iset.investplatformpfe.Entity.LocalPartner;

import java.util.List;
import java.util.Optional;

public interface LocalPartnerRepository extends JpaRepository<LocalPartner, Long> {


    boolean existsByEmail(String email);


    // Optionnel: rechercher par domaine d'activité
    List<LocalPartner> findByActivityDomain(String activityDomain);

    // Optionnel: rechercher par statut
    List<LocalPartner> findByStatus(String status);

    // Optionnel: rechercher les partenaires actifs
    List<LocalPartner> findByActiveTrue();
    List<LocalPartner> findByFirstNameContainingOrLastNameContainingOrEmailContaining(String firstName, String lastName, String email);

    Optional<LocalPartner> findByEmail(String email);

    @Query("SELECT lp FROM LocalPartner lp WHERE " +
            "LOWER(lp.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(lp.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(lp.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<LocalPartner> searchPartners(@Param("search") String search);
}