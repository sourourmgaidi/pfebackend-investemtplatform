package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.iset.investplatformpfe.Entity.Tourist;

import java.util.List;
import java.util.Optional;

@Repository
public interface TouristRepository extends JpaRepository<Tourist, Long> {

    // Vérifier si un email existe déjà
    boolean existsByEmail(String email);

    // Trouver un touriste par email
    Optional<Tourist> findByEmail(String email);

    // Optionnel: trouver par nom et prénom
    // Optional<Tourist> findByLastNameAndFirstName(String lastName, String firstName);

    // Optionnel: trouver les touristes par nationalité
    // List<Tourist> findByNationality(String nationality);

    // Optionnel: trouver les touristes actifs
    // List<Tourist> findByActiveTrue();
    List<Tourist> findByFirstNameContainingOrLastNameContainingOrEmailContaining(String firstName, String lastName, String email);
}