package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.iset.investplatformpfe.Entity.Tourist;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TouristRepository extends JpaRepository<Tourist, Long> {

    // Vérifier si un email existe déjà
    boolean existsByEmail(String email);
    long count();

    // Trouver un touriste par email
    Optional<Tourist> findByEmail(String email);

    // Optionnel: trouver par nom et prénom
    // Optional<Tourist> findByLastNameAndFirstName(String lastName, String firstName);

    // Optionnel: trouver les touristes par nationalité
    // List<Tourist> findByNationality(String nationality);

    // Optionnel: trouver les touristes actifs
    // List<Tourist> findByActiveTrue();
    List<Tourist> findByFirstNameContainingOrLastNameContainingOrEmailContaining(String firstName, String lastName, String email);
    // Déjà existant (mois/année)
    @Query("SELECT COUNT(t) FROM Tourist t " +
            "WHERE YEAR(t.registrationDate) = :year " +
            "AND MONTH(t.registrationDate) = :month")
    int countByYearAndMonth(@Param("year") int year, @Param("month") int month);

    //CORRIGÉ : Compter par plage de dates pour Tourist
    @Query("SELECT COUNT(t) FROM Tourist t " +
            "WHERE t.registrationDate >= :start " +
            "AND t.registrationDate < :end")
    int countByDateRange(@Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end);
}