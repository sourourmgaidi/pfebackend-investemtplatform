package tn.iset.investplatformpfe.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.investplatformpfe.Entity.Investor;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Repository.InvestorRepository;
import tn.iset.investplatformpfe.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InvestorService {
    private final InvestorRepository investorRepository;

    public InvestorService(InvestorRepository investorRepository) {
        this.investorRepository = investorRepository;
    }

    @Transactional
    public Investor createInvestor(Investor investor) {

        // Validate required fields
        if (investor.getLastName() == null || investor.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }

        if (investor.getFirstName() == null || investor.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }

        if (investor.getEmail() == null || investor.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        // ✅ VALIDATION EMAIL avec @
        if (!investor.getEmail().contains("@")) {
            throw new IllegalArgumentException("Email must contain '@' symbol");
        }

        // Validation email format complet (optionnel mais recommandé)
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (!investor.getEmail().matches(emailRegex)) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Check if email already exists
        if (investorRepository.existsByEmail(investor.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + investor.getEmail());
        }

        if (investor.getPassword() == null || investor.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        // Set default values
        if (investor.getActive() == null) {
            investor.setActive(true);
        }

        if (investor.getRole() == null) {
            investor.setRole(Role.INVESTOR);
        }

        if (investor.getRegistrationDate() == null) {
            investor.setRegistrationDate(LocalDateTime.now());
        }

        // TODO: Hash password with BCrypt
        // investor.setPassword(passwordEncoder.encode(investor.getPassword()));

        // Save to database and return
        return investorRepository.save(investor);
    }

    // Update
    // ========================================
// UPDATE (Full Update)
// ========================================

    @Transactional
    public Investor updateInvestor(Long id, Investor investorDetails) {

        // 1. Trouver l'investisseur existant
        Investor investor = investorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investor", "id", id));

        // 2. Valider les champs obligatoires
        if (investorDetails.getLastName() == null || investorDetails.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required");
        }

        if (investorDetails.getFirstName() == null || investorDetails.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }

        if (investorDetails.getEmail() == null || investorDetails.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        // 3. Valider le format de l'email
        if (!investorDetails.getEmail().contains("@")) {
            throw new IllegalArgumentException("Email must contain '@' symbol");
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (!investorDetails.getEmail().matches(emailRegex)) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // 4. Vérifier si l'email a changé et s'il existe déjà
        if (!investor.getEmail().equals(investorDetails.getEmail()) &&
                investorRepository.existsByEmail(investorDetails.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + investorDetails.getEmail());
        }

        // 5. Mettre à jour tous les champs
        investor.setLastName(investorDetails.getLastName());
        investor.setFirstName(investorDetails.getFirstName());
        investor.setEmail(investorDetails.getEmail());
        investor.setPhone(investorDetails.getPhone());
        investor.setProfilePicture(investorDetails.getProfilePicture());
        investor.setActive(investorDetails.getActive());
        investor.setCompany(investorDetails.getCompany());
        investor.setOriginCountry(investorDetails.getOriginCountry());
        investor.setActivitySector(investorDetails.getActivitySector());
        investor.setWebsite(investorDetails.getWebsite());
        investor.setLinkedinProfile(investorDetails.getLinkedinProfile());

        // 6. Mettre à jour le mot de passe uniquement s'il est fourni
        if (investorDetails.getPassword() != null && !investorDetails.getPassword().isEmpty()) {
            // TODO: Hash password with BCrypt
            investor.setPassword(investorDetails.getPassword());
        }

        // 7. Sauvegarder et retourner
        return investorRepository.save(investor);
    }

    //Delete
    @Transactional
    public void deleteInvestor(Long id) {

        // 1. Vérifier si l'investisseur existe
        Investor investor = investorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investor", "id", id));

        // 2. Supprimer de la base de données
        investorRepository.delete(investor);
    }

    @Transactional(readOnly = true)
    public Investor getInvestorById(Long id) {

        // Chercher l'investisseur par ID
        // Si non trouvé, lance une exception ResourceNotFoundException
        return investorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investor", "id", id));
    }
    @Transactional(readOnly = true)
    public List<Investor> getAllInvestors() {

        // Récupère tous les investisseurs de la base de données
        return investorRepository.findAll();
    }
}
