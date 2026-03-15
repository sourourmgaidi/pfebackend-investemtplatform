package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.iset.investplatformpfe.Entity.Touriste;

import java.util.Optional;

@Repository
public interface TouristeRepository extends JpaRepository<Touriste, Long> {
    boolean existsByEmail(String email);
    Optional<Touriste> findByEmail(String email);
}
