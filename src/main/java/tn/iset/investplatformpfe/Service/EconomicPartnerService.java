package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Entity.EconomicPartner;  // Import corrigé
import tn.iset.investplatformpfe.Repository.EconomicPartnerRepository;

import java.util.List;
import java.util.Optional;

@Service
public class EconomicPartnerService {  // Renommé

    @Autowired
    private EconomicPartnerRepository repository;

    public String validateAndSave(EconomicPartner partner) {  // Paramètre renommé

        // Vérifier champs obligatoires - avec noms anglais
        if (partner.getLastName() == null || partner.getLastName().isBlank()) return "Last name is required";
        if (partner.getFirstName() == null || partner.getFirstName().isBlank()) return "First name is required";
        if (partner.getEmail() == null || partner.getEmail().isBlank()) return "Email is required";
        if (partner.getPassword() == null || partner.getPassword().isBlank()) return "Password is required";

        // Vérifier mot de passe minimal
        if (partner.getPassword().length() < 6) return "Password must be at least 6 characters";

        // Vérifier email contient @
        if (!partner.getEmail().contains("@")) return "Invalid email format";

        // Vérifier si email existe déjà
        Optional<EconomicPartner> existing = repository.findByEmail(partner.getEmail());
        if (existing.isPresent() && (partner.getId() == null || !existing.get().getId().equals(partner.getId()))) {
            return "Email already in use";
        }

        // Tout est bon, enregistrer
        repository.save(partner);
        return "OK";
    }

    // READ all
    public List<EconomicPartner> getAllPartners() {  // Renommé
        return repository.findAll();
    }

    // READ by ID
    public Optional<EconomicPartner> getPartnerById(Long id) {  // Renommé
        return repository.findById(id);
    }

    // DELETE
    public boolean deletePartner(Long id) {  // Renommé
        Optional<EconomicPartner> partner = repository.findById(id);
        if (partner.isPresent()) {
            repository.deleteById(id);
            return true;
        } else return false;
    }
}