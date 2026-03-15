package tn.iset.investplatformpfe.Service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PartenaireLocalService {

    @Autowired
    private PartenaireLocalRepository repository;

    public String validateAndSave(PartenaireLocal partenaire) {
        // Champs obligatoires
        if (partenaire.getNom() == null || partenaire.getNom().isBlank()) return "Nom obligatoire";
        if (partenaire.getPrenom() == null || partenaire.getPrenom().isBlank()) return "Prénom obligatoire";
        if (partenaire.getEmail() == null || partenaire.getEmail().isBlank()) return "Email obligatoire";
        if (partenaire.getMotDePasse() == null || partenaire.getMotDePasse().isBlank()) return "Mot de passe obligatoire";

        // Vérifier email contient @
        if (!partenaire.getEmail().contains("@")) return "Email invalide";

        // Email unique
        Optional<PartenaireLocal> existing = repository.findByEmail(partenaire.getEmail());
        if (existing.isPresent() && (partenaire.getId() == null || !existing.get().getId().equals(partenaire.getId()))) {
            return "Email déjà utilisé";
        }

        // Mot de passe minimum 6 caractères
        if (partenaire.getMotDePasse().length() < 6) return "Mot de passe doit contenir au moins 6 caractères";

        repository.save(partenaire);
        return "OK";
    }

    public List<PartenaireLocal> getAllPartenaires() {
        return repository.findAll();
    }

    public Optional<PartenaireLocal> getPartenaireById(Long id) {
        return repository.findById(id);
    }

    public boolean deletePartenaire(Long id) {
        Optional<PartenaireLocal> partenaire = repository.findById(id);
        if (partenaire.isPresent()) {
            repository.deleteById(id);
            return true;
        } else return false;
    }
}

