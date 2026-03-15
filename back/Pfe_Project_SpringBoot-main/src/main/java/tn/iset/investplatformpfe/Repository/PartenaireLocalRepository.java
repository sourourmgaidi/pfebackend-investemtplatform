package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PartenaireLocalRepository extends JpaRepository<PartenaireLocal, Long> {
    boolean existsByEmail(String email);
    Optional<PartenaireLocal> findByEmail(String email);
}
