package tn.iset.investplatformpfe.Service;

import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Repository.AdminRepository;
import tn.iset.investplatformpfe.Repository.InvestorRepository;
import tn.iset.investplatformpfe.Repository.EconomicPartnerRepository;
import tn.iset.investplatformpfe.Repository.TouristRepository;
import tn.iset.investplatformpfe.Repository.InternationalCompanyRepository;
import tn.iset.investplatformpfe.Repository.LocalPartnerRepository;  // Import corrigé

@Service
public class UserService {

    private final AdminRepository adminRepository;
    private final LocalPartnerRepository localPartnerRepository;  // Changé de PartenaireLocalRepository
    private final EconomicPartnerRepository partenaireEconomiqueRepository;
    private final InternationalCompanyRepository internationalCompanyRepository;
    private final InvestorRepository investorRepository;
    private final TouristRepository touristeRepository;

    public UserService(AdminRepository adminRepository,
                       LocalPartnerRepository localPartnerRepository,  // Changé le type du paramètre
                       EconomicPartnerRepository partenaireEconomiqueRepository,
                       InternationalCompanyRepository internationalCompanyRepository,
                       InvestorRepository investorRepository,
                       TouristRepository touristeRepository) {
        this.adminRepository = adminRepository;
        this.localPartnerRepository = localPartnerRepository;  // Changé le nom du champ
        this.partenaireEconomiqueRepository = partenaireEconomiqueRepository;
        this.internationalCompanyRepository = internationalCompanyRepository;
        this.investorRepository = investorRepository;
        this.touristeRepository = touristeRepository;
    }

    public Long getUserIdByEmailAndRole(String email, Role role) {
        switch (role) {
            case ADMIN:
                return adminRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Admin not found"))
                        .getId();
            case LOCAL_PARTNER:
                return localPartnerRepository.findByEmail(email)  // Changé partenaireLocalRepository à localPartnerRepository
                        .orElseThrow(() -> new RuntimeException("Local partner not found"))
                        .getId();
            case PARTNER:
                return partenaireEconomiqueRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Economic partner not found"))
                        .getId();
            case INTERNATIONAL_COMPANY:
                return internationalCompanyRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("International company not found"))
                        .getId();
            case INVESTOR:
                return investorRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Investor not found"))
                        .getId();
            case TOURIST:
                return touristeRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("Tourist not found"))
                        .getId();
            default:
                throw new RuntimeException("Unknown role: " + role);
        }
    }


    // ========================================
// AJOUTER CES MÉTHODES À LA FIN DE LA CLASSE
// ========================================

    /**
     * Récupérer l'email d'un utilisateur par son ID et son rôle
     */
    public String getUserEmailByIdAndRole(Long userId, Role role) {
        switch (role) {
            case ADMIN:
                return adminRepository.findById(userId)
                        .map(admin -> admin.getEmail())
                        .orElseThrow(() -> new RuntimeException("Admin non trouvé avec ID: " + userId));

            case LOCAL_PARTNER:
                return localPartnerRepository.findById(userId)
                        .map(partner -> partner.getEmail())
                        .orElseThrow(() -> new RuntimeException("Partenaire local non trouvé avec ID: " + userId));

            case PARTNER:
                return partenaireEconomiqueRepository.findById(userId)
                        .map(partner -> partner.getEmail())
                        .orElseThrow(() -> new RuntimeException("Partenaire économique non trouvé avec ID: " + userId));

            case INTERNATIONAL_COMPANY:
                return internationalCompanyRepository.findById(userId)
                        .map(company -> company.getEmail())
                        .orElseThrow(() -> new RuntimeException("Société internationale non trouvée avec ID: " + userId));

            case INVESTOR:
                return investorRepository.findById(userId)
                        .map(investor -> investor.getEmail())
                        .orElseThrow(() -> new RuntimeException("Investisseur non trouvé avec ID: " + userId));

            case TOURIST:
                return touristeRepository.findById(userId)
                        .map(tourist -> tourist.getEmail())
                        .orElseThrow(() -> new RuntimeException("Touriste non trouvé avec ID: " + userId));

            default:
                throw new RuntimeException("Rôle non supporté: " + role);
        }
    }

    /**
     * Récupérer le nom complet d'un utilisateur par son email et son rôle
     */
    public String getUserFullName(String email, Role role) {
        switch (role) {
            case ADMIN:
                return adminRepository.findByEmail(email)
                        .map(admin -> admin.getFirstName() + " " + admin.getLastName())
                        .orElse("Admin");

            case LOCAL_PARTNER:
                return localPartnerRepository.findByEmail(email)
                        .map(partner -> partner.getFirstName() + " " + partner.getLastName())
                        .orElse("Partenaire Local");

            case PARTNER:
                return partenaireEconomiqueRepository.findByEmail(email)
                        .map(partner -> partner.getFirstName() + " " + partner.getLastName())
                        .orElse("Partenaire Économique");

            case INTERNATIONAL_COMPANY:
                return internationalCompanyRepository.findByEmail(email)
                        .map(company -> company.getContactFirstName() + " " + company.getContactLastName())
                        .orElse("Société Internationale");

            case INVESTOR:
                return investorRepository.findByEmail(email)
                        .map(investor -> investor.getFirstName() + " " + investor.getLastName())
                        .orElse("Investisseur");

            case TOURIST:
                return touristeRepository.findByEmail(email)
                        .map(tourist -> tourist.getFirstName() + " " + tourist.getLastName())
                        .orElse("Touriste");

            default:
                return "Utilisateur";
        }
    }

    /**
     * Récupérer le nom complet d'un utilisateur par son ID et son rôle (méthode utilitaire)
     */
    public String getUserFullNameById(Long userId, Role role) {
        String email = getUserEmailByIdAndRole(userId, role);
        return getUserFullName(email, role);
    }
}