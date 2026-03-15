package tn.iset.investplatformpfe.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.investplatformpfe.Entity.InvestmentService;
import tn.iset.investplatformpfe.Entity.Investor;
import tn.iset.investplatformpfe.Entity.internationalcompany;
import tn.iset.investplatformpfe.Repository.InvestmentServiceRepository;
import tn.iset.investplatformpfe.Repository.InvestorRepository;
import tn.iset.investplatformpfe.Repository.InternationalCompanyRepository;

import java.util.List;

@Service
public class FavoriteInvestService {

    private final InvestorRepository investorRepository;
    private final InternationalCompanyRepository internationalCompanyRepository;
    private final InvestmentServiceRepository investmentServiceRepository;

    public FavoriteInvestService(
            InvestorRepository investorRepository,
            InternationalCompanyRepository internationalCompanyRepository,
            InvestmentServiceRepository investmentServiceRepository) {
        this.investorRepository = investorRepository;
        this.internationalCompanyRepository = internationalCompanyRepository;
        this.investmentServiceRepository = investmentServiceRepository;
    }

    // ========================================
    // POUR INVESTOR
    // ========================================

    @Transactional
    public InvestmentService addInvestorFavorite(Long investorId, Long serviceId) {
        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new RuntimeException("Investisseur non trouvé avec l'ID: " + investorId));

        InvestmentService service = investmentServiceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service non trouvé avec l'ID: " + serviceId));

        // Initialiser la liste si elle est null
        if (investor.getFavoriteServices() == null) {
            investor.setFavoriteServices(new java.util.ArrayList<>());
        }

        // Vérifier si le service est déjà en favori
        boolean alreadyFavorite = investor.getFavoriteServices().stream()
                .anyMatch(s -> s.getId().equals(serviceId));

        if (!alreadyFavorite) {
            investor.getFavoriteServices().add(service);
            investorRepository.save(investor);
            System.out.println("✅ Service " + serviceId + " ajouté aux favoris de l'investisseur " + investorId);
        } else {
            System.out.println("ℹ️ Service déjà en favori");
        }

        return service;
    }

    @Transactional
    public void removeInvestorFavorite(Long investorId, Long serviceId) {
        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new RuntimeException("Investisseur non trouvé avec l'ID: " + investorId));

        if (investor.getFavoriteServices() != null) {
            boolean removed = investor.getFavoriteServices().removeIf(s -> s.getId().equals(serviceId));
            if (removed) {
                investorRepository.save(investor);
                System.out.println("✅ Service " + serviceId + " retiré des favoris de l'investisseur " + investorId);
            }
        }
    }

    public List<InvestmentService> getInvestorFavorites(Long investorId) {
        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new RuntimeException("Investisseur non trouvé avec l'ID: " + investorId));

        return investor.getFavoriteServices() != null ?
                investor.getFavoriteServices() :
                List.of();
    }

    public boolean isInvestorFavorite(Long investorId, Long serviceId) {
        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new RuntimeException("Investisseur non trouvé avec l'ID: " + investorId));

        return investor.getFavoriteServices() != null &&
                investor.getFavoriteServices().stream()
                        .anyMatch(s -> s.getId().equals(serviceId));
    }

    public int countInvestorFavorites(Long investorId) {
        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new RuntimeException("Investisseur non trouvé avec l'ID: " + investorId));

        return investor.getFavoriteServices() != null ?
                investor.getFavoriteServices().size() : 0;
    }

    // ========================================
    // POUR INTERNATIONAL COMPANY
    // ========================================

    @Transactional
    public InvestmentService addCompanyFavorite(Long companyId, Long serviceId) {
        internationalcompany company = internationalCompanyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Société non trouvée avec l'ID: " + companyId));

        InvestmentService service = investmentServiceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service non trouvé avec l'ID: " + serviceId));

        // Initialiser la liste si elle est null
        if (company.getFavoriteServices() == null) {
            company.setFavoriteServices(new java.util.ArrayList<>());
        }

        // Vérifier si le service est déjà en favori
        boolean alreadyFavorite = company.getFavoriteServices().stream()
                .anyMatch(s -> s.getId().equals(serviceId));

        if (!alreadyFavorite) {
            company.getFavoriteServices().add(service);
            internationalCompanyRepository.save(company);
            System.out.println("✅ Service " + serviceId + " ajouté aux favoris de la société " + companyId);
        } else {
            System.out.println("ℹ️ Service déjà en favori");
        }

        return service;
    }

    @Transactional
    public void removeCompanyFavorite(Long companyId, Long serviceId) {
        internationalcompany company = internationalCompanyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Société non trouvée avec l'ID: " + companyId));

        if (company.getFavoriteServices() != null) {
            boolean removed = company.getFavoriteServices().removeIf(s -> s.getId().equals(serviceId));
            if (removed) {
                internationalCompanyRepository.save(company);
                System.out.println("✅ Service " + serviceId + " retiré des favoris de la société " + companyId);
            }
        }
    }

    public List<InvestmentService> getCompanyFavorites(Long companyId) {
        internationalcompany company = internationalCompanyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Société non trouvée avec l'ID: " + companyId));

        return company.getFavoriteServices() != null ?
                company.getFavoriteServices() :
                List.of();
    }

    public boolean isCompanyFavorite(Long companyId, Long serviceId) {
        internationalcompany company = internationalCompanyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Société non trouvée avec l'ID: " + companyId));

        return company.getFavoriteServices() != null &&
                company.getFavoriteServices().stream()
                        .anyMatch(s -> s.getId().equals(serviceId));
    }

    public int countCompanyFavorites(Long companyId) {
        internationalcompany company = internationalCompanyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Société non trouvée avec l'ID: " + companyId));

        return company.getFavoriteServices() != null ?
                company.getFavoriteServices().size() : 0;
    }

    // ========================================
// ✅ NOUVELLE MÉTHODE: Retirer un service de TOUS les favoris
// ========================================
    @Transactional
    public void removeServiceFromAllFavorites(Long serviceId) {
        System.out.println("🗑️ Suppression du service " + serviceId + " de tous les favoris");

        // 1. Retirer des favoris de tous les investisseurs
        List<Investor> allInvestors = investorRepository.findAll();
        int investorCount = 0;

        for (Investor investor : allInvestors) {
            if (investor.getFavoriteServices() != null) {
                boolean removed = investor.getFavoriteServices().removeIf(s -> s.getId().equals(serviceId));
                if (removed) {
                    investorRepository.save(investor);
                    investorCount++;
                }
            }
        }
        System.out.println("✅ Retiré des favoris de " + investorCount + " investisseurs");

        // 2. Retirer des favoris de toutes les entreprises internationales
        List<internationalcompany> allCompanies = internationalCompanyRepository.findAll();
        int companyCount = 0;

        for (internationalcompany company : allCompanies) {
            if (company.getFavoriteServices() != null) {
                boolean removed = company.getFavoriteServices().removeIf(s -> s.getId().equals(serviceId));
                if (removed) {
                    internationalCompanyRepository.save(company);
                    companyCount++;
                }
            }
        }
        System.out.println("✅ Retiré des favoris de " + companyCount + " entreprises");

        System.out.println("✅ Service " + serviceId + " retiré de tous les favoris avec succès");
    }
}