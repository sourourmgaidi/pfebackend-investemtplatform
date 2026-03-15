package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.iset.investplatformpfe.Entity.EconomicSector;

import java.util.List;
import java.util.Optional;

@Repository
public interface EconomicSectorRepository extends JpaRepository<EconomicSector, Long> {

    Optional<EconomicSector> findByName(String name);

    boolean existsByName(String name);

    List<EconomicSector> findByNameContainingIgnoreCase(String keyword);
}