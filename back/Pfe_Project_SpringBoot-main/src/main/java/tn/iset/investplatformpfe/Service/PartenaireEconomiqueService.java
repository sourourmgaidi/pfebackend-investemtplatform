package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Entity.PartenaireEconomique;
import tn.iset.investplatformpfe.Repository.PartenaireEconomiqueRepository;

import java.util.List;
import java.util.Optional;

@Service
public class PartenaireEconomiqueService {

    @Autowired
    private PartenaireEconomiqueRepository repository;

    public String validateAndSave(PartenaireEconomique partenaire) {
        // Vérifier champs obligatoires
        if (partenaire.getNom() == null || partenaire.getNom().isBlank()) return "Nom obligatoire";
        if (partenaire.getPrenom() == null || partenaire.getPrenom().isBlank()) return "Prénom obligatoire";
        if (partenaire.getEmail() == null || partenaire.getEmail().isBlank()) return "Email obligatoire";
        if (partenaire.getMotDePasse() == null || partenaire.getMotDePasse().isBlank()) return "Mot de passe obligatoire";

        // Vérifier mot de passe minimal
        if (partenaire.getMotDePasse().length() < 6) return "Mot de passe doit contenir au moins 6 caractères";

        // Vérifier email contient @
        if (!partenaire.getEmail().contains("@")) return "Email invalide";

        // Vérifier si email existe déjà
        Optional<PartenaireEconomique> existing = repository.findByEmail(partenaire.getEmail());
        if (existing.isPresent() && (partenaire.getId() == null || !existing.get().getId().equals(partenaire.getId()))) {
            return "Email déjà utilisé";
        }

        // Tout est bon, enregistrer
        repository.save(partenaire);
        return "OK";
    }

    // READ all
    public List<PartenaireEconomique> getAllPartenaires() {
        return repository.findAll();
    }

    // READ by ID
    public Optional<PartenaireEconomique> getPartenaireById(Long id) {
        return repository.findById(id);
    }

    // DELETE
    public boolean deletePartenaire(Long id) {
        Optional<PartenaireEconomique> partenaire = repository.findById(id);
        if (partenaire.isPresent()) {
            repository.deleteById(id);
            return true;
        } else return false;
    }
}
