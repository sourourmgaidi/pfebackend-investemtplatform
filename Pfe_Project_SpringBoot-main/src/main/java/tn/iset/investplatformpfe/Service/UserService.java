package tn.iset.investplatformpfe.Service;

import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Repository.AdminRepository;
import tn.iset.investplatformpfe.Repository.InvestorRepository;
import tn.iset.investplatformpfe.Repository.PartenaireEconomiqueRepository;
import tn.iset.investplatformpfe.Repository.TouristeRepository;
import tn.iset.investplatformpfe.Repository.InternationalCompanyRepository;

@Service
public class UserService {

    private final AdminRepository adminRepository;
    private final PartenaireLocalRepository partenaireLocalRepository;
    private final PartenaireEconomiqueRepository partenaireEconomiqueRepository;
    private final InternationalCompanyRepository internationalCompanyRepository;
    private final InvestorRepository investorRepository;
    private final TouristeRepository touristeRepository;

    public UserService(AdminRepository adminRepository,
                       PartenaireLocalRepository partenaireLocalRepository,
                       PartenaireEconomiqueRepository partenaireEconomiqueRepository,
                       InternationalCompanyRepository internationalCompanyRepository,
                       InvestorRepository investorRepository,
                       TouristeRepository touristeRepository) {
        this.adminRepository = adminRepository;
        this.partenaireLocalRepository = partenaireLocalRepository;
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
                return partenaireLocalRepository.findByEmail(email)
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
}
