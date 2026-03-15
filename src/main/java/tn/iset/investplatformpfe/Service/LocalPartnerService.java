package tn.iset.investplatformpfe.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Entity.LocalPartner;
import tn.iset.investplatformpfe.Repository.LocalPartnerRepository;

import java.util.List;
import java.util.Optional;

@Service
public class LocalPartnerService {

    @Autowired
    private LocalPartnerRepository repository;

    public String validateAndSave(LocalPartner partner) {
        // Required fields
        if (partner.getLastName() == null || partner.getLastName().isBlank()) return "Last name is required";
        if (partner.getFirstName() == null || partner.getFirstName().isBlank()) return "First name is required";
        if (partner.getEmail() == null || partner.getEmail().isBlank()) return "Email is required";
        if (partner.getPassword() == null || partner.getPassword().isBlank()) return "Password is required";

        // Check if email contains @
        if (!partner.getEmail().contains("@")) return "Invalid email format";

        // Unique email
        Optional<LocalPartner> existing = repository.findByEmail(partner.getEmail());
        if (existing.isPresent() && (partner.getId() == null || !existing.get().getId().equals(partner.getId()))) {
            return "Email already in use";
        }

        // Password minimum 6 characters
        if (partner.getPassword().length() < 6) return "Password must contain at least 6 characters";

        repository.save(partner);
        return "OK";
    }

    public List<LocalPartner> getAllPartners() {
        return repository.findAll();
    }

    public Optional<LocalPartner> getPartnerById(Long id) {
        return repository.findById(id);
    }

    public boolean deletePartner(Long id) {
        Optional<LocalPartner> partner = repository.findById(id);
        if (partner.isPresent()) {
            repository.deleteById(id);
            return true;
        } else return false;
    }
}
