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
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CollaborationServiceService {

    private static final Logger log = LoggerFactory.getLogger(CollaborationServiceService.class);

    private final CollaborationServiceRepository repository;
    private final CollaborationServiceDocumentRepository documentRepository;
    private final LocalPartnerRepository localPartnerRepository;
    private final RegionRepository regionRepository;
    private final NotificationService notificationService;
    private final CollaborationServiceRequestRepository collaborationRequestRepository;
    private final AdminRepository adminRepository;
    private final FileStorageService fileStorageService;

    public CollaborationServiceService(
            CollaborationServiceRepository repository,
            CollaborationServiceDocumentRepository documentRepository,
            LocalPartnerRepository localPartnerRepository,
            RegionRepository regionRepository,
            NotificationService notificationService,
            CollaborationServiceRequestRepository collaborationRequestRepository,
            AdminRepository adminRepository,
            FileStorageService fileStorageService) {
        this.repository = repository;
        this.documentRepository = documentRepository;
        this.localPartnerRepository = localPartnerRepository;
        this.regionRepository = regionRepository;
        this.notificationService = notificationService;
        this.collaborationRequestRepository = collaborationRequestRepository;
        this.adminRepository = adminRepository;
        this.fileStorageService = fileStorageService;

        log.info("✅ CollaborationServiceService initialized");
    }

    // ========================================
    // CREATE - Par un partenaire local (avec email)
    // ========================================
    @Transactional
    public CollaborationService createCollaborationService(String partnerEmail, CollaborationService service) {
        log.info("📝 Creating collaboration service by email: {}", partnerEmail);

        LocalPartner partner = localPartnerRepository.findByEmail(partnerEmail)
                .orElseThrow(() -> {
                    log.error("❌ Local partner not found with email: {}", partnerEmail);
                    return new RuntimeException("Local partner not found with email: " + partnerEmail);
                });

        log.info("✅ Partner found: {} {}", partner.getFirstName(), partner.getLastName());
        return createCollaborationServiceWithProviderId(service, partner.getId());
    }

    // ========================================
    // CREATE - With Provider ID
    // ========================================
    @Transactional
    public CollaborationService createCollaborationServiceWithProviderId(CollaborationService service, Long providerId) {
        log.info("🔵 ===== START CREATION COLLABORATION SERVICE =====");
        log.info("🔵 Provider ID received: {}", providerId);

        LocalPartner provider = localPartnerRepository.findById(providerId)
                .orElseThrow(() -> {
                    log.error("❌ Provider not found with id: {}", providerId);
                    return new RuntimeException("Provider not found with id: " + providerId);
                });

        log.info("✅ Provider found: {} - {}", provider.getEmail(), provider.getFirstName());

        CollaborationService newService = new CollaborationService();
        newService.setName(service.getName());
        newService.setDescription(service.getDescription());
        log.info("📋 Service: {} - {}", service.getName(), service.getDescription());

        if (service.getRegion() != null && service.getRegion().getId() != null) {
            Region region = regionRepository.findById(service.getRegion().getId())
                    .orElseThrow(() -> {
                        log.error("❌ Region not found with id: {}", service.getRegion().getId());
                        return new RuntimeException("Region not found with id: " + service.getRegion().getId());
                    });
            newService.setRegion(region);
            log.info("📍 Region: {}", region.getName());
        }

        newService.setRequestedBudget(service.getRequestedBudget());
        newService.setAvailability(service.getAvailability());
        newService.setContactPerson(service.getContactPerson());
        log.info("💰 Requested budget: {}, Availability: {}, Contact: {}",
                service.getRequestedBudget(), service.getAvailability(), service.getContactPerson());

        newService.setCollaborationType(service.getCollaborationType());
        newService.setActivityDomain(service.getActivityDomain());
        newService.setExpectedBenefits(service.getExpectedBenefits());
        newService.setRequiredSkills(service.getRequiredSkills());
        newService.setCollaborationDuration(service.getCollaborationDuration());
        newService.setAddress(service.getAddress());
        if (service.getDocuments() != null && !service.getDocuments().isEmpty()) {
            log.info("📎 Copie de {} documents", service.getDocuments().size());
            for (CollaborationServiceDocument doc : service.getDocuments()) {
                CollaborationServiceDocument newDoc = new CollaborationServiceDocument();
                newDoc.setFileName(doc.getFileName());
                newDoc.setFileType(doc.getFileType());
                newDoc.setFileSize(doc.getFileSize());
                newDoc.setFilePath(doc.getFilePath());
                newDoc.setDownloadUrl(doc.getDownloadUrl());
                newDoc.setIsPrimary(doc.getIsPrimary());
                newDoc.setCollaborationService(newService);
                newService.addDocument(newDoc);
            }
        }

        newService.setProvider(provider);
        log.info("👤 Provider assigned: {}", provider.getEmail());

        validateRequiredFields(newService);

        try {
            CollaborationService saved = repository.save(newService);
            log.info("✅ Collaboration service saved with ID: {}", saved.getId());

            log.info("📢 Notification ADMIN - New service pending");
            notificationService.notifyAdminNewService(saved);

            log.info("🔵 ===== END CREATION SERVICE (SUCCESS) =====\n");
            return saved;

        } catch (Exception e) {
            log.error("❌ ERROR during save: {}", e.getMessage(), e);
            throw new RuntimeException("Error during save: " + e.getMessage());
        }
    }

    // ========================================
    // READ - All services
    // ========================================
    public List<CollaborationService> getAllCollaborationServices() {
        log.debug("📋 Retrieving all collaboration services");
        List<CollaborationService> services = repository.findAll();
        log.debug("✅ {} services found", services.size());
        return services;
    }

    // ========================================
    // READ - By ID
    // ========================================
    public CollaborationService getCollaborationServiceById(Long id) {
        log.debug("🔍 Searching service by ID: {}", id);
        return repository.findById(id)
                .orElseThrow(() -> {
                    log.error("❌ Service not found with id: {}", id);
                    return new RuntimeException("Collaboration service not found with id: " + id);
                });
    }

    // ========================================
    // READ - By provider
    // ========================================
    public List<CollaborationService> getCollaborationServicesByProviderId(Long providerId) {
        log.debug("📋 Retrieving services for provider ID: {}", providerId);
        LocalPartner provider = localPartnerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found with id: " + providerId));
        List<CollaborationService> services = repository.findByProvider(provider);
        log.debug("✅ {} services found", services.size());
        return services;
    }

    // ========================================
    // READ - By region
    // ========================================
    public List<CollaborationService> getCollaborationServicesByRegionId(Long regionId) {
        log.debug("📋 Retrieving services for region ID: {}", regionId);
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new RuntimeException("Region not found with id: " + regionId));
        List<CollaborationService> services = repository.findByRegion(region);
        log.debug("✅ {} services found", services.size());
        return services;
    }

    // ========================================
    // READ - By status
    // ========================================
    public List<CollaborationService> getCollaborationServicesByStatus(ServiceStatus status) {
        log.debug("📋 Retrieving services with status: {}", status);
        List<CollaborationService> services = repository.findByStatus(status);
        log.debug("✅ {} services found", services.size());
        return services;
    }

    // ========================================
    // READ - Pending services
    // ========================================
    public List<CollaborationService> getPendingCollaborationServices() {
        log.debug("📋 Retrieving pending services");
        List<CollaborationService> services = repository.findByStatus(ServiceStatus.PENDING);
        log.debug("✅ {} pending services", services.size());
        return services;
    }

    // ========================================
    // READ - Approved services
    // ========================================
    public List<CollaborationService> getApprovedCollaborationServices() {
        log.debug("📋 Retrieving approved services");
        List<CollaborationService> services = repository.findByStatus(ServiceStatus.APPROVED);
        log.debug("✅ {} approved services", services.size());
        return services;
    }

    // ========================================
    // READ - Rejected services
    // ========================================
    public List<CollaborationService> getRejectedCollaborationServices() {
        log.debug("📋 Retrieving rejected services");
        List<CollaborationService> services = repository.findByStatus(ServiceStatus.REJECTED);
        log.debug("✅ {} rejected services", services.size());
        return services;
    }

    // ========================================
    // READ - By availability
    // ========================================
    public List<CollaborationService> getCollaborationServicesByAvailability(Availability availability) {
        log.debug("📋 Retrieving services with availability: {}", availability);
        List<CollaborationService> services = repository.findByAvailability(availability);
        log.debug("✅ {} services found", services.size());
        return services;
    }

    // ========================================
    // READ - By max budget
    // ========================================
    public List<CollaborationService> getCollaborationServicesByMaxBudget(BigDecimal maxBudget) {
        log.debug("📋 Retrieving services with max budget: {}", maxBudget);
        List<CollaborationService> services = repository.findByRequestedBudgetLessThanEqual(maxBudget);
        log.debug("✅ {} services found", services.size());
        return services;
    }

    // ========================================
    // READ - By collaboration type
    // ========================================
    public List<CollaborationService> getCollaborationServicesByCollaborationType(CollaborationType type) {
        log.debug("📋 Retrieving services with type: {}", type);
        List<CollaborationService> services = repository.findByCollaborationType(type);
        log.debug("✅ {} services found", services.size());
        return services;
    }

    // ========================================
    // READ - By activity domain
    // ========================================
    public List<CollaborationService> getCollaborationServicesByActivityDomain(ActivityDomain domain) {
        log.debug("📋 Retrieving services with domain: {}", domain);
        List<CollaborationService> services = repository.findByActivityDomain(domain);
        log.debug("✅ {} services found", services.size());
        return services;
    }

    // ========================================
    // UPDATE - With authorization check for approved services
    // ========================================
    @Transactional
    public CollaborationService updateCollaborationService(Long id, CollaborationService serviceDetails, String partnerEmail) {
        log.info("📝 Updating service ID: {} by: {}", id, partnerEmail);

        CollaborationService existingService = getCollaborationServiceById(id);
        log.info("📋 Existing service: {}", existingService.getName());
        log.info("📋 Current status: {}", existingService.getStatus());

        LocalPartner partner = localPartnerRepository.findByEmail(partnerEmail)
                .orElseThrow(() -> {
                    log.error("❌ Partner not found with email: {}", partnerEmail);
                    return new RuntimeException("Partner not found");
                });

        // Verify partner is the owner
        if (!existingService.getProvider().getId().equals(partner.getId())) {
            log.error("❌ Unauthorized modification attempt by: {}", partnerEmail);
            throw new RuntimeException("You can only modify your own services");
        }

        // ✅ AUTHORIZATION CHECK BY STATUS
        if (existingService.getStatus() == ServiceStatus.APPROVED) {
            // Approved service - needs authorization
            if (!existingService.isEditAuthorized()) {
                log.warn("⚠️ Attempt to modify APPROVED service without authorization");
                throw new RuntimeException(
                        "This service is approved. To modify it, you must request authorization from admin."
                );
            }
            log.info("✅ Modification authorized by admin until {}",
                    existingService.getEditAuthorizedUntil().toLocalDate());
        }
        else if (existingService.getStatus() == ServiceStatus.PENDING) {
            // Pending service - modification allowed without restriction
            log.info("✅ Modification of pending service authorized");
        }
        else if (existingService.getStatus() == ServiceStatus.REJECTED) {
            log.error("❌ Attempt to modify REJECTED service");
            throw new RuntimeException("Cannot modify a rejected service");
        }
        else {
            log.error("❌ Attempt to modify service with unknown status: {}", existingService.getStatus());
            throw new RuntimeException("Unknown service status");
        }

        // ✅ Save old state for notifications
        String oldName = existingService.getName();

        // Update fields
        if (serviceDetails.getName() != null) {
            existingService.setName(serviceDetails.getName());
            log.info("📝 Name updated: {}", serviceDetails.getName());
        }
        if (serviceDetails.getDescription() != null) {
            existingService.setDescription(serviceDetails.getDescription());
            log.info("📝 Description updated");
        }
        if (serviceDetails.getRegion() != null) {
            existingService.setRegion(serviceDetails.getRegion());
            log.info("📍 Region updated");
        }
        if (serviceDetails.getRequestedBudget() != null) {
            existingService.setRequestedBudget(serviceDetails.getRequestedBudget());
            log.info("💰 Budget updated: {}", serviceDetails.getRequestedBudget());
        }
        if (serviceDetails.getAvailability() != null) {
            existingService.setAvailability(serviceDetails.getAvailability());
            log.info("📅 Availability updated: {}", serviceDetails.getAvailability());
        }
        if (serviceDetails.getContactPerson() != null) {
            existingService.setContactPerson(serviceDetails.getContactPerson());
            log.info("👤 Contact updated: {}", serviceDetails.getContactPerson());
        }
        if (serviceDetails.getCollaborationType() != null) {
            existingService.setCollaborationType(serviceDetails.getCollaborationType());
            log.info("🤝 Collaboration type updated: {}", serviceDetails.getCollaborationType());
        }
        if (serviceDetails.getActivityDomain() != null) {
            existingService.setActivityDomain(serviceDetails.getActivityDomain());
            log.info("🏭 Activity domain updated: {}", serviceDetails.getActivityDomain());
        }
        if (serviceDetails.getExpectedBenefits() != null) {
            existingService.setExpectedBenefits(serviceDetails.getExpectedBenefits());
            log.info("💰 Expected benefits updated");
        }
        if (serviceDetails.getRequiredSkills() != null) {
            existingService.setRequiredSkills(serviceDetails.getRequiredSkills());
            log.info("📚 Required skills updated");
        }
        if (serviceDetails.getCollaborationDuration() != null) {
            existingService.setCollaborationDuration(serviceDetails.getCollaborationDuration());
            log.info("⏱️ Duration updated: {}", serviceDetails.getCollaborationDuration());
        }
        if (serviceDetails.getAddress() != null) {
            existingService.setAddress(serviceDetails.getAddress());
            log.info("📍 Address updated: {}", serviceDetails.getAddress());
        }
        if (serviceDetails.getDocuments() != null && !serviceDetails.getDocuments().isEmpty()) {
            // ✅ Supprimer les anciens documents (fichiers physiques + BD)
            for (CollaborationServiceDocument oldDoc : existingService.getDocuments()) {
                try {
                    fileStorageService.deleteFile(oldDoc.getFilePath(), FileStorageService.ServiceType.COLLABORATION);
                    log.info("🗑️ Ancien fichier supprimé: {}", oldDoc.getFilePath());
                } catch (Exception e) {
                    log.warn("⚠️ Impossible de supprimer l'ancien fichier: {}", e.getMessage());
                }
                documentRepository.delete(oldDoc);
            }

            existingService.getDocuments().clear();

            // Ajouter les nouveaux documents
            for (CollaborationServiceDocument doc : serviceDetails.getDocuments()) {
                CollaborationServiceDocument newDoc = new CollaborationServiceDocument();
                newDoc.setFileName(doc.getFileName());
                newDoc.setFileType(doc.getFileType());
                newDoc.setFileSize(doc.getFileSize());
                newDoc.setFilePath(doc.getFilePath());
                newDoc.setDownloadUrl(doc.getDownloadUrl());
                newDoc.setIsPrimary(doc.getIsPrimary());
                newDoc.setCollaborationService(existingService);
                documentRepository.save(newDoc);
                existingService.addDocument(newDoc);
            }
            log.info("📎 Documents remplacés avec succès");
        }

        // ✅ If it was an authorized modification, revoke authorization after use
        if (existingService.getStatus() == ServiceStatus.APPROVED && existingService.isEditAuthorized()) {
            existingService.setEditAuthorizedUntil(null);
            existingService.setAuthorizedByAdminId(null);
            log.info("🔓 Edit authorization revoked after use");

            // Delete associated requests
            List<CollaborationServiceRequest> approvedRequests = collaborationRequestRepository
                    .findByServiceIdAndStatus(id, ServiceStatus.APPROVED);

            for (CollaborationServiceRequest request : approvedRequests) {
                if (request.getRequestType() == RequestType.EDIT) {
                    collaborationRequestRepository.delete(request);
                    log.info("📝 Request ID: {} deleted after use", request.getId());
                }
            }
        }

        CollaborationService updated = repository.save(existingService);
        log.info("✅ Service updated successfully, ID: {}", updated.getId());

        // ============================================================
        // ✅ NOTIFICATIONS FOR MODIFICATION
        // ============================================================
        if (updated.getStatus() == ServiceStatus.APPROVED) {
            // 1. NOTIFICATION TO ADMIN
            String adminTitle = "✏️ Collaboration service modified";
            String adminMessage = String.format(
                    "Partner %s %s modified the service '%s'.\n" +
                            "Old name: %s | New name: %s",
                    partner.getFirstName(),
                    partner.getLastName(),
                    updated.getName(),
                    oldName,
                    updated.getName()
            );

            notificationService.createNotificationForRole(
                    adminTitle,
                    adminMessage,
                    Role.ADMIN,
                    updated.getId()
            );

            // 2. NOTIFICATION TO ECONOMIC PARTNERS
            String partnerTitle = "🔄 Collaboration service updated";
            String partnerMessage = String.format(
                    "The collaboration service '%s' has been updated.\n" +
                            "Budget: %s TND | Region: %s",
                    updated.getName(),
                    updated.getRequestedBudget(),
                    updated.getRegion() != null ? updated.getRegion().getName() : "N/A"
            );

            notificationService.createNotificationForRole(
                    partnerTitle,
                    partnerMessage,
                    Role.PARTNER,
                    updated.getId()
            );

            // 3. NOTIFICATION TO INTERNATIONAL COMPANIES
            String companyTitle = "🔄 Collaboration service updated";
            String companyMessage = String.format(
                    "The collaboration service '%s' has been updated.\n" +
                            "Budget: %s TND | Domain: %s",
                    updated.getName(),
                    updated.getRequestedBudget(),
                    updated.getActivityDomain() != null ? updated.getActivityDomain().toString() : "N/A"
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
    // DELETE - With authorization check for approved services
    // ========================================
    @Transactional
    public void deleteCollaborationService(Long id, String partnerEmail, boolean isAdmin) {
        log.info("🗑️ Deleting service ID: {}, by: {}, isAdmin: {}", id, partnerEmail, isAdmin);

        CollaborationService service = getCollaborationServiceById(id);
        String serviceName = service.getName();
        Long serviceId = service.getId();

        log.info("📋 Service to delete: {} (ID: {})", serviceName, id);
        log.info("📋 Service status: {}", service.getStatus());

        // Check associated requests
        List<CollaborationServiceRequest> relatedRequests = collaborationRequestRepository.findByServiceId(id);
        int requestCount = relatedRequests.size();

        if (requestCount > 0) {
            log.info("📋 This service has {} associated request(s)", requestCount);
        }
        boolean hasCompanyFavorites = service.getFavoritedByCompanies() != null && !service.getFavoritedByCompanies().isEmpty();
        boolean hasPartnerFavorites = service.getFavoritedByPartners() != null && !service.getFavoritedByPartners().isEmpty();

        if (hasCompanyFavorites) {
            log.info("🏢 Ce service est dans les favoris de {} société(s) internationale(s)", service.getFavoritedByCompanies().size());
        }
        if (hasPartnerFavorites) {
            log.info("🤝 Ce service est dans les favoris de {} partenaire(s) économique(s)", service.getFavoritedByPartners().size());
        }

        // ✅ ADMIN CASE - Direct deletion
        if (isAdmin) {
            log.info("👑 Deletion by ADMIN - authorized");

            // Notify local partner
            try {
                notificationService.notifyPartnerCollaborationServiceDeletedByAdmin(service);
            } catch (Exception e) {
                log.error("❌ Notification error: {}", e.getMessage());
            }

            cleanAllFavorites(service);
            cleanAllDocuments(service);

            if (!relatedRequests.isEmpty()) {
                log.info("📝 Deleting {} associated request(s)", relatedRequests.size());
                collaborationRequestRepository.deleteAll(relatedRequests);
            }


            repository.delete(service);
            log.info("✅ Service successfully deleted by admin");

            // Deletion notifications
            sendDeletionNotifications(serviceName, serviceId, null, true);
            return;
        }

        // ✅ LOCAL PARTNER CASE
        log.info("👤 Deletion by local partner");

        LocalPartner partner = localPartnerRepository.findByEmail(partnerEmail)
                .orElseThrow(() -> {
                    log.error("❌ Partner not found with email: {}", partnerEmail);
                    return new RuntimeException("Partner not found");
                });

        if (!service.getProvider().getId().equals(partner.getId())) {
            log.error("❌ Unauthorized deletion attempt");
            throw new RuntimeException("You can only delete your own services");
        }

        // ✅ AUTHORIZATION CHECK BY STATUS
        if (service.getStatus() == ServiceStatus.APPROVED) {
            if (!service.isDeleteAuthorized()) {
                log.warn("⚠️ Attempt to delete without authorization");
                throw new RuntimeException(
                        "This service is approved. To delete it, you must request authorization from admin."
                );
            }
            log.info("✅ Deletion authorized by admin");
        }
        else if (service.getStatus() == ServiceStatus.PENDING) {
            log.info("✅ Deletion of pending service authorized");
        }
        else if (service.getStatus() == ServiceStatus.REJECTED) {
            log.info("✅ Deletion of rejected service authorized");
        }
        cleanAllFavorites(service);
        cleanAllDocuments(service);

        // Delete associated requests
        if (!relatedRequests.isEmpty()) {
            log.info("📝 Deleting {} associated request(s)", relatedRequests.size());
            collaborationRequestRepository.deleteAll(relatedRequests);
        }

        repository.delete(service);
        log.info("✅ Service successfully deleted");

        // Deletion notifications
        sendDeletionNotifications(serviceName, serviceId, partner, false);
    }

    /**
     * Utility method to send deletion notifications
     */
    private void sendDeletionNotifications(String serviceName, Long serviceId, LocalPartner partner, boolean isAdmin) {
        // 1. NOTIFICATION TO ADMIN
        String adminTitle = "🗑️ Collaboration service deleted";
        String adminMessage;

        if (isAdmin) {
            adminMessage = String.format(
                    "You deleted the service '%s'.",
                    serviceName
            );
        } else {
            adminMessage = String.format(
                    "Partner %s %s deleted the service '%s' (authorization approved).",
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

        // 2. NOTIFICATION TO ECONOMIC PARTNERS
        String partnerTitle = "🗑️ Collaboration service deleted";
        String partnerMessage = String.format(
                "The collaboration service '%s' has been deleted and is no longer available.",
                serviceName
        );

        notificationService.createNotificationForRole(
                partnerTitle,
                partnerMessage,
                Role.PARTNER,
                serviceId
        );

        // 3. NOTIFICATION TO INTERNATIONAL COMPANIES
        String companyTitle = "🗑️ Collaboration service deleted";
        String companyMessage = String.format(
                "The collaboration service '%s' has been deleted and is no longer available.",
                serviceName
        );

        notificationService.createNotificationForRole(
                companyTitle,
                companyMessage,
                Role.INTERNATIONAL_COMPANY,
                serviceId
        );
    }

    // ========================================
    // ADMIN: Approve service
    // ========================================
    @Transactional
    public CollaborationService approveService(Long id) {
        CollaborationService service = getCollaborationServiceById(id);
        service.setStatus(ServiceStatus.APPROVED);
        CollaborationService approved = repository.save(service);

        notificationService.notifyLocalPartnerServiceApproved(approved);
        notificationService.notifyPartnersAndCompaniesNewOpportunity(approved);


        return approved;
    }

    // ========================================
    // ADMIN: Reject service
    // ========================================
    @Transactional
    public CollaborationService rejectService(Long id, String rejectionReason, String adminEmail) {
        log.info("🔴 ===== REJECT COLLABORATION SERVICE =====");
        log.info("🔴 ID: {}, Admin: {}", id, adminEmail);
        log.info("🔴 Reason: {}", rejectionReason);

        // ✅ 1. VALIDATE reason
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            log.error("❌ Rejection reason is missing");
            throw new RuntimeException("Rejection reason is required");
        }

        // ✅ 2. GET service
        CollaborationService service = getCollaborationServiceById(id);
        log.info("🔴 Service found: {}", service.getName());
        log.info("🔴 Current status: {}", service.getStatus());

        // ✅ 3. VERIFY service is PENDING
        if (service.getStatus() != ServiceStatus.PENDING) {
            log.error("❌ Attempt to reject a service that is not pending - Status: {}", service.getStatus());
            throw new RuntimeException("Only pending services can be rejected");
        }

        // ✅ 4. GET admin (optional - if you want to track who rejected)
        if (adminEmail != null && !adminEmail.isEmpty()) {
            adminRepository.findByEmail(adminEmail).ifPresent(admin -> {
                service.setRejectedByAdminId(admin.getId());
                log.info("🔴 Admin recorded: {}", admin.getEmail());
            });
        }

        // ✅ 5. UPDATE service with rejection information
        service.setStatus(ServiceStatus.REJECTED);
        service.setRejectionReason(rejectionReason);
        service.setRejectedAt(LocalDateTime.now());

        // ✅ 6. SAVE
        CollaborationService rejected = repository.save(service);
        log.info("🔴 Service rejected with ID: {}", rejected.getId());
        log.info("🔴 Reason saved: {}", rejected.getRejectionReason());

        // ✅ 7. NOTIFY local partner (you'll need to update this method too)
        log.info("🔴 Calling notifyLocalPartnerServiceRejected");
        notificationService.notifyLocalPartnerServiceRejected(rejected);

        log.info("🔴 ===== END REJECT =====");
        return rejected;
    }

    // ========================================
    // PART 1: REQUEST MANAGEMENT (LOCAL PARTNER) - SPÉCIFIQUE COLLABORATION
    // ========================================

    /**
     * Local partner: Request modification of an approved service (Collaboration)
     */
    @Transactional
    public CollaborationServiceRequest requestEdit(Long serviceId, String partnerEmail, String reason, String requestedChanges) {
        log.info("📝 EDIT REQUEST for collaboration service ID: {} by: {}", serviceId, partnerEmail);

        CollaborationService service = getCollaborationServiceById(serviceId);
        LocalPartner partner = localPartnerRepository.findByEmail(partnerEmail)
                .orElseThrow(() -> new RuntimeException("Partner not found"));

        // Verify partner is the owner
        if (!service.getProvider().getId().equals(partner.getId())) {
            throw new RuntimeException("You can only request for your own services");
        }

        // Verify service is approved
        if (service.getStatus() != ServiceStatus.APPROVED) {
            throw new RuntimeException("Only approved services can be modified");
        }

        // Verify no pending request already exists
        List<CollaborationServiceRequest> existingRequests = collaborationRequestRepository
                .findByServiceIdAndStatus(serviceId, ServiceStatus.PENDING);

        if (!existingRequests.isEmpty()) {
            throw new RuntimeException("A request is already pending for this service");
        }

        CollaborationServiceRequest request = new CollaborationServiceRequest(
                service, partner, RequestType.EDIT, reason, requestedChanges
        );

        CollaborationServiceRequest saved = collaborationRequestRepository.save(request);

        // NOTIFICATION: Admin (spécifique collaboration)
        notificationService.notifyAdminNewCollaborationRequest(saved);

        log.info("✅ Edit request created with ID: {}", saved.getId());
        return saved;
    }

    /**
     * Local partner: Request deletion of an approved service (Collaboration)
     */
    @Transactional
    public CollaborationServiceRequest requestDelete(Long serviceId, String partnerEmail, String reason) {
        log.info("🗑️ DELETE REQUEST for collaboration service ID: {} by: {}", serviceId, partnerEmail);

        CollaborationService service = getCollaborationServiceById(serviceId);
        LocalPartner partner = localPartnerRepository.findByEmail(partnerEmail)
                .orElseThrow(() -> new RuntimeException("Partner not found"));

        // Verify partner is the owner
        if (!service.getProvider().getId().equals(partner.getId())) {
            throw new RuntimeException("You can only request for your own services");
        }

        // Verify service is approved
        if (service.getStatus() != ServiceStatus.APPROVED) {
            throw new RuntimeException("Only approved services can be deleted");
        }

        // Verify no pending request already exists
        List<CollaborationServiceRequest> existingRequests = collaborationRequestRepository
                .findByServiceIdAndStatus(serviceId, ServiceStatus.PENDING);

        if (!existingRequests.isEmpty()) {
            throw new RuntimeException("A request is already pending for this service");
        }

        CollaborationServiceRequest request = new CollaborationServiceRequest(
                service, partner, RequestType.DELETE, reason, null
        );

        CollaborationServiceRequest saved = collaborationRequestRepository.save(request);

        // NOTIFICATION: Admin (spécifique collaboration)
        notificationService.notifyAdminNewCollaborationRequest(saved);

        log.info("✅ Delete request created with ID: {}", saved.getId());
        return saved;
    }

    /**
     * Get partner's requests (Collaboration)
     */
    public List<CollaborationServiceRequest> getPartnerRequests(Long partnerId) {
        log.debug("📋 Retrieving collaboration requests for partner ID: {}", partnerId);
        return collaborationRequestRepository.findByPartnerId(partnerId);
    }

    // ========================================
    // PART 2: ADMIN APPROVES REQUESTS (SPÉCIFIQUE COLLABORATION) - AVEC SUPPRESSION AUTOMATIQUE
    // ========================================

    /**
     * Admin: Approve an edit request (Collaboration) - La demande est supprimée après approbation
     */
    @Transactional
    public void approveEditRequest(Long requestId, String adminEmail) {
        log.info("🔐 Admin {} approves EDIT REQUEST for collaboration ID: {}", adminEmail, requestId);

        CollaborationServiceRequest request = collaborationRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getRequestType() != RequestType.EDIT) {
            throw new RuntimeException("This is not an edit request");
        }

        if (request.getStatus() != ServiceStatus.PENDING) {
            throw new RuntimeException("This request has already been processed");
        }

        Admin admin = adminRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        CollaborationService service = request.getService();

        // ✅ ACTIVATE EDIT AUTHORIZATION (7 days)
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(7);
        service.setEditAuthorizedUntil(expiryDate);
        service.setAuthorizedByAdminId(admin.getId());

        repository.save(service);

        // Sauvegarder les infos pour notification avant suppression
        String serviceName = service.getName();
        Long serviceId = service.getId();
        Long partnerId = request.getPartner().getId();
        String partnerEmail = request.getPartner().getEmail();

        // ❌ SUPPRIMER la demande (plus besoin de la garder)
        collaborationRequestRepository.delete(request);
        log.info("📝 Request ID: {} deleted after approval", requestId);

        // NOTIFICATION: Local partner (à adapter)
        try {
            // Vous devrez créer cette méthode dans NotificationService
            notificationService.notifyPartnerCollaborationEditApproved(request);
            log.info("📢 Notification sent to partner: {}", partnerEmail);
        } catch (Exception e) {
            log.error("❌ Error sending notification: {}", e.getMessage());
        }

        // NOTIFICATION: Admin (confirmation)
        String adminTitle = "✅ Edit request approved";
        String adminMessage = String.format(
                "You approved the edit request for service '%s'. The request has been deleted.",
                serviceName
        );
        notificationService.createNotificationForUser(adminTitle, adminMessage, Role.ADMIN, admin.getId(), serviceId);

        log.info("✅ Edit request approved and deleted - Service editable until {}", expiryDate.toLocalDate());
    }

    /**
     * Admin: Approve a delete request (Collaboration) - La demande est supprimée après approbation
     */
    @Transactional
    public void approveDeleteRequest(Long requestId, String adminEmail) {
        log.info("🔐 Admin {} approves DELETE REQUEST for collaboration ID: {}", adminEmail, requestId);

        CollaborationServiceRequest request = collaborationRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getRequestType() != RequestType.DELETE) {
            throw new RuntimeException("This is not a delete request");
        }

        if (request.getStatus() != ServiceStatus.PENDING) {
            throw new RuntimeException("This request has already been processed");
        }

        Admin admin = adminRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        CollaborationService service = request.getService();

        // ✅ ACTIVATE DELETE AUTHORIZATION (3 days)
        service.setDeleteAuthorized(true);
        service.setAuthorizedByAdminId(admin.getId());

        repository.save(service);

        // Sauvegarder les infos pour notification avant suppression
        String serviceName = service.getName();
        Long serviceId = service.getId();
        Long partnerId = request.getPartner().getId();
        String partnerEmail = request.getPartner().getEmail();

        // ❌ SUPPRIMER la demande (plus besoin de la garder)
        collaborationRequestRepository.delete(request);
        log.info("📝 Request ID: {} deleted after approval", requestId);

        // NOTIFICATION: Local partner (à adapter)
        try {
            // Vous devrez créer cette méthode dans NotificationService
            notificationService.notifyPartnerCollaborationDeleteApproved(request);
            log.info("📢 Notification sent to partner: {}", partnerEmail);
        } catch (Exception e) {
            log.error("❌ Error sending notification: {}", e.getMessage());
        }

        // NOTIFICATION: Admin (confirmation)
        String adminTitle = "✅ Delete request approved";
        String adminMessage = String.format(
                "You approved the delete request for service '%s'. The request has been deleted.",
                serviceName
        );
        notificationService.createNotificationForUser(adminTitle, adminMessage, Role.ADMIN, admin.getId(), serviceId);

        log.info("✅ Delete request approved and deleted - Service deletable until {}",
                LocalDateTime.now().plusDays(3).toLocalDate());
    }

    /**
     * Admin: Reject a request (Collaboration) with reason - La demande est supprimée après rejet
     */
    @Transactional
    public void rejectRequest(Long requestId, String adminEmail, String rejectionReason) {
        log.info("🔐 Admin {} rejects request ID: {} with reason: {}", adminEmail, requestId, rejectionReason);

        // ✅ 1. VALIDATION de la raison
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            log.error("❌ Rejection reason is missing");
            throw new RuntimeException("Rejection reason is required");
        }

        // ✅ 2. Récupération de la demande
        CollaborationServiceRequest request = collaborationRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        if (request.getStatus() != ServiceStatus.PENDING) {
            log.error("❌ Request already processed - Status: {}", request.getStatus());
            throw new RuntimeException("This request has already been processed");
        }

        // ✅ 3. Récupération de l'admin
        Admin admin = adminRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found"));

        // ✅ 4. Sauvegarder les informations pour la notification AVANT suppression
        String partnerEmail = request.getPartner().getEmail();
        String serviceName = request.getService().getName();
        Long serviceId = request.getService().getId();
        RequestType requestType = request.getRequestType();
        String initialReason = request.getReason();

        // ✅ 5. Définir les informations de rejet (pour la notification)
        request.setRejectionReason(rejectionReason);
        request.setRejectedAt(LocalDateTime.now());
        request.setRejectedByAdminId(admin.getId());
        request.setStatus(ServiceStatus.REJECTED);
        request.setResponseDate(LocalDateTime.now());
        request.setAdmin(admin);

        // ✅ 6. NOTIFICATION au partenaire local (AVANT suppression)
        try {
            notificationService.notifyPartnerCollaborationRequestRejected(request);
            log.info("📢 Notification sent to partner: {}", partnerEmail);
        } catch (Exception e) {
            log.error("❌ Error sending notification: {}", e.getMessage());
        }

        // ❌ 7. SUPPRIMER la demande
        collaborationRequestRepository.delete(request);
        log.info("📝 Request ID: {} deleted after rejection with reason: {}", requestId, rejectionReason);

        log.info("✅ Request rejected and deleted successfully");
    }
    // ========================================
    // PART 3: METHODS TO RETRIEVE REQUESTS (Collaboration)
    // ========================================

    public List<CollaborationServiceRequest> getPendingRequests() {
        return collaborationRequestRepository.findByStatus(ServiceStatus.PENDING);
    }

    public List<CollaborationServiceRequest> getRequestsByType(RequestType type) {
        return collaborationRequestRepository.findByRequestType(type);
    }

    public List<CollaborationServiceRequest> getRequestsByStatus(ServiceStatus status) {
        return collaborationRequestRepository.findByStatus(status);
    }

    public List<CollaborationServiceRequest> getRequestsByTypeAndStatus(RequestType type, ServiceStatus status) {
        return collaborationRequestRepository.findByTypeAndStatus(type, status);
    }

    public CollaborationServiceRequest getRequestById(Long requestId) {
        return collaborationRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));
    }

    public long getPendingRequestsCount() {
        return collaborationRequestRepository.countByStatus(ServiceStatus.PENDING);
    }

    public long getPendingEditRequestsCount() {
        return collaborationRequestRepository.countByTypeAndStatus(RequestType.EDIT, ServiceStatus.PENDING);
    }

    public long getPendingDeleteRequestsCount() {
        return collaborationRequestRepository.countByTypeAndStatus(RequestType.DELETE, ServiceStatus.PENDING);
    }

    public Map<String, Object> getRequestStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", collaborationRequestRepository.count());
        stats.put("pending", getPendingRequestsCount());
        stats.put("approved", collaborationRequestRepository.countByStatus(ServiceStatus.APPROVED));
        stats.put("rejected", collaborationRequestRepository.countByStatus(ServiceStatus.REJECTED));
        stats.put("editRequests", collaborationRequestRepository.countByType(RequestType.EDIT));
        stats.put("deleteRequests", collaborationRequestRepository.countByType(RequestType.DELETE));
        stats.put("pendingEdit", getPendingEditRequestsCount());
        stats.put("pendingDelete", getPendingDeleteRequestsCount());
        return stats;
    }

    // ========================================
    // SEARCH
    // ========================================
    public List<CollaborationService> searchCollaborationServices(String keyword) {
        log.info("🔍 Searching services with keyword: {}", keyword);
        return repository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword);
    }

    // ========================================
    // SCHEDULED TASK TO CLEAN EXPIRED AUTHORIZATIONS
    // ========================================
    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void cleanExpiredAuthorizations() {
        log.info("🧹 Cleaning expired authorizations");

        LocalDateTime now = LocalDateTime.now();

        List<CollaborationService> servicesWithExpiredEdit = repository
                .findByEditAuthorizedUntilBefore(now);

        for (CollaborationService service : servicesWithExpiredEdit) {
            service.setEditAuthorizedUntil(null);
            service.setAuthorizedByAdminId(null);
            log.info("⏰ Expired edit authorization for service ID: {}", service.getId());
        }
        repository.saveAll(servicesWithExpiredEdit);

        log.info("🧹 Cleaning completed - {} expired authorizations cleaned", servicesWithExpiredEdit.size());
    }

    // ========================================
    // DELETE - Rejected service (by local partner)
    // ========================================
    @Transactional
    public void deleteRejectedService(Long id, String partnerEmail) {
        log.info("🗑️ Deleting rejected service ID: {} by: {}", id, partnerEmail);

        CollaborationService service = getCollaborationServiceById(id);

        // Verify service is REJECTED
        if (service.getStatus() != ServiceStatus.REJECTED) {
            log.error("❌ Attempt to delete non-rejected service - Status: {}", service.getStatus());
            throw new RuntimeException("Only rejected services can be deleted directly");
        }

        LocalPartner partner = localPartnerRepository.findByEmail(partnerEmail)
                .orElseThrow(() -> new RuntimeException("Partner not found"));

        // Verify partner is the owner
        if (!service.getProvider().getId().equals(partner.getId())) {
            log.error("❌ Unauthorized deletion attempt - Partner {} is not the owner", partnerEmail);
            throw new RuntimeException("You can only delete your own services");
        }
        cleanAllDocuments(service);

        // Check relations before deletion
        List<CollaborationServiceRequest> relatedRequests = collaborationRequestRepository.findByServiceId(id);

        if (!relatedRequests.isEmpty()) {
            log.info("📝 {} request(s) associated with the service", relatedRequests.size());
            collaborationRequestRepository.deleteAll(relatedRequests);
        }

        // Delete the service
        repository.delete(service);
        log.info("✅ Rejected service successfully deleted by partner");
    }

    // ========================================
    // VALIDATION
    // ========================================
    private void validateRequiredFields(CollaborationService service) {
        log.debug("🔍 Validating required fields");

        if (service.getName() == null || service.getName().trim().isEmpty()) {
            log.error("❌ Validation failed: missing name");
            throw new RuntimeException("Name is required");
        }
        if (service.getRegion() == null) {
            log.error("❌ Validation failed: missing region");
            throw new RuntimeException("Region is required");
        }
        if (service.getRequestedBudget() == null) {
            log.error("❌ Validation failed: missing budget");
            throw new RuntimeException("Budget is required");
        }
        if (service.getAvailability() == null) {
            log.error("❌ Validation failed: missing availability");
            throw new RuntimeException("Availability is required");
        }
        if (service.getContactPerson() == null || service.getContactPerson().trim().isEmpty()) {
            log.error("❌ Validation failed: missing contact person");
            throw new RuntimeException("Contact person is required");
        }

        log.debug("✅ Validation successful");
    }

    // ========================================
// PART 4: CANCEL REQUEST (LOCAL PARTNER)
// ========================================

    /**
     * Local partner: Cancel a pending request
     * La demande est automatiquement supprimée
     */
    @Transactional
    public void cancelRequest(Long requestId, String partnerEmail) {
        log.info("🗑️ PARTNER {} cancels request ID: {}", partnerEmail, requestId);

        // Récupérer la demande
        CollaborationServiceRequest request = collaborationRequestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.error("❌ Request not found with id: {}", requestId);
                    return new RuntimeException("Request not found with id: " + requestId);
                });

        log.info("📋 Request details - Type: {}, Status: {}",
                request.getRequestType(), request.getStatus());

        // Vérifier que la demande est bien en attente
        if (request.getStatus() != ServiceStatus.PENDING) {
            log.error("❌ Cannot cancel request with status: {}", request.getStatus());
            throw new RuntimeException("Only pending requests can be cancelled");
        }

        // Vérifier que le partenaire est bien le propriétaire
        if (!request.getPartner().getEmail().equals(partnerEmail)) {
            log.error("❌ Unauthorized cancellation attempt - Partner {} is not the owner", partnerEmail);
            throw new RuntimeException("You can only cancel your own requests");
        }

        // Sauvegarder les infos pour notification avant suppression
        String serviceName = request.getService().getName();
        Long serviceId = request.getService().getId();
        RequestType requestType = request.getRequestType();
        Long partnerId = request.getPartner().getId();

        // ❌ SUPPRIMER DIRECTEMENT LA DEMANDE
        collaborationRequestRepository.delete(request);
        log.info("✅ Request ID: {} cancelled and deleted", requestId);

        // NOTIFICATION: Confirmation au partenaire
        String partnerTitle = "✅ Request cancelled";
        String partnerMessage = String.format(
                "Your %s request for service '%s' has been cancelled.",
                requestType == RequestType.EDIT ? "modification" : "deletion",
                serviceName
        );

        try {
            notificationService.createNotificationForUser(
                    partnerTitle,
                    partnerMessage,
                    Role.PARTNER,
                    partnerId,
                    serviceId
            );
            log.info("📢 Notification sent to partner ID: {}", partnerId);
        } catch (Exception e) {
            log.error("❌ Error sending partner notification: {}", e.getMessage());
        }

        // NOTIFICATION: Admin (info)
        String adminTitle = "🗑️ Request cancelled by partner";
        String adminMessage = String.format(
                "Partner cancelled their %s request for service '%s' (Request ID: %d)",
                requestType == RequestType.EDIT ? "modification" : "deletion",
                serviceName,
                requestId
        );

        try {
            notificationService.createNotificationForRole(
                    adminTitle,
                    adminMessage,
                    Role.ADMIN,
                    serviceId
            );
            log.info("📢 Notification sent to admins");
        } catch (Exception e) {
            log.error("❌ Error sending admin notification: {}", e.getMessage());
        }

        log.info("✅ Request cancellation completed successfully");
    }

    private void cleanAllDocuments(CollaborationService service) {
        if (service.getDocuments() != null && !service.getDocuments().isEmpty()) {
            log.info("📎 Nettoyage de {} documents", service.getDocuments().size());

            // Supprimer les fichiers physiques
            for (CollaborationServiceDocument doc : service.getDocuments()) {
                try {
                    fileStorageService.deleteFile(doc.getFilePath(), FileStorageService.ServiceType.COLLABORATION);
                    log.info("🗑️ Fichier supprimé: {}", doc.getFilePath());
                } catch (Exception e) {
                    log.warn("⚠️ Impossible de supprimer le fichier: {}", e.getMessage());
                }
            }

            // Vider la liste (les entités seront supprimées en cascade)
            service.getDocuments().clear();
        }
    }
    @Transactional
    public CollaborationService addDocumentsToService(Long serviceId, List<CollaborationServiceDocument> documents) {
        log.info("📎 Ajout de {} documents au service ID: {}", documents.size(), serviceId);

        CollaborationService service = getCollaborationServiceById(serviceId);

        for (CollaborationServiceDocument doc : documents) {
            doc.setCollaborationService(service);
            documentRepository.save(doc);
            service.addDocument(doc);
        }

        CollaborationService updated = repository.save(service);
        log.info("✅ Documents ajoutés avec succès");

        return updated;
    }
    @Transactional
    public CollaborationService replaceDocuments(Long serviceId, List<CollaborationServiceDocument> newDocuments) {
        log.info("🔄 Remplacement des documents pour le service ID: {}", serviceId);

        CollaborationService service = getCollaborationServiceById(serviceId);

        // Supprimer les anciens documents (physiques + BD)
        for (CollaborationServiceDocument oldDoc : service.getDocuments()) {
            try {
                fileStorageService.deleteFile(oldDoc.getFilePath(), FileStorageService.ServiceType.COLLABORATION);
                log.info("🗑️ Fichier supprimé: {}", oldDoc.getFilePath());
            } catch (Exception e) {
                log.warn("⚠️ Impossible de supprimer le fichier: {}", e.getMessage());
            }
            documentRepository.delete(oldDoc);
        }

        // Vider la liste des documents
        service.getDocuments().clear();

        // Ajouter les nouveaux documents
        for (CollaborationServiceDocument doc : newDocuments) {
            doc.setCollaborationService(service);
            CollaborationServiceDocument savedDoc = documentRepository.save(doc);
            service.addDocument(savedDoc);
        }

        CollaborationService updated = repository.save(service);
        log.info("✅ Documents remplacés avec succès - {} nouveaux documents", newDocuments.size());

        return updated;
    }

    /**
     * ✅ AJOUT : Récupérer un document par son ID
     */
    public CollaborationServiceDocument getDocumentById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document non trouvé avec id: " + documentId));
    }

    /**
     * ✅ AJOUT : Supprimer un document spécifique
     */
    @Transactional
    public void deleteDocument(Long documentId) {
        log.info("🗑️ Suppression du document ID: {}", documentId);

        CollaborationServiceDocument document = getDocumentById(documentId);

        // Détacher du service
        CollaborationService service = document.getCollaborationService();
        if (service != null) {
            service.getDocuments().remove(document);
            repository.save(service);
        }

        // Supprimer le fichier physique
        try {
            fileStorageService.deleteFile(document.getFilePath(), FileStorageService.ServiceType.COLLABORATION);
            log.info("🗑️ Fichier physique supprimé: {}", document.getFilePath());
        } catch (Exception e) {
            log.warn("⚠️ Impossible de supprimer le fichier physique: {}", e.getMessage());
        }

        // Supprimer le document
        documentRepository.delete(document);
        log.info("✅ Document supprimé avec succès");
    }
    // ========================================
// Ajouter un document au service
// ========================================
    @Transactional
    public CollaborationServiceDocument addDocumentToService(Long serviceId, CollaborationServiceDocument document) {
        log.info("📎 Ajout d'un document au service ID: {}", serviceId);

        CollaborationService service = getCollaborationServiceById(serviceId);

        // Si c'est le premier document, le marquer comme principal
        if (service.getDocuments().isEmpty()) {
            document.setIsPrimary(true);
        }

        document.setCollaborationService(service);
        CollaborationServiceDocument savedDoc = documentRepository.save(document);
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

        CollaborationService service = getCollaborationServiceById(serviceId);

        // ✅ 1. Révoquer l'autorisation
        service.setEditAuthorizedUntil(null);
        service.setAuthorizedByAdminId(null);
        repository.save(service);
        log.info("✅ Autorisation de modification révoquée pour service ID: {}", serviceId);

        // ✅ 2. Mettre à jour la demande associée
        List<CollaborationServiceRequest> approvedRequests = collaborationRequestRepository
                .findByServiceIdAndStatus(serviceId, ServiceStatus.APPROVED);

        for (CollaborationServiceRequest request : approvedRequests) {
            if (request.getRequestType() == RequestType.EDIT) {
                request.setStatus(ServiceStatus.REJECTED); // Marquer comme utilisée
                request.setExecutionDate(LocalDateTime.now());
                collaborationRequestRepository.save(request);
                log.info("📝 Demande ID: {} marquée comme utilisée", request.getId());
            }
        }
    }
    /**
     * ✅ Méthode utilitaire pour nettoyer tous les favoris d'un service de collaboration
     */
    private void cleanAllFavorites(CollaborationService service) {
        log.info(" Nettoyage des favoris pour le service de collaboration ID: {}", service.getId());

        // 1. Nettoyer les favoris des sociétés internationales
        if (service.getFavoritedByCompanies() != null && !service.getFavoritedByCompanies().isEmpty()) {
            int companyCount = service.getFavoritedByCompanies().size();
            log.info(" Retrait du service des favoris de {} société(s) internationale(s)", companyCount);

            for (internationalcompany company : new ArrayList<>(service.getFavoritedByCompanies())) {
                company.getFavoriteCollaborationServices().remove(service);
            }
            service.getFavoritedByCompanies().clear();
            log.info(" Favoris des sociétés internationales nettoyés");
        }

        // 2. Nettoyer les favoris des partenaires économiques
        if (service.getFavoritedByPartners() != null && !service.getFavoritedByPartners().isEmpty()) {
            int partnerCount = service.getFavoritedByPartners().size();
            log.info(" Retrait du service des favoris de {} partenaire(s) économique(s)", partnerCount);

            for (EconomicPartner partner : new ArrayList<>(service.getFavoritedByPartners())) {
                partner.getFavoriteCollaborationServices().remove(service);
            }
            service.getFavoritedByPartners().clear();
            log.info(" Favoris des partenaires économiques nettoyés");
        }
    }
}