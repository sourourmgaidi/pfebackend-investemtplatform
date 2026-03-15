package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.iset.investplatformpfe.Entity.Investor;
import tn.iset.investplatformpfe.Entity.Role;

import java.util.List;
import java.util.Optional;
@Repository
public interface InvestorRepository extends JpaRepository<Investor, Long> {
    Optional<Investor> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Investor> findByActive(Boolean active);

    List<Investor> findByRole(Role role);
}
