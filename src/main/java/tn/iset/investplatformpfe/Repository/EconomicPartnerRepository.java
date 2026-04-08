package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.iset.investplatformpfe.Entity.EconomicPartner;  // Import corrigé

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EconomicPartnerRepository extends JpaRepository<EconomicPartner, Long> {  // Type générique corrigé

    // Vérifier si un email existe déjà
    boolean existsByEmail(String email);

    // Trouver un partenaire économique par email
    Optional<EconomicPartner> findByEmail(String email);  // Type de retour corrigé

    // Optionnel: trouver par nom de famille
    // List<EconomicPartner> findByLastName(String lastName);

    // Optionnel: trouver par pays d'origine
    // List<EconomicPartner> findByCountryOfOrigin(String countryOfOrigin);

    // Optionnel: trouver par secteur d'activité
    // List<EconomicPartner> findByBusinessSector(String businessSector);

    // Optionnel: trouver les partenaires actifs
    // List<EconomicPartner> findByActiveTrue();

    long count();

    List<EconomicPartner> findByFirstNameContainingOrLastNameContainingOrEmailContaining(String firstName, String lastName, String email);
    // Déjà existant (mois/année)
    @Query("SELECT COUNT(e) FROM EconomicPartner e " +
            "WHERE YEAR(e.registrationDate) = :year " +
            "AND MONTH(e.registrationDate) = :month")
    int countByYearAndMonth(@Param("year") int year, @Param("month") int month);

    // ✅ CORRIGÉ : Compter par plage de dates pour EconomicPartner
    @Query("SELECT COUNT(e) FROM EconomicPartner e " +
            "WHERE e.registrationDate >= :start " +
            "AND e.registrationDate < :end")
    int countByDateRange(@Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end);

}