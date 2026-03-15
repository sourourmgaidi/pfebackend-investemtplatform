package tn.iset.investplatformpfe.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.ArrayList;

@Service
public class InvestmentServiceService {

    private static final Logger log = LoggerFactory.getLogger(InvestmentServiceService.class);

    private final InvestmentServiceRepository investmentRepository;
    private final InvestmentServiceDocumentRepository documentRepository;
    private final LocalPartnerRepository localPartnerRepository;
    private final InvestorRepository investorRepository;
    private final RegionRepository regionRepository;
    private final EconomicSectorRepository economicSectorRepository;
    private final NotificationService notificationService;
    private final ServiceRequestRepository serviceRequestRepository;
    private final AdminRepository adminRepository;
    private final FileStorageService fileStorageService;

    public InvestmentServiceService(
            InvestmentServiceRepository investmentRepository,
            LocalPartnerRepository localPartnerRepository,
            InvestorRepository investorRepository,
            RegionRepository regionRepository,
            EconomicSectorRepository economicSectorRepository,
            NotificationService notificationService,

            ServiceRequestRepository serviceRequestRepository,
            InvestmentServiceDocumentRepository documentRepository,
            AdminRepository adminRepository,FileStorageService fileStorageService) {
        this.investmentRepository = investmentRepository;
        this.documentRepository = documentRepository;
        this.localPartnerRepository = localPartnerRepository;
        this.investorRepository = investorRepository;
        this.regionRepository = regionRepository;
        this.economicSectorRepository = economicSectorRepository;
        this.notificationService = notificationService;
        this.serviceRequestRepository = serviceRequestRepository;
        this.adminRepository = adminRepository;
        this.fileStorageService = fileStorageService;

        log.info("✅ InvestmentServiceService initialisé");
    }

    // ========================================
    // CREATE - Par un partenaire local (avec email)
    // ========================================
    @Transactional
    public InvestmentService createInvestmentService(InvestmentService service, String partnerEmail) {
        log.info("📝 Création service d'investissement par email: {}", partnerEmail);

        LocalPartner partner = localPartnerRepository.findByEmail(partnerEmail)
                .orElseThrow(() -> {
                    log.error("❌ Partenaire local non trouvé avec email: {}", partnerEmail);
                    return new RuntimeException("Partenaire local non trouvé avec email: " + partnerEmail);
                });

        log.info("✅ Partenaire trouvé: {} {}", partner.getFirstName(), partner.getLastName());
        return createInvestmentServiceWithProvider(service, partner.getId());
    }

    // ========================================
    // CREATE - Avec ID du fournisseur
    // ========================================
    @Transactional
    public InvestmentService createInvestmentServiceWithProvider(InvestmentService service, Long providerId) {

        log.info("🔵 ===== DÉBUT CRÉATION SERVICE D'INVESTISSEMENT =====");
        log.info("🔵 Provider ID reçu: {}", providerId);

        LocalPartner provider = localPartnerRepository.findById(providerId)
                .orElseThrow(() -> {
                    log.error("❌ Provider non trouvé avec id: {}", providerId);
                    return new RuntimeException("Provider non trouvé avec id: " + providerId);
                });

        log.info("✅ Provider trouvé: {} - {}", provider.getEmail(), provider.getFirstName());

        InvestmentService newService = new InvestmentService();
        newService.setName(service.getName());
        newService.setDescription(service.getDescription());
        log.info("📋 Service: {} - {}", service.getName(), service.getDescription());

        if (service.getRegion() != null && service.getRegion().getId() != null) {
            Region region = regionRepository.findById(service.getRegion().getId())
                    .orElseThrow(() -> {
                        log.error("❌ Région non trouvée avec id: {}", service.getRegion().getId());
                        return new RuntimeException("Région non trouvée avec id: " + service.getRegion().getId());
                    });
            newService.setRegion(region);
            log.info("📍 Région: {}", region.getName());
        }

        // 🗑️ SUPPRIMÉ : newService.setPrice(service.getPrice());
        newService.setAvailability(service.getAvailability());
        newService.setContactPerson(service.getContactPerson());
        log.info("📅 Disponibilité: {}, Contact: {}", service.getAvailability(), service.getContactPerson());

        newService.setTitle(service.getTitle());
        newService.setZone(service.getZone());
        log.info("📌 Titre: {}, Zone: {}", service.getTitle(), service.getZone());

        if (service.getEconomicSector() != null && service.getEconomicSector().getId() != null) {
            EconomicSector sector = economicSectorRepository.findById(service.getEconomicSector().getId())
                    .orElseThrow(() -> {
                        log.error("❌ Secteur économique non trouvé avec id: {}", service.getEconomicSector().getId());
                        return new RuntimeException("Secteur économique non trouvé avec id: " + service.getEconomicSector().getId());
                    });
            newService.setEconomicSector(sector);
            log.info("🏭 Secteur: {}", sector.getName());
        }

        newService.setTotalAmount(service.getTotalAmount());
        newService.setMinimumAmount(service.getMinimumAmount());
        newService.setDeadlineDate(service.getDeadlineDate());
        newService.setProjectDuration(service.getProjectDuration());
        if (service.getDocuments() != null && !service.getDocuments().isEmpty()) {
            log.info("📎 Copie de {} documents", service.getDocuments().size());
            for (InvestmentServiceDocument doc : service.getDocuments()) {
                InvestmentServiceDocument newDoc = new InvestmentServiceDocument();
                newDoc.setFileName(doc.getFileName());
                newDoc.setFileType(doc.getFileType());
                newDoc.setFileSize(doc.getFileSize());
                newDoc.setFilePath(doc.getFilePath());
                newDoc.setDownloadUrl(doc.getDownloadUrl());
                newDoc.setIsPrimary(doc.getIsPrimary());
                newDoc.setInvestmentService(newService);
                newService.addDocument(newDoc);
            }
        }

        log.info("💰 Montant total: {}, Minimum: {}, Deadline: {}, Durée: {}",
                service.getTotalAmount(), service.getMinimumAmount(),
                service.getDeadlineDate(), service.getProjectDuration());

        newService.setProvider(provider);
        log.info("👤 Provider assigné: {}", provider.getEmail());

        validateRequiredFields(newService);

        try {
            InvestmentService saved = investmentRepository.save(newService);
            log.info("✅ Service d'investissement sauvegardé avec ID: {}", saved.getId());

            log.info("📢 Notification ADMIN - Nouveau service en attente");
            notificationService.notifyAdminNewInvestmentService(saved);

            log.info("🔵 ===== FIN CRÉATION SERVICE (SUCCÈS) =====\n");
            return saved;

        } catch (Exception e) {
            log.error("❌ ERREUR lors de la sauvegarde: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    // ========================================
    // READ - Tous les services
    // ========================================
    public List<InvestmentService> getAllInvestmentServices() {
        log.debug("📋 Récupération de tous les services d'investissement");
        List<InvestmentService> services = investmentRepository.findAll();
        log.debug("✅ {} services trouvés", services.size());
        return services;
    }

    // ========================================
    // READ - Par ID
    // ========================================
    public InvestmentService getInvestmentServiceById(Long id) {
        log.debug("🔍 Recherche service par ID: {}", id);
        return investmentRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("❌ Service non trouvé avec id: {}", id);
                    return new RuntimeException("Service d'investissement non trouvé avec id: " + id);
                });
    }

    // ========================================
    // READ - Par fournisseur
    // ========================================
    public List<InvestmentService> getInvestmentServicesByProviderId(Long providerId) {
        log.debug("📋 Récupération services pour provider ID: {}", providerId);
        List<InvestmentService> services = investmentRepository.findByProviderId(providerId);
        log.debug("✅ {} services trouvés", services.size());
        return services;
    }

    // ========================================
    // READ - Par région
    // ========================================
    public List<InvestmentService> getInvestmentServicesByRegionId(Long regionId) {
        log.debug("📋 Récupération services pour région ID: {}", regionId);
        List<InvestmentService> services = investmentRepository.findByRegionId(regionId);
        log.debug("✅ {} services trouvés", services.size());
        return services;
    }

    // ========================================
    // READ - Par statut
    // ========================================
    public List<InvestmentService> getInvestmentServicesByStatus(ServiceStatus status) {
        log.debug("📋 Récupération services avec statut: {}", status);
        List<InvestmentService> services = investmentRepository.findByStatus(status);
        log.debug("✅ {} services trouvés", services.size());
        return services;
    }

    // ========================================
    // READ - Services en attente
    // ========================================
    public List<InvestmentService> getPendingInvestmentServices() {
        log.debug("📋 Récupération services en attente");
        List<InvestmentService> services = investmentRepository.findByStatus(ServiceStatus.PENDING);
        log.debug("✅ {} services en attente", services.size());
        return services;
    }

    // ========================================
    // READ - Services approuvés
    // ========================================
    public List<InvestmentService> getApprovedInvestmentServices() {
        log.debug("📋 Récupération services approuvés");
        List<InvestmentService> services = investmentRepository.findByStatus(ServiceStatus.APPROVED);
        log.debug("✅ {} services approuvés", services.size());
        return services;
    }

    // ========================================
    // READ - Par zone
    // ========================================
    public List<InvestmentService> getInvestmentServicesByZone(String zone) {
        log.debug("📋 Récupération services pour zone: {}", zone);
        List<InvestmentService> services = investmentRepository.findByZone(zone);
        log.debug("✅ {} services trouvés", services.size());
        return services;
    }

    // ========================================
    // READ - Par montant maximum
    // ========================================
    public List<InvestmentService> getInvestmentServicesByMaxAmount(BigDecimal maxAmount) {
        log.debug("📋 Récupération services avec montant max: {}", maxAmount);
        List<InvestmentService> services = investmentRepository.findByMinimumAmountLessThanEqual(maxAmount);
        log.debug("✅ {} services trouvés", services.size());
        return services;
    }

    // ========================================
    // READ - Services actifs
    // ========================================
    public List<InvestmentService> getActiveInvestmentServices() {
        log.debug("📋 Récupération services actifs");
        List<InvestmentService> services = investmentRepository.findByDeadlineDateAfter(LocalDate.now());
        log.debug("✅ {} services actifs", services.size());
        return services;
    }

    // ========================================
    // UPDATE - Avec vérification d'autorisation pour les services approuvés
    // ========================================
    @Transactional
    public InvestmentService updateInvestmentService(Long id, InvestmentService serviceDetails, String partnerEmail) {
        log.info("📝 Mise à jour service ID: {} par: {}", id, partnerEmail);

        InvestmentService existingService = getInvestmentServiceById(id);
        log.info("📋 Service existant: {}", existingService.getTitle());
        log.info("📋 Statut actuel: {}", existingService.getStatus());

        LocalPartner partner = localPartnerRepository.findByEmail(partnerEmail)
                .orElseThrow(() -> {
                    log.error("❌ Partenaire non trouvé avec email: {}", partnerEmail);
                    return new RuntimeException("Partenaire non trouvé");
                });

        // Vérification que le partenaire est bien le propriétaire
        if (!existingService.getProvider().getId().equals(partner.getId())) {
            log.error("❌ Tentative de modification non autorisée par: {}", partnerEmail);
            throw new RuntimeException("Vous ne pouvez modifier que vos propres services");
        }

        // ✅ VÉRIFICATION D'AUTORISATION SELON LE STATUT
        if (existingService.getStatus() == ServiceStatus.APPROVED) {
            // Service approuvé - besoin d'autorisation
            if (!existingService.isEditAuthorized()) {
                log.warn("⚠️ Tentative de modification d'un service APPROUVÉ sans autorisation");
                throw new RuntimeException(
                        "Ce service est approuvé. Pour le modifier, vous devez faire une demande à l'admin."
                );
            }
            log.info("✅ Modification autorisée par admin jusqu'au {}",
                    existingService.getEditAuthorizedUntil().toLocalDate());
        }
        else if (existingService.getStatus() == ServiceStatus.PENDING) {
            // Service en attente - modification autorisée sans restriction
            log.info("✅ Modification de service en attente autorisée");
        }
        else if (existingService.getStatus() == ServiceStatus.REJECTED) {
            log.error("❌ Tentative de modification d'un service REJETÉ");
            throw new RuntimeException("Impossible de modifier un service rejeté");
        }
        else {
            log.error("❌ Tentative de modification d'un service avec statut inconnu: {}", existingService.getStatus());
            throw new RuntimeException("Statut de service non reconnu");
        }

        // ✅ Sauvegarder l'ancien état pour les notifications
        String oldTitle = existingService.getTitle();
        String oldDescription = existingService.getDescription();

        // Mise à jour des champs
        if (serviceDetails.getName() != null) {
            existingService.setName(serviceDetails.getName());
            log.info("📝 Nom mis à jour: {}", serviceDetails.getName());
        }
        if (serviceDetails.getDescription() != null) {
            existingService.setDescription(serviceDetails.getDescription());
            log.info("📝 Description mise à jour");
        }
        if (serviceDetails.getRegion() != null) {
            existingService.setRegion(serviceDetails.getRegion());
            log.info("📍 Région mise à jour");
        }
        // 🗑️ SUPPRIMÉ : if (serviceDetails.getPrice() != null) { ... }
        if (serviceDetails.getAvailability() != null) {
            existingService.setAvailability(serviceDetails.getAvailability());
            log.info("📅 Disponibilité mise à jour: {}", serviceDetails.getAvailability());
        }
        if (serviceDetails.getContactPerson() != null) {
            existingService.setContactPerson(serviceDetails.getContactPerson());
            log.info("👤 Contact mis à jour: {}", serviceDetails.getContactPerson());
        }
        if (serviceDetails.getTitle() != null) {
            existingService.setTitle(serviceDetails.getTitle());
            log.info("📌 Titre mis à jour: {}", serviceDetails.getTitle());
        }
        if (serviceDetails.getZone() != null) {
            existingService.setZone(serviceDetails.getZone());
            log.info("📍 Zone mise à jour: {}", serviceDetails.getZone());
        }
        if (serviceDetails.getEconomicSector() != null) {
            existingService.setEconomicSector(serviceDetails.getEconomicSector());
            log.info("🏭 Secteur économique mis à jour");
        }
        if (serviceDetails.getTotalAmount() != null) {
            existingService.setTotalAmount(serviceDetails.getTotalAmount());
            log.info("💰 Montant total mis à jour: {}", serviceDetails.getTotalAmount());
        }
        if (serviceDetails.getMinimumAmount() != null) {
            existingService.setMinimumAmount(serviceDetails.getMinimumAmount());
            log.info("💰 Montant minimum mis à jour: {}", serviceDetails.getMinimumAmount());
        }
        if (serviceDetails.getDeadlineDate() != null) {
            existingService.setDeadlineDate(serviceDetails.getDeadlineDate());
            log.info("📅 Deadline mise à jour: {}", serviceDetails.getDeadlineDate());
        }
        if (serviceDetails.getProjectDuration() != null) {
            existingService.setProjectDuration(serviceDetails.getProjectDuration());
            log.info("⏱️ Durée projet mise à jour: {}", serviceDetails.getProjectDuration());
        }
        if (serviceDetails.getDocuments() != null && !serviceDetails.getDocuments().isEmpty()) {
            // Supprimer les anciens documents si nécessaire
            existingService.getDocuments().clear();

            // Ajouter les nouveaux documents
            for (InvestmentServiceDocument doc : serviceDetails.getDocuments()) {
                InvestmentServiceDocument newDoc = new InvestmentServiceDocument();
                newDoc.setFileName(doc.getFileName());
                newDoc.setFileType(doc.getFileType());
                newDoc.setFileSize(doc.getFileSize());
                newDoc.setFilePath(doc.getFilePath());
                newDoc.setDownloadUrl(doc.getDownloadUrl());
                newDoc.setIsPrimary(doc.getIsPrimary());
                newDoc.setInvestmentService(existingService);
                existingService.addDocument(newDoc);
            }
            log.info("📎 Documents mis à jour");
        }

        // ✅ Si c'était une modification autorisée, on révoque l'autorisation après utilisation
        if (existingService.getStatus() == ServiceStatus.APPROVED && existingService.isEditAuthorized()) {
            existingService.setEditAuthorizedUntil(null);
            existingService.setAuthorizedByAdminId(null);
            log.info("🔓 Autorisation de modification révoquée après utilisation");

            // Mettre à jour la demande associée
            List<ServiceRequest> approvedRequests = serviceRequestRepository
                    .findByServiceIdAndStatus(id, ServiceStatus.APPROVED);

            for (ServiceRequest request : approvedRequests) {
                if (request.getRequestType() == RequestType.EDIT) {
                    request.setStatus(ServiceStatus.REJECTED); // On utilise REJECTED pour indiquer "utilisé"
                    request.setExecutionDate(LocalDateTime.now());
                    serviceRequestRepository.save(request);
                    log.info("📝 Demande ID: {} marquée comme utilisée", request.getId());
                }
            }
        }

        InvestmentService updated = investmentRepository.save(existingService);
        log.info("✅ Service mis à jour avec succès, ID: {}", updated.getId());

        // ============================================================
        // ✅ NOTIFICATIONS VERS ADMIN, INVESTORS ET INTERNATIONAL_COMPANY
        // ============================================================
        if (updated.getStatus() == ServiceStatus.APPROVED) {

            // 1. NOTIFICATION À L'ADMIN
            String adminTitle = "✏️ Service d'investissement modifié";
            String adminMessage = String.format(
                    "Le partenaire %s %s a modifié le service '%s'.\n" +
                            "Ancien titre: %s | Nouveau titre: %s",
                    partner.getFirstName(),
                    partner.getLastName(),
                    updated.getTitle(),
                    oldTitle,
                    updated.getTitle()
            );

            notificationService.createNotificationForRole(
                    adminTitle,
                    adminMessage,
                    Role.ADMIN,
                    updated.getId()
            );

            // 2. NOTIFICATION À TOUS LES INVESTISSEURS
            String investorTitle = "🔄 Nouvelle mise à jour de service";
            String investorMessage = String.format(
                    "Le service d'investissement '%s' a été mis à jour.\n" +
                            "Titre: %s | Montant total: %s TND | Région: %s",
                    updated.getTitle(),
                    updated.getTitle(),
                    updated.getTotalAmount() != null ? updated.getTotalAmount() : "N/A",
                    updated.getRegion() != null ? updated.getRegion().getName() : "N/A"
            );

            notificationService.createNotificationForRole(
                    investorTitle,
                    investorMessage,
                    Role.INVESTOR,
                    updated.getId()
            );

            // 3. NOTIFICATION À TOUTES LES SOCIÉTÉS INTERNATIONALES
            String companyTitle = "🔄 Service d'investissement mis à jour";
            String companyMessage = String.format(
                    "Le service d'investissement '%s' a été mis à jour.\n" +
                            "Titre: %s | Montant total: %s TND | Secteur: %s",
                    updated.getTitle(),
                    updated.getTitle(),
                    updated.getTotalAmount() != null ? updated.getTotalAmount() : "N/A",
                    updated.getEconomicSector() != null ? updated.getEconomicSector().getName() : "N/A"
            );

            notificationService.createNotificationForRole(
                    companyTitle,
                    companyMessage,
                    Role.INTERNATIONAL_COMPANY,
                    updated.getId()
            );
        }

        return updated;
    }

    // ========================================
    // DELETE - Avec vérification d'autorisation pour les services approuvés
    // ========================================
    @Transactional
    public void deleteInvestmentService(Long id, String partnerEmail, boolean isAdmin) {
        log.info("🗑️ Suppression service ID: {}, par: {}, isAdmin: {}", id, partnerEmail, isAdmin);

        // ✅ 1. Récupérer le service avec ses relations
        InvestmentService service = getInvestmentServiceById(id);
        String serviceName = service.getTitle();
        Long serviceId = service.getId();

        log.info("📋 Service à supprimer: {} (ID: {})", serviceName, id);
        log.info("📋 Statut du service: {}", service.getStatus());

        // ✅ 2. Vérifier toutes les relations
        List<ServiceRequest> relatedRequests = serviceRequestRepository.findByServiceId(id);
        int requestCount = relatedRequests.size();

        // ✅ Vérifier les favoris
        boolean hasInvestorFavorites = service.getFavoritedByInvestors() != null && !service.getFavoritedByInvestors().isEmpty();
        boolean hasCompanyFavorites = service.getFavoritedByCompanies() != null && !service.getFavoritedByCompanies().isEmpty();

        if (hasInvestorFavorites) {
            log.info("❤️ Ce service est dans les favoris de {} investisseur(s)", service.getFavoritedByInvestors().size());
        }
        if (hasCompanyFavorites) {
            log.info("🏢 Ce service est dans les favoris de {} société(s) internationale(s)", service.getFavoritedByCompanies().size());
        }
        if (requestCount > 0) {
            log.info("📋 Ce service a {} demande(s) associée(s)", requestCount);
        }

        // ✅ 3. CAS ADMIN - Suppression directe
        if (isAdmin) {
            log.info("👑 Suppression par ADMIN - autorisée");

            // Notifier le partenaire local avant suppression
            try {
                notificationService.notifyPartnerServiceDeletedByAdmin(service);
            } catch (Exception e) {
                log.error("❌ Erreur notification admin: {}", e.getMessage());
            }

            // ✅ 3.1 Nettoyer toutes les relations
            cleanAllFavorites(service);

            // ✅ 3.2 Puis nettoyer toutes les autres relations
            cleanAllRelations(service, relatedRequests, hasInvestorFavorites, hasCompanyFavorites);

            // ✅ 3.2 NOTIFICATIONS VERS INVESTORS ET INTERNATIONAL_COMPANY
            sendDeletionNotifications(serviceName, serviceId, null, true);

            // Supprimer le service
            investmentRepository.delete(service);
            log.info("✅ Service supprimé avec succès par admin");
            return;
        }

        // ✅ 4. CAS PARTENAIRE LOCAL - Vérifications d'autorisation
        log.info("👤 Suppression par partenaire local");

        // Vérifier que le partenaire existe
        LocalPartner partner = localPartnerRepository.findByEmail(partnerEmail)
                .orElseThrow(() -> {
                    log.error("❌ Partenaire non trouvé avec email: {}", partnerEmail);
                    return new RuntimeException("Partenaire non trouvé");
                });

        // Vérifier que le partenaire est bien le propriétaire
        if (!service.getProvider().getId().equals(partner.getId())) {
            log.error("❌ Tentative de suppression non autorisée - Partenaire {} n'est pas propriétaire", partnerEmail);
            throw new RuntimeException("Vous ne pouvez supprimer que vos propres services");
        }

        // ✅ 5. VÉRIFICATION D'AUTORISATION SELON LE STATUT
        if (service.getStatus() == ServiceStatus.APPROVED) {
            // Service approuvé - besoin d'autorisation
            if (!service.isDeleteAuthorized()) {
                log.warn("⚠️ Tentative de suppression d'un service APPROUVÉ sans autorisation");
                throw new RuntimeException(
                        "Ce service est approuvé. Pour le supprimer, vous devez faire une demande à l'admin."
                );
            }
            log.info("✅ Suppression autorisée par admin - Autorisation valide");
        }
        else if (service.getStatus() == ServiceStatus.PENDING) {
            // Service en attente - suppression autorisée sans restriction
            log.info("✅ Suppression de service en attente autorisée");
        }
        else if (service.getStatus() == ServiceStatus.REJECTED) {
            log.error("❌ Tentative de suppression d'un service REJETÉ");
            throw new RuntimeException("Impossible de supprimer un service rejeté");
        }
        else {
            log.error("❌ Statut de service non reconnu: {}", service.getStatus());
            throw new RuntimeException("Statut de service non reconnu");
        }

        cleanAllFavorites(service);

// ✅ 7. PUIS NETTOYER TOUTES LES AUTRES RELATIONS
        cleanAllRelations(service, relatedRequests, hasInvestorFavorites, hasCompanyFavorites);

        // ✅ 7. NOTIFICATIONS VERS ADMIN, INVESTORS ET INTERNATIONAL_COMPANY
        sendDeletionNotifications(serviceName, serviceId, partner, false);

        // ✅ 8. SUPPRESSION DU SERVICE
        try {
            investmentRepository.delete(service);
            log.info("✅ Service supprimé avec succès");
        } catch (Exception e) {
            log.error("❌ Erreur lors de la suppression: {}", e.getMessage());

            // Tentative de secours: deleteById
            try {
                investmentRepository.deleteById(id);
                log.info("✅ Service supprimé avec deleteById");
            } catch (Exception ex) {
                log.error("❌ Échec de la suppression même avec deleteById: {}", ex.getMessage());
                throw new RuntimeException("Impossible de supprimer le service: " + e.getMessage());
            }
        }
    }

    /**
     * Méthode utilitaire pour envoyer les notifications de suppression
     */
    private void sendDeletionNotifications(String serviceName, Long serviceId, LocalPartner partner, boolean isAdmin) {

        // 1. NOTIFICATION À L'ADMIN
        String adminTitle = "🗑️ Service d'investissement supprimé";
        String adminMessage;

        if (isAdmin) {
            adminMessage = String.format(
                    "Vous avez supprimé le service '%s'.",
                    serviceName
            );
        } else {
            adminMessage = String.format(
                    "Le partenaire %s %s a supprimé le service '%s' (autorisation approuvée).",
                    partner.getFirstName(),
                    partner.getLastName(),
                    serviceName
            );
        }

        notificationService.createNotificationForRole(
                adminTitle,
                adminMessage,
                Role.ADMIN,
                serviceId
        );

        // 2. NOTIFICATION À TOUS LES INVESTISSEURS
        String investorTitle = "🗑️ Service d'investissement supprimé";
        String investorMessage = String.format(
                "Le service d'investissement '%s' a été supprimé et n'est plus disponible.",
                serviceName
        );

        notificationService.createNotificationForRole(
                investorTitle,
                investorMessage,
                Role.INVESTOR,
                serviceId
        );

        // 3. NOTIFICATION À TOUTES LES SOCIÉTÉS INTERNATIONALES
        String companyTitle = "🗑️ Service d'investissement supprimé";
        String companyMessage = String.format(
                "Le service d'investissement '%s' a été supprimé et n'est plus disponible.",
                serviceName
        );

        notificationService.createNotificationForRole(
                companyTitle,
                companyMessage,
                Role.INTERNATIONAL_COMPANY,
                serviceId
        );
    }

    /**
     * Méthode utilitaire pour nettoyer toutes les relations avant suppression
     */
    private void cleanAllRelations(InvestmentService service, List<ServiceRequest> relatedRequests,
                                   boolean hasInvestorFavorites, boolean hasCompanyFavorites) {


        if (service.getDocuments() != null && !service.getDocuments().isEmpty()) {
            log.info("📎 Nettoyage de {} documents", service.getDocuments().size());
            service.getDocuments().clear();  // Les documents seront supprimés en cascade
        }
        // ✅ 1. Nettoyer les favoris des investisseurs
        if (hasInvestorFavorites) {
            log.info("❤️ Nettoyage des favoris des investisseurs...");
            for (Investor investor : service.getFavoritedByInvestors()) {
                investor.getFavoriteServices().remove(service);
            }
            service.getFavoritedByInvestors().clear();
        }

        // ✅ 2. Nettoyer les favoris des sociétés internationales
        if (hasCompanyFavorites) {
            log.info("🏢 Nettoyage des favoris des sociétés internationales...");
            for (internationalcompany company : service.getFavoritedByCompanies()) {
                company.getFavoriteServices().remove(service);
            }
            service.getFavoritedByCompanies().clear();
        }

        // ✅ 3. Traiter les demandes associées
        if (!relatedRequests.isEmpty()) {
            log.info("📝 Traitement de {} demande(s) associée(s)", relatedRequests.size());

            // Identifier les demandes de suppression approuvées
            List<ServiceRequest> approvedDeleteRequests = relatedRequests.stream()
                    .filter(req -> req.getRequestType() == RequestType.DELETE
                            && req.getStatus() == ServiceStatus.APPROVED)
                    .collect(Collectors.toList());

            // Marquer les demandes de suppression comme utilisées
            for (ServiceRequest request : approvedDeleteRequests) {
                request.setStatus(ServiceStatus.REJECTED);
                request.setExecutionDate(LocalDateTime.now());
                log.info("📝 Demande de suppression ID: {} marquée comme utilisée", request.getId());
            }

            if (!approvedDeleteRequests.isEmpty()) {
                serviceRequestRepository.saveAll(approvedDeleteRequests);
            }

            // ✅ CRITIQUE: Détacher TOUTES les demandes du service
            for (ServiceRequest request : relatedRequests) {
                request.setService(null);
            }
            serviceRequestRepository.saveAll(relatedRequests);
            serviceRequestRepository.flush();

            log.info("✅ Demandes détachées du service avec succès");
        }
    }

    // ========================================
    // ADMIN: Approuver un service
    // ========================================
    @Transactional
    public InvestmentService approveInvestmentService(Long id) {
        InvestmentService service = getInvestmentServiceById(id);
        service.setStatus(ServiceStatus.APPROVED);
        return investmentRepository.save(service);
    }

    // ========================================
    // ADMIN: Approuver un service avec notifications
    // ========================================
    public InvestmentService approveAndNotify(Long id) {
        log.info("🔵🔵🔵 ===== DÉBUT approveAndNotify POUR ID: {} ===== 🔵🔵🔵", id);

        InvestmentService approved = null;

        try {
            // 1. Approuver le service
            log.info("🔵 Étape 1: Appel de approveInvestmentService({})", id);
            approved = approveInvestmentService(id);
            log.info("✅ Service approuvé avec succès - ID: {}, Titre: {}", approved.getId(), approved.getTitle());

        } catch (Exception e) {
            log.error("❌ ERREUR lors de l'approbation du service: {}", e.getMessage(), e);
            throw e;
        }

        // 2. Notification au LOCAL_PARTNER
        try {
            log.info("🔵 Étape 2: Notification au LOCAL_PARTNER");
            notificationService.notifyLocalPartnerInvestmentApproved(approved);
            log.info("✅ Notification LOCAL_PARTNER envoyée avec succès");
        } catch (Exception e) {
            log.error("❌ ERREUR notification LOCAL_PARTNER: {}", e.getMessage(), e);
        }

        // 3. Notification aux INVESTORS
        try {
            log.info("🔵 Étape 3: Notification aux INVESTORS");
            notificationService.notifyInvestorsNewInvestmentService(approved);
            log.info("✅ Notification INVESTORS envoyée avec succès");
        } catch (Exception e) {
            log.error("❌ ERREUR notification INVESTORS: {}", e.getMessage(), e);
        }

        // 4. Notification aux INTERNATIONAL_COMPANY
        try {
            log.info("🔵 Étape 4: Notification aux INTERNATIONAL_COMPANY");
            notificationService.notifyInternationalCompaniesNewInvestmentService(approved);
            log.info("✅ Notification INTERNATIONAL_COMPANY envoyée avec succès");
        } catch (Exception e) {
            log.error("❌ ERREUR notification INTERNATIONAL_COMPANY: {}", e.getMessage(), e);
        }

        log.info("🔵🔵🔵 ===== FIN approveAndNotify POUR ID: {} ===== 🔵🔵🔵", id);
        return approved;
    }

    // ========================================
    // ADMIN: Rejeter un service
    // ========================================
    @Transactional
    public InvestmentService rejectInvestmentService(Long id) {
        log.info("🔴 ===== REJETER SERVICE =====");
        log.info("🔴 ID: {}", id);

        InvestmentService service = getInvestmentServiceById(id);
        log.info("🔴 Service trouvé: {}", service.getTitle());
        log.info("🔴 Statut actuel: {}", service.getStatus());

        service.setStatus(ServiceStatus.REJECTED);
        InvestmentService rejected = investmentRepository.save(service);
        log.info("🔴 Service rejeté avec ID: {}", rejected.getId());

        log.info("🔴 Appel notifyLocalPartnerInvestmentRejected");
        notificationService.notifyLocalPartnerInvestmentRejected(rejected);

        log.info("🔴 ===== FIN REJET =====");
        return rejected;
    }

    // ========================================
    // PARTIE 1: GESTION DES DEMANDES (PARTENAIRE LOCAL)
    // ========================================

    /**
     * Partenaire local : Demander la modification d'un service approuvé
     */
    @Transactional
    public ServiceRequest requestEdit(Long serviceId, String partnerEmail, String reason, String requestedChanges) {
        log.info("📝 Demande de MODIFICATION pour service ID: {} par: {}", serviceId, partnerEmail);

        InvestmentService service = getInvestmentServiceById(serviceId);
        LocalPartner partner = localPartnerRepository.findByEmail(partnerEmail)
                .orElseThrow(() -> new RuntimeException("Partenaire non trouvé"));

        // Vérifier que le partenaire est bien le propriétaire
        if (!service.getProvider().getId().equals(partner.getId())) {
            throw new RuntimeException("Vous ne pouvez demander que pour vos propres services");
        }

        // Vérifier que le service est approuvé
        if (service.getStatus() != ServiceStatus.APPROVED) {
            throw new RuntimeException("Seuls les services approuvés peuvent être modifiés");
        }

        // Vérifier qu'il n'y a pas déjà une demande en cours
        List<ServiceRequest> existingRequests = serviceRequestRepository
                .findByServiceIdAndStatus(serviceId, ServiceStatus.PENDING);

        if (!existingRequests.isEmpty()) {
            throw new RuntimeException("Une demande est déjà en cours pour ce service");
        }

        ServiceRequest request = new ServiceRequest(
                service, partner, RequestType.EDIT, reason, requestedChanges
        );

        ServiceRequest saved = serviceRequestRepository.save(request);

        // NOTIFICATION : Admin
        notificationService.notifyAdminNewRequest(saved);

        log.info("✅ Demande de modification créée avec ID: {}", saved.getId());
        return saved;
    }

    /**
     * Partenaire local : Demander la suppression d'un service approuvé
     */
    @Transactional
    public ServiceRequest requestDelete(Long serviceId, String partnerEmail, String reason) {
        log.info("🗑️ Demande de SUPPRESSION pour service ID: {} par: {}", serviceId, partnerEmail);

        InvestmentService service = getInvestmentServiceById(serviceId);
        LocalPartner partner = localPartnerRepository.findByEmail(partnerEmail)
                .orElseThrow(() -> new RuntimeException("Partenaire non trouvé"));

        // Vérifier que le partenaire est bien le propriétaire
        if (!service.getProvider().getId().equals(partner.getId())) {
            throw new RuntimeException("Vous ne pouvez demander que pour vos propres services");
        }

        // Vérifier que le service est approuvé
        if (service.getStatus() != ServiceStatus.APPROVED) {
            throw new RuntimeException("Seuls les services approuvés peuvent être supprimés");
        }

        // Vérifier qu'il n'y a pas déjà une demande en cours
        List<ServiceRequest> existingRequests = serviceRequestRepository
                .findByServiceIdAndStatus(serviceId, ServiceStatus.PENDING);

        if (!existingRequests.isEmpty()) {
            throw new RuntimeException("Une demande est déjà en cours pour ce service");
        }

        ServiceRequest request = new ServiceRequest(
                service, partner, RequestType.DELETE, reason, null
        );

        ServiceRequest saved = serviceRequestRepository.save(request);

        // NOTIFICATION : Admin
        notificationService.notifyAdminNewRequest(saved);

        log.info("✅ Demande de suppression créée avec ID: {}", saved.getId());
        return saved;
    }

    // ========================================
    // PARTIE 2: ADMIN APPROUVE LES DEMANDES
    // ========================================

    /**
     * Admin : Approuver une demande de modification
     */
    @Transactional
    public void approveEditRequest(Long requestId, String adminEmail) {  // Changé le retour en void
        log.info("🔐 Admin {} approuve la demande de MODIFICATION ID: {}", adminEmail, requestId);

        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        if (request.getRequestType() != RequestType.EDIT) {
            throw new RuntimeException("Cette demande n'est pas une demande de modification");
        }

        if (request.getStatus() != ServiceStatus.PENDING) {
            throw new RuntimeException("Cette demande a déjà été traitée");
        }

        Admin admin = adminRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        InvestmentService service = request.getService();

        // ✅ ACTIVER L'AUTORISATION DE MODIFICATION (7 jours)
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(7);
        service.setEditAuthorizedUntil(expiryDate);
        service.setAuthorizedByAdminId(admin.getId());

        investmentRepository.save(service);

        // ✅ NOTIFICATION avant suppression (utilise l'objet request encore existant)
        notificationService.notifyPartnerEditApproved(request);

        // ❌ SUPPRIMER la demande après la notification
        serviceRequestRepository.delete(request);
        log.info("📝 Demande ID: {} supprimée après approbation", requestId);

        log.info("✅ Demande de modification approuvée et supprimée - Service modifiable jusqu'au {}", expiryDate.toLocalDate());
    }

    /**
     * Admin : Approuver une demande de suppression
     */
    @Transactional
    public void approveDeleteRequest(Long requestId, String adminEmail) {  // Changé le retour en void
        log.info("🔐 Admin {} approuve la demande de SUPPRESSION ID: {}", adminEmail, requestId);

        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        if (request.getRequestType() != RequestType.DELETE) {
            throw new RuntimeException("Cette demande n'est pas une demande de suppression");
        }

        if (request.getStatus() != ServiceStatus.PENDING) {
            throw new RuntimeException("Cette demande a déjà été traitée");
        }

        Admin admin = adminRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        InvestmentService service = request.getService();

        // ✅ ACTIVER L'AUTORISATION DE SUPPRESSION (3 jours)
        service.setDeleteAuthorized(true);
        service.setAuthorizedByAdminId(admin.getId());

        investmentRepository.save(service);

        // ✅ NOTIFICATION avant suppression
        notificationService.notifyPartnerDeleteApproved(request);

        // ❌ SUPPRIMER la demande après la notification
        serviceRequestRepository.delete(request);
        log.info("📝 Demande ID: {} supprimée après approbation", requestId);

        log.info("✅ Demande de suppression approuvée et supprimée - Service supprimable jusqu'au {}",
                LocalDateTime.now().plusDays(3).toLocalDate());
    }

    /**
     * Admin : Rejeter une demande
     */
    @Transactional
    public void rejectRequest(Long requestId, String adminEmail) {
        log.info("🔐 Admin {} rejette la demande ID: {}", adminEmail, requestId);

        ServiceRequest request = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée"));

        if (request.getStatus() != ServiceStatus.PENDING) {
            throw new RuntimeException("Cette demande a déjà été traitée");
        }

        Admin admin = adminRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        // ✅ NOTIFICATION avant suppression
        notificationService.notifyPartnerRequestRejected(request);

        // ❌ SUPPRIMER directement la demande
        serviceRequestRepository.delete(request);
        log.info("📝 Demande ID: {} supprimée après rejet", requestId);

        log.info("✅ Demande rejetée et supprimée");
    }

    // ========================================
    // PARTIE 3: MÉTHODES POUR RÉCUPÉRER LES DEMANDES
    // ========================================

    /**
     * Récupérer toutes les demandes en attente
     */
    public List<ServiceRequest> getPendingRequests() {
        return serviceRequestRepository.findByStatus(ServiceStatus.PENDING);
    }

    /**
     * Récupérer les demandes par type
     */
    public List<ServiceRequest> getRequestsByType(RequestType type) {
        return serviceRequestRepository.findByRequestType(type);
    }

    /**
     * Récupérer les demandes d'un partenaire
     */
    public List<ServiceRequest> getPartnerRequests(Long partnerId) {
        return serviceRequestRepository.findByPartnerId(partnerId);
    }

    /**
     * Compter les demandes en attente
     */
    public long getPendingRequestsCount() {
        return serviceRequestRepository.countByStatus(ServiceStatus.PENDING);
    }

    // ========================================
    // INVESTOR: Marquer son intérêt
    // ========================================
    @Transactional
    public InvestmentService markInterest(Long serviceId, Long investorId) {
        log.info("⭐ Marquer intérêt - Service ID: {}, Investor ID: {}", serviceId, investorId);

        InvestmentService service = getInvestmentServiceById(serviceId);
        Investor investor = investorRepository.findById(investorId)
                .orElseThrow(() -> {
                    log.error("❌ Investisseur non trouvé avec ID: {}", investorId);
                    return new RuntimeException("Investisseur non trouvé");
                });

        if (service.getInterestedInvestors() == null) {
            service.setInterestedInvestors(new java.util.ArrayList<>());
        }

        boolean alreadyInterested = service.getInterestedInvestors().stream()
                .anyMatch(i -> i.getId().equals(investorId));

        if (!alreadyInterested) {
            service.getInterestedInvestors().add(investor);
            log.info("✅ Intérêt marqué avec succès");
        } else {
            log.info("ℹ️ L'investisseur avait déjà marqué son intérêt");
        }

        return investmentRepository.save(service);
    }

    // ========================================
    // RECHERCHE
    // ========================================
    public List<InvestmentService> searchInvestmentServices(String keyword) {
        log.info("🔍 Recherche de services avec mot-clé: {}", keyword);
        List<InvestmentService> results = investmentRepository
                .findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCaseOrTitleContainingIgnoreCase(
                        keyword, keyword, keyword);
        log.info("✅ {} résultats trouvés", results.size());
        return results;
    }

    // ========================================
    // RECHERCHE AVANCÉE
    // ========================================
    public List<InvestmentService> advancedSearch(
            Long regionId, Long sectorId, ServiceStatus status, BigDecimal maxAmount) {
        log.info("🔍 Recherche avancée - Région: {}, Secteur: {}, Statut: {}, MaxAmount: {}",
                regionId, sectorId, status, maxAmount);

        List<InvestmentService> results = investmentRepository.advancedSearch(regionId, sectorId, status, maxAmount);
        log.info("✅ {} résultats trouvés", results.size());
        return results;
    }

    // ========================================
    // STATISTIQUES
    // ========================================
    public Map<String, Object> getStatistics() {
        log.info("📊 Calcul des statistiques");

        Map<String, Object> stats = new HashMap<>();
        long total = investmentRepository.count();
        long pending = investmentRepository.findByStatus(ServiceStatus.PENDING).size();
        long approved = investmentRepository.findByStatus(ServiceStatus.APPROVED).size();
        long rejected = investmentRepository.findByStatus(ServiceStatus.REJECTED).size();
        long pendingRequests = serviceRequestRepository.countByStatus(ServiceStatus.PENDING);

        stats.put("total", total);
        stats.put("pending", pending);
        stats.put("approved", approved);
        stats.put("rejected", rejected);
        stats.put("pendingRequests", pendingRequests);

        log.info("📊 Statistiques - Total: {}, En attente: {}, Approuvés: {}, Rejetés: {}, Demandes: {}",
                total, pending, approved, rejected, pendingRequests);

        return stats;
    }

    // ========================================
    // TÂCHE PLANIFIÉE POUR NETTOYER LES AUTORISATIONS EXPIRÉES
    // ========================================
    @Scheduled(cron = "0 0 * * * *") // Toutes les heures
    @Transactional
    public void cleanExpiredAuthorizations() {
        log.info("🧹 Nettoyage des autorisations expirées");

        LocalDateTime now = LocalDateTime.now();

        // Nettoyer les autorisations de modification expirées
        List<InvestmentService> servicesWithExpiredEdit = investmentRepository
                .findByEditAuthorizedUntilBefore(now);

        for (InvestmentService service : servicesWithExpiredEdit) {
            service.setEditAuthorizedUntil(null);
            service.setAuthorizedByAdminId(null);
            log.info("⏰ Autorisation de modification expirée pour service ID: {}", service.getId());
        }
        investmentRepository.saveAll(servicesWithExpiredEdit);

        log.info("🧹 Nettoyage terminé - {} autorisations expirées nettoyées", servicesWithExpiredEdit.size());
    }

    // ========================================
    // VALIDATION
    // ========================================
    private void validateRequiredFields(InvestmentService service) {
        log.debug("🔍 Validation des champs obligatoires");

        if (service.getName() == null || service.getName().trim().isEmpty()) {
            log.error("❌ Validation échouée: nom manquant");
            throw new RuntimeException("Le nom est obligatoire");
        }
        if (service.getTitle() == null || service.getTitle().trim().isEmpty()) {
            log.error("❌ Validation échouée: titre manquant");
            throw new RuntimeException("Le titre est obligatoire");
        }
        if (service.getRegion() == null) {
            log.error("❌ Validation échouée: région manquante");
            throw new RuntimeException("La région est obligatoire");
        }
        // 🗑️ SUPPRIMÉ : validation du prix
        if (service.getAvailability() == null) {
            log.error("❌ Validation échouée: disponibilité manquante");
            throw new RuntimeException("La disponibilité est obligatoire");
        }
        if (service.getContactPerson() == null || service.getContactPerson().trim().isEmpty()) {
            log.error("❌ Validation échouée: contact manquant");
            throw new RuntimeException("Le contact responsable est obligatoire");
        }

        log.debug("✅ Validation réussie");
    }

    // ========================================
    // MÉTHODES POUR LE CONTROLLER ServiceRequestController
    // ========================================

    /**
     * Récupérer une demande par son ID
     */
    public ServiceRequest getRequestById(Long requestId) {
        log.info("🔍 Récupération demande par ID: {}", requestId);
        return serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Demande non trouvée avec id: " + requestId));
    }

    /**
     * Récupérer toutes les demandes
     */
    public List<ServiceRequest> getAllRequests() {
        log.info("📋 Récupération de toutes les demandes");
        return serviceRequestRepository.findAll();
    }

    /**
     * Récupérer les demandes par statut
     */
    public List<ServiceRequest> getRequestsByStatus(ServiceStatus status) {
        log.info("📋 Récupération des demandes avec statut: {}", status);
        return serviceRequestRepository.findByStatus(status);
    }

    /**
     * Récupérer les demandes par type et statut
     */
    public List<ServiceRequest> getRequestsByTypeAndStatus(RequestType type, ServiceStatus status) {
        log.info("📋 Récupération des demandes - Type: {}, Statut: {}", type, status);
        return serviceRequestRepository.findByTypeAndStatus(type, status);
    }

    /**
     * Compter les demandes de modification en attente
     */
    public long getPendingEditRequestsCount() {
        log.debug("🔢 Comptage des demandes de modification en attente");
        return serviceRequestRepository.countByTypeAndStatus(RequestType.EDIT, ServiceStatus.PENDING);
    }

    /**
     * Compter les demandes de suppression en attente
     */
    public long getPendingDeleteRequestsCount() {
        log.debug("🔢 Comptage des demandes de suppression en attente");
        return serviceRequestRepository.countByTypeAndStatus(RequestType.DELETE, ServiceStatus.PENDING);
    }

    /**
     * Annuler une demande (par le partenaire)
     */
    @Transactional
    public void cancelRequest(Long requestId, Long partnerId) {
        log.info("🗑️ Annulation demande ID: {} par partenaire ID: {}", requestId, partnerId);

        ServiceRequest request = getRequestById(requestId);

        if (!request.getPartner().getId().equals(partnerId)) {
            log.error("❌ Tentative d'annulation non autorisée - Partenaire {} n'est pas propriétaire", partnerId);
            throw new RuntimeException("Vous ne pouvez annuler que vos propres demandes");
        }

        if (request.getStatus() != ServiceStatus.PENDING) {
            log.error("❌ Tentative d'annulation d'une demande non en attente - Statut: {}", request.getStatus());
            throw new RuntimeException("Seules les demandes en attente peuvent être annulées");
        }

        // ✅ NOTIFICATION si nécessaire (optionnel)
        // notificationService.notifyRequestCancelled(request);

        // ❌ SUPPRIMER directement la demande
        serviceRequestRepository.delete(request);
        log.info("✅ Demande {} supprimée après annulation par le partenaire {}", requestId, partnerId);
    }

    /**
     * Obtenir des statistiques sur les demandes
     */
    public Map<String, Object> getRequestStatistics() {
        log.info("📊 Calcul des statistiques des demandes");

        Map<String, Object> stats = new HashMap<>();

        long total = serviceRequestRepository.count();
        long pending = serviceRequestRepository.countByStatus(ServiceStatus.PENDING);
        long approved = serviceRequestRepository.countByStatus(ServiceStatus.APPROVED);
        long rejected = serviceRequestRepository.countByStatus(ServiceStatus.REJECTED);
        long editRequests = serviceRequestRepository.countByType(RequestType.EDIT);
        long deleteRequests = serviceRequestRepository.countByType(RequestType.DELETE);

        stats.put("total", total);
        stats.put("pending", pending);
        stats.put("approved", approved);
        stats.put("rejected", rejected);
        stats.put("editRequests", editRequests);
        stats.put("deleteRequests", deleteRequests);
        stats.put("pendingEdit", serviceRequestRepository.countByTypeAndStatus(RequestType.EDIT, ServiceStatus.PENDING));
        stats.put("pendingDelete", serviceRequestRepository.countByTypeAndStatus(RequestType.DELETE, ServiceStatus.PENDING));

        log.info("📊 Statistiques - Total: {}, En attente: {}, Approuvées: {}, Rejetées: {}",
                total, pending, approved, rejected);

        return stats;
    }
    // ========================================
// DELETE - Service rejeté (par le partenaire local)
// ========================================
    @Transactional
    public void deleteRejectedService(Long id, String partnerEmail) {
        log.info("🗑️ Suppression service rejeté ID: {} par: {}", id, partnerEmail);

        InvestmentService service = getInvestmentServiceById(id);

        // Vérifier que le service est bien REJECTED
        if (service.getStatus() != ServiceStatus.REJECTED) {
            log.error("❌ Tentative de suppression d'un service non rejeté - Statut: {}", service.getStatus());
            throw new RuntimeException("Seuls les services rejetés peuvent être supprimés directement");
        }

        LocalPartner partner = localPartnerRepository.findByEmail(partnerEmail)
                .orElseThrow(() -> new RuntimeException("Partenaire non trouvé"));

        // Vérifier que le partenaire est bien le propriétaire
        if (!service.getProvider().getId().equals(partner.getId())) {
            log.error("❌ Tentative de suppression non autorisée - Partenaire {} n'est pas propriétaire", partnerEmail);
            throw new RuntimeException("Vous ne pouvez supprimer que vos propres services");
        }

        // Vérifier les relations avant suppression
        List<ServiceRequest> relatedRequests = serviceRequestRepository.findByServiceId(id);
        boolean hasInvestorFavorites = service.getFavoritedByInvestors() != null && !service.getFavoritedByInvestors().isEmpty();
        boolean hasCompanyFavorites = service.getFavoritedByCompanies() != null && !service.getFavoritedByCompanies().isEmpty();

        // Nettoyer les relations
        cleanAllRelations(service, relatedRequests, hasInvestorFavorites, hasCompanyFavorites);

        // Supprimer le service
        investmentRepository.delete(service);
        log.info("✅ Service rejeté supprimé avec succès par le partenaire");
    }
    @Transactional
    public InvestmentService addDocumentsToService(Long serviceId, List<InvestmentServiceDocument> documents) {
        log.info("📎 Ajout de {} documents au service ID: {}", documents.size(), serviceId);

        InvestmentService service = getInvestmentServiceById(serviceId);

        for (InvestmentServiceDocument doc : documents) {
            doc.setInvestmentService(service);
            documentRepository.save(doc);  // ✅ Sauvegarder d'abord le document
            service.addDocument(doc);       // ✅ Puis l'ajouter à la liste
        }

        InvestmentService updated = investmentRepository.save(service);
        log.info("✅ Documents ajoutés avec succès");

        return updated;
    }
    @Transactional
    public InvestmentService replaceDocuments(Long serviceId, List<InvestmentServiceDocument> newDocuments) {
        log.info("🔄 Remplacement des documents pour le service ID: {}", serviceId);

        InvestmentService service = getInvestmentServiceById(serviceId);

        // 1. Supprimer les anciens documents (physiques + BD)
        for (InvestmentServiceDocument oldDoc : service.getDocuments()) {
            try {
                // Supprimer le fichier physique
                fileStorageService.deleteFile(oldDoc.getFilePath(), FileStorageService.ServiceType.INVESTMENT);
                log.info("🗑️ Fichier supprimé: {}", oldDoc.getFilePath());
            } catch (Exception e) {
                log.warn("⚠️ Impossible de supprimer le fichier: {}", e.getMessage());
            }
            // Supprimer de la BD
            documentRepository.delete(oldDoc);
        }

        // 2. Vider la liste des documents
        service.getDocuments().clear();

        // 3. Ajouter les nouveaux documents
        for (InvestmentServiceDocument doc : newDocuments) {
            doc.setInvestmentService(service);
            InvestmentServiceDocument savedDoc = documentRepository.save(doc);
            service.addDocument(savedDoc);
        }

        InvestmentService updated = investmentRepository.save(service);
        log.info("✅ Documents remplacés avec succès - {} nouveaux documents", newDocuments.size());

        return updated;
    }
    // ========================================
// Récupérer un document par son ID
// ========================================
    public InvestmentServiceDocument getDocumentById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document non trouvé avec id: " + documentId));
    }

    // ========================================
// Supprimer un document
// ========================================
    @Transactional
    public void deleteDocument(Long documentId) {
        log.info("🗑️ Suppression du document ID: {}", documentId);

        InvestmentServiceDocument document = getDocumentById(documentId);

        // Détacher du service
        InvestmentService service = document.getInvestmentService();
        if (service != null) {
            service.getDocuments().remove(document);
            investmentRepository.save(service);
        }

        // Supprimer le document
        documentRepository.delete(document);
        log.info("✅ Document supprimé avec succès");
    }
    // ========================================
// Ajouter un document au service
// ========================================
    @Transactional
    public InvestmentServiceDocument addDocumentToService(Long serviceId, InvestmentServiceDocument document) {
        log.info("📎 Ajout d'un document au service ID: {}", serviceId);

        InvestmentService service = getInvestmentServiceById(serviceId);

        // Si c'est le premier document, le marquer comme principal
        if (service.getDocuments().isEmpty()) {
            document.setIsPrimary(true);
        }

        document.setInvestmentService(service);
        InvestmentServiceDocument savedDoc = documentRepository.save(document);
        service.getDocuments().add(savedDoc);

        log.info("✅ Document ajouté avec succès, ID: {}", savedDoc.getId());
        return savedDoc;
    }

    // ========================================
// Révoquer l'autorisation de modification
// ========================================
    @Transactional
    public void revokeEditAuthorization(Long serviceId) {
        log.info("🔓 Révocation autorisation de modification pour service ID: {}", serviceId);

        InvestmentService service = getInvestmentServiceById(serviceId);

        // ✅ 1. Révoquer l'autorisation
        service.setEditAuthorizedUntil(null);
        service.setAuthorizedByAdminId(null);
        investmentRepository.save(service);
        log.info("✅ Autorisation de modification révoquée pour service ID: {}", serviceId);

        // ✅ 2. Mettre à jour la demande associée
        List<ServiceRequest> approvedRequests = serviceRequestRepository
                .findByServiceIdAndStatus(serviceId, ServiceStatus.APPROVED);

        for (ServiceRequest request : approvedRequests) {
            if (request.getRequestType() == RequestType.EDIT) {
                request.setStatus(ServiceStatus.REJECTED); // Marquer comme utilisée
                request.setExecutionDate(LocalDateTime.now());
                serviceRequestRepository.save(request);
                log.info("📝 Demande ID: {} marquée comme utilisée", request.getId());
            }
        }
    }
    /**
     * ✅ Méthode utilitaire pour nettoyer tous les favoris d'un service
     */
    private void cleanAllFavorites(InvestmentService service) {
        log.info(" Nettoyage des favoris pour le service ID: {}", service.getId());

        // 1. Nettoyer les favoris des investisseurs
        if (service.getFavoritedByInvestors() != null && !service.getFavoritedByInvestors().isEmpty()) {
            int investorCount = service.getFavoritedByInvestors().size();
            log.info(" Retrait du service des favoris de {} investisseur(s)", investorCount);

            for (Investor investor : new ArrayList<>(service.getFavoritedByInvestors())) {
                investor.getFavoriteServices().remove(service);
            }
            service.getFavoritedByInvestors().clear();
            log.info(" Favoris des investisseurs nettoyés");
        }

        // 2. Nettoyer les favoris des sociétés internationales
        if (service.getFavoritedByCompanies() != null && !service.getFavoritedByCompanies().isEmpty()) {
            int companyCount = service.getFavoritedByCompanies().size();
            log.info(" Retrait du service des favoris de {} société(s) internationale(s)", companyCount);

            for (internationalcompany company : new ArrayList<>(service.getFavoritedByCompanies())) {
                company.getFavoriteServices().remove(service);
            }
            service.getFavoritedByCompanies().clear();
            log.info(" Favoris des sociétés nettoyés");
        }
    }
}