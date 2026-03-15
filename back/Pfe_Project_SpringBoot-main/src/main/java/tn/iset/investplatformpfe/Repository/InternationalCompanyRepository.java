package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.iset.investplatformpfe.Entity.internationalcompany;

import java.util.Optional;

@Repository
public interface InternationalCompanyRepository extends JpaRepository<internationalcompany, Long> {
    boolean existsByEmail(String email);
    boolean existsBySiret(String siret);
    Optional<internationalcompany> findByEmail(String email);
}
