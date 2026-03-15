package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Entity.Tourist;
import tn.iset.investplatformpfe.Repository.TouristRepository;  // Import corrigé

import java.util.List;
import java.util.Optional;

@Service
public class TouristService {  // Renommé

    @Autowired
    private TouristRepository repository;  // Renommé

    // CREATE ou UPDATE avec validation
    public String validateAndSave(Tourist tourist) {  // Paramètre renommé
        
        // Validation avec les nouveaux noms anglais
        if (tourist.getLastName() == null || tourist.getLastName().isBlank()) {
            return "Last name is required";
        }
        if (tourist.getFirstName() == null || tourist.getFirstName().isBlank()) {
            return "First name is required";
        }
        if (tourist.getEmail() == null || tourist.getEmail().isBlank()) {
            return "Email is required";
        }
        if (!tourist.getEmail().contains("@")) {
            return "Invalid email format";
        }
        if (tourist.getPassword() == null || tourist.getPassword().isBlank()) {  // Changé de getMotDePasse à getPassword
            return "Password is required";
        }
        if (tourist.getPassword().length() < 6) {
            return "Password must be at least 6 characters";
        }

        // Vérifier si l'email existe déjà
        Optional<Tourist> existing = repository.findByEmail(tourist.getEmail());
        if (existing.isPresent() && (tourist.getId() == null || !existing.get().getId().equals(tourist.getId()))) {
            return "Email already in use";
        }

        repository.save(tourist);
        return "OK";
    }

    // READ ALL
    public List<Tourist> getAllTourists() {  // Renommé
        return repository.findAll();
    }

    // READ BY ID
    public Optional<Tourist> getTouristById(Long id) {  // Renommé
        return repository.findById(id);
    }

    // DELETE
    public boolean deleteTourist(Long id) {  // Renommé
        Optional<Tourist> tourist = repository.findById(id);
        if (tourist.isPresent()) {
            repository.deleteById(id);
            return true;
        } else {
            return false;
        }
    }
}