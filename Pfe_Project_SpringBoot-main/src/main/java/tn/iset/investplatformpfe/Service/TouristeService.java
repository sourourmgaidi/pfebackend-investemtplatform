package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Entity.Touriste;
import tn.iset.investplatformpfe.Repository.TouristeRepository;

import java.util.List;
import java.util.Optional;

@Service
public class TouristeService {

    @Autowired
    private TouristeRepository repository;

    // CREATE ou UPDATE avec validation
    public String validateAndSave(Touriste touriste) {
        if (touriste.getNom() == null || touriste.getNom().isBlank()) return "Nom obligatoire";
        if (touriste.getPrenom() == null || touriste.getPrenom().isBlank()) return "Prénom obligatoire";
        if (touriste.getEmail() == null || touriste.getEmail().isBlank()) return "Email obligatoire";
        if (!touriste.getEmail().contains("@")) return "Email invalide";
        if (touriste.getMotDePasse() == null || touriste.getMotDePasse().isBlank()) return "Mot de passe obligatoire";
        if (touriste.getMotDePasse().length() < 6) return "Mot de passe doit contenir au moins 6 caractères";

        Optional<Touriste> existing = repository.findByEmail(touriste.getEmail());
        if (existing.isPresent() && (touriste.getId() == null || !existing.get().getId().equals(touriste.getId()))) {
            return "Email déjà utilisé";
        }

        repository.save(touriste);
        return "OK";
    }

    // READ ALL
    public List<Touriste> getAllTouristes() {
        return repository.findAll();
    }

    // READ BY ID
    public Optional<Touriste> getTouristeById(Long id) {
        return repository.findById(id);
    }

    // DELETE
    public boolean deleteTouriste(Long id) {
        Optional<Touriste> t = repository.findById(id);
        if (t.isPresent()) {
            repository.deleteById(id);
            return true;
        } else return false;
    }
}
