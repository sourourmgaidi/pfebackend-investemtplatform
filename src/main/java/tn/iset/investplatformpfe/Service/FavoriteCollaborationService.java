package tn.iset.investplatformpfe.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.investplatformpfe.Entity.CollaborationService;
import tn.iset.investplatformpfe.Entity.EconomicPartner;
import tn.iset.investplatformpfe.Entity.internationalcompany;
import tn.iset.investplatformpfe.Repository.CollaborationServiceRepository;
import tn.iset.investplatformpfe.Repository.InternationalCompanyRepository;
import tn.iset.investplatformpfe.Repository.EconomicPartnerRepository;

import java.util.List;

@Service
public class FavoriteCollaborationService {

    private final InternationalCompanyRepository internationalCompanyRepository;
    private final EconomicPartnerRepository economicPartnerRepository;
    private final CollaborationServiceRepository collaborationServiceRepository;

    public FavoriteCollaborationService(
            InternationalCompanyRepository internationalCompanyRepository,
            EconomicPartnerRepository economicPartnerRepository,
            CollaborationServiceRepository collaborationServiceRepository) {
        this.internationalCompanyRepository = internationalCompanyRepository;
        this.economicPartnerRepository = economicPartnerRepository;
        this.collaborationServiceRepository = collaborationServiceRepository;
    }

    // ========================================
    // POUR INTERNATIONAL COMPANY
    // ========================================

    @Transactional
    public CollaborationService addCompanyFavorite(Long companyId, Long serviceId) {
        internationalcompany company = internationalCompanyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Société internationale non trouvée avec l'ID: " + companyId));

        CollaborationService service = collaborationServiceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service de collaboration non trouvé avec l'ID: " + serviceId));

        // Vérifier que le service est approuvé
        if (service.getStatus() != tn.iset.investplatformpfe.Entity.ServiceStatus.APPROVED) {
            throw new RuntimeException("Seuls les services approuvés peuvent être ajoutés aux favoris");
        }

        // Initialiser la liste si elle est null
        if (company.getFavoriteCollaborationServices() == null) {
            company.setFavoriteCollaborationServices(new java.util.ArrayList<>());
        }

        // Vérifier si le service est déjà en favori
        boolean alreadyFavorite = company.getFavoriteCollaborationServices().stream()
                .anyMatch(s -> s.getId().equals(serviceId));

        if (!alreadyFavorite) {
            company.getFavoriteCollaborationServices().add(service);
            internationalCompanyRepository.save(company);
            System.out.println("✅ Service de collaboration " + serviceId + " ajouté aux favoris de la société internationale " + companyId);
        } else {
            System.out.println("ℹ️ Service déjà en favori");
        }

        return service;
    }

    @Transactional
    public void removeCompanyFavorite(Long companyId, Long serviceId) {
        internationalcompany company = internationalCompanyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Société internationale non trouvée avec l'ID: " + companyId));

        if (company.getFavoriteCollaborationServices() != null) {
            boolean removed = company.getFavoriteCollaborationServices().removeIf(s -> s.getId().equals(serviceId));
            if (removed) {
                internationalCompanyRepository.save(company);
                System.out.println("✅ Service de collaboration " + serviceId + " retiré des favoris de la société internationale " + companyId);
            }
        }
    }

    public List<CollaborationService> getCompanyFavorites(Long companyId) {
        internationalcompany company = internationalCompanyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Société internationale non trouvée avec l'ID: " + companyId));

        return company.getFavoriteCollaborationServices() != null ?
                company.getFavoriteCollaborationServices() :
                List.of();
    }

    public boolean isCompanyFavorite(Long companyId, Long serviceId) {
        internationalcompany company = internationalCompanyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Société internationale non trouvée avec l'ID: " + companyId));

        return company.getFavoriteCollaborationServices() != null &&
                company.getFavoriteCollaborationServices().stream()
                        .anyMatch(s -> s.getId().equals(serviceId));
    }

    public int countCompanyFavorites(Long companyId) {
        internationalcompany company = internationalCompanyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Société internationale non trouvée avec l'ID: " + companyId));

        return company.getFavoriteCollaborationServices() != null ?
                company.getFavoriteCollaborationServices().size() : 0;
    }

    // ========================================
    // POUR ECONOMIC PARTNER
    // ========================================

    @Transactional
    public CollaborationService addPartnerFavorite(Long partnerId, Long serviceId) {
        EconomicPartner partner = economicPartnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partenaire économique non trouvé avec l'ID: " + partnerId));

        CollaborationService service = collaborationServiceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service de collaboration non trouvé avec l'ID: " + serviceId));

        // Vérifier que le service est approuvé
        if (service.getStatus() != tn.iset.investplatformpfe.Entity.ServiceStatus.APPROVED) {
            throw new RuntimeException("Seuls les services approuvés peuvent être ajoutés aux favoris");
        }

        // Initialiser la liste si elle est null
        if (partner.getFavoriteCollaborationServices() == null) {
            partner.setFavoriteCollaborationServices(new java.util.ArrayList<>());
        }

        // Vérifier si le service est déjà en favori
        boolean alreadyFavorite = partner.getFavoriteCollaborationServices().stream()
                .anyMatch(s -> s.getId().equals(serviceId));

        if (!alreadyFavorite) {
            partner.getFavoriteCollaborationServices().add(service);
            economicPartnerRepository.save(partner);
            System.out.println("✅ Service de collaboration " + serviceId + " ajouté aux favoris du partenaire économique " + partnerId);
        } else {
            System.out.println("ℹ️ Service déjà en favori");
        }

        return service;
    }

    @Transactional
    public void removePartnerFavorite(Long partnerId, Long serviceId) {
        EconomicPartner partner = economicPartnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partenaire économique non trouvé avec l'ID: " + partnerId));

        if (partner.getFavoriteCollaborationServices() != null) {
            boolean removed = partner.getFavoriteCollaborationServices().removeIf(s -> s.getId().equals(serviceId));
            if (removed) {
                economicPartnerRepository.save(partner);
                System.out.println("✅ Service de collaboration " + serviceId + " retiré des favoris du partenaire économique " + partnerId);
            }
        }
    }

    public List<CollaborationService> getPartnerFavorites(Long partnerId) {
        EconomicPartner partner = economicPartnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partenaire économique non trouvé avec l'ID: " + partnerId));

        return partner.getFavoriteCollaborationServices() != null ?
                partner.getFavoriteCollaborationServices() :
                List.of();
    }

    public boolean isPartnerFavorite(Long partnerId, Long serviceId) {
        EconomicPartner partner = economicPartnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partenaire économique non trouvé avec l'ID: " + partnerId));

        return partner.getFavoriteCollaborationServices() != null &&
                partner.getFavoriteCollaborationServices().stream()
                        .anyMatch(s -> s.getId().equals(serviceId));
    }

    public int countPartnerFavorites(Long partnerId) {
        EconomicPartner partner = economicPartnerRepository.findById(partnerId)
                .orElseThrow(() -> new RuntimeException("Partenaire économique non trouvé avec l'ID: " + partnerId));

        return partner.getFavoriteCollaborationServices() != null ?
                partner.getFavoriteCollaborationServices().size() : 0;
    }
    // ========================================
// ✅ NOUVELLE MÉTHODE: Retirer un service de collaboration de TOUS les favoris
// ========================================
    @Transactional
    public void removeServiceFromAllFavorites(Long serviceId) {
        System.out.println("🗑️ Suppression du service de collaboration " + serviceId + " de tous les favoris");

        // 1. Retirer des favoris de toutes les sociétés internationales
        List<internationalcompany> allCompanies = internationalCompanyRepository.findAll();
        int companyCount = 0;

        for (internationalcompany company : allCompanies) {
            if (company.getFavoriteCollaborationServices() != null) {
                boolean removed = company.getFavoriteCollaborationServices().removeIf(s -> s.getId().equals(serviceId));
                if (removed) {
                    internationalCompanyRepository.save(company);
                    companyCount++;
                }
            }
        }
        System.out.println("✅ Retiré des favoris de " + companyCount + " sociétés internationales");

        // 2. Retirer des favoris de tous les partenaires économiques
        List<EconomicPartner> allPartners = economicPartnerRepository.findAll();
        int partnerCount = 0;

        for (EconomicPartner partner : allPartners) {
            if (partner.getFavoriteCollaborationServices() != null) {
                boolean removed = partner.getFavoriteCollaborationServices().removeIf(s -> s.getId().equals(serviceId));
                if (removed) {
                    economicPartnerRepository.save(partner);
                    partnerCount++;
                }
            }
        }
        System.out.println("✅ Retiré des favoris de " + partnerCount + " partenaires économiques");

        System.out.println("✅ Service de collaboration " + serviceId + " retiré de tous les favoris avec succès");
    }


}
