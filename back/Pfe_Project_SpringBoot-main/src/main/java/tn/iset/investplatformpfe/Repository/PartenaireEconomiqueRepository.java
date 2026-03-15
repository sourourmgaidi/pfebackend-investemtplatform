package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.iset.investplatformpfe.Entity.PartenaireEconomique;

import java.util.Optional;

@Repository
public interface PartenaireEconomiqueRepository extends JpaRepository<PartenaireEconomique, Long> {
    boolean existsByEmail(String email);
    Optional<PartenaireEconomique> findByEmail(String email);
}
