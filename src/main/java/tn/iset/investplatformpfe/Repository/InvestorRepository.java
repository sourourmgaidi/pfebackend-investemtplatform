package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.iset.investplatformpfe.Entity.Investor;
import tn.iset.investplatformpfe.Entity.Role;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
@Repository
public interface InvestorRepository extends JpaRepository<Investor, Long> {
    Optional<Investor> findByEmail(String email);

    boolean existsByEmail(String email);
    long count();

    List<Investor> findByActive(Boolean active);

    List<Investor> findByRole(Role role);
    List<Investor> findByFirstNameContainingOrLastNameContainingOrEmailContaining(String firstName, String lastName, String email);

    // Déjà existant (mois/année)
    @Query("SELECT COUNT(i) FROM Investor i " +
            "WHERE YEAR(i.registrationDate) = :year " +
            "AND MONTH(i.registrationDate) = :month")
    int countByYearAndMonth(@Param("year") int year, @Param("month") int month);

    // NOUVEAU : compter par jour dans une plage de dates
    @Query("SELECT COUNT(i) FROM Investor i " +
            "WHERE i.registrationDate >= :start " +
            "AND i.registrationDate < :end")
    int countByDateRange(@Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end);

}
