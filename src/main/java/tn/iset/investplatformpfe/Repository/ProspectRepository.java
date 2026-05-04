package tn.iset.investplatformpfe.Repository;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.iset.investplatformpfe.Entity.Prospect;

import java.util.List;

public interface ProspectRepository extends JpaRepository<Prospect, Long> {
    List<Prospect> findByStatus(String status);
}
