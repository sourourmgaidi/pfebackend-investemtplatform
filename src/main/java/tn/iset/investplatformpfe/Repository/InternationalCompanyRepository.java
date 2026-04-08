package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.iset.investplatformpfe.Entity.internationalcompany;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface InternationalCompanyRepository extends JpaRepository<internationalcompany, Long> {
    boolean existsByEmail(String email);
    boolean existsBySiret(String siret);
    Optional<internationalcompany> findByEmail(String email);
    List<internationalcompany> findByCompanyNameContainingOrEmailContainingOrContactFirstNameContaining(
            String companyName, String email, String contactFirstName);
    long count();
    @Query("SELECT COUNT(c) FROM internationalcompany c " +
            "WHERE YEAR(c.registrationDate) = :year " +
            "AND MONTH(c.registrationDate) = :month")
    int countByYearAndMonth(@Param("year") int year, @Param("month") int month);

    // CORRIGÉ : Compter par plage de dates pour internationalcompany
    @Query("SELECT COUNT(c) FROM internationalcompany c " +
            "WHERE c.registrationDate >= :start " +
            "AND c.registrationDate < :end")
    int countByDateRange(@Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end);
}
