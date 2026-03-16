package tn.iset.investplatformpfe.Service;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Entity.TouristService;
import tn.iset.investplatformpfe.Repository.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class TouristServiceService {

    private static final Logger log = LoggerFactory.getLogger(TouristServiceService.class);

    private final TouristServiceRepository touristServiceRepository;
    private final LocalPartnerRepository localPartnerRepository;
    private final NotificationService notificationService;
    private final TouristServiceRequestRepository touristServiceRequestRepository;
    private final AdminRepository adminRepository;
    private final TouristServiceDocumentRepository documentRepository;
    private final FileStorageService fileStorageService;
    private final FavoriteTouristService favoriteTouristService;


    public TouristServiceService(
            TouristServiceRepository touristServiceRepository,
            LocalPartnerRepository localPartnerRepository,
            NotificationService notificationService,
            TouristServiceRequestRepository touristServiceRequestRepository,
            AdminRepository adminRepository,
            TouristServiceDocumentRepository documentRepository,
            FileStorageService fileStorageService,
            FavoriteTouristService favoriteTouristService) {
        this.touristServiceRepository = touristServiceRepository;
        this.localPartnerRepository = localPartnerRepository;
        this.notificationService = notificationService;
        this.touristServiceRequestRepository = touristServiceRequestRepository;
        this.adminRepository = adminRepository;
        this.documentRepository = documentRepository;
        this.fileStorageService = fileStorageService;
        this.favoriteTouristService = favoriteTouristService;
    }

    // ================ Méthodes pour tous ================

    // ✅ GET services approuvés (pour les touristes - PUBLIC)
    public List<TouristService> getApprovedServices() {
        return touristServiceRepository.findByStatus(ServiceStatus.APPROVED);
    }

    // ================ Méthodes pour Admin ================

    // ✅ GET tous les services (pour admin)
    public List<TouristService> getAllServices() {
        return touristServiceRepository.findAll();
    }

    // ✅ GET services en attente (pour admin)
    public List<TouristService> getPendingServices() {
        return touristServiceRepository.findByStatus(ServiceStatus.PENDING);
    }

    // ✅ PUT approuver un service (pour admin)
    public TouristService approveService(Long id) {
        TouristService service = getServiceById(id);

        if (service.getStatus() != ServiceStatus.PENDING) {
            throw new RuntimeException("Only PENDING services can be approved. Current status: " + service.getStatus());
        }

        service.setStatus(ServiceStatus.APPROVED);
        TouristService saved = touristServiceRepository.save(service);

        notificationService.notifyLocalPartnerTouristApproved(saved);
        notificationService.notifyTouristsNewService(saved);

        return saved;
    }

    // ✅ PUT rejeter un service (pour admin)
    public TouristService rejectService(Long id, String rejectionReason, String adminEmail) {
        log.info("🔴 ===== REJECT TOURIST SERVICE =====");
        log.info("🔴 ID: {}, Admin: {}", id, adminEmail);
        log.info("🔴 Reason: {}", rejectionReason);

        // ✅ 1. VALIDATE reason
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            log.error("❌ Rejection reason is missing");
            throw new RuntimeException("Rejection reason is required");
        }

        // ✅ 2. GET service
        TouristService service = getServiceById(id);
        log.info("🔴 Service found: {}", service.getName());
        log.info("🔴 Current status: {}", service.getStatus());

        // ✅ 3. VERIFY service is PENDING
        if (service.getStatus() != ServiceStatus.PENDING) {
            log.error("❌ Attempt to reject a service that is not pending - Status: {}", service.getStatus());
            throw new RuntimeException("Only PENDING services can be rejected. Current status: " + service.getStatus());
        }

        // ✅ 4. GET admin (optional - for tracking)
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
        TouristService saved = touristServiceRepository.save(service);
        log.info("🔴 Service rejected with ID: {}", saved.getId());
        log.info("🔴 Reason saved: {}", saved.getRejectionReason());

        // ✅ 7. NOTIFY local partner
        log.info("🔴 Calling notifyLocalPartnerTouristRejected");
        notificationService.notifyLocalPartnerTouristRejected(saved);

        log.info("🔴 ===== END REJECT =====");
        return saved;
    }

    // ================ Méthodes pour Local Partners ================

    // ✅ GET tous les services d'un partenaire par email
    public List<TouristService> getServicesByProviderEmail(String email) {
        LocalPartner provider = localPartnerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Provider not found with email: " + email));
        return touristServiceRepository.findByProviderId(provider.getId());
    }

    // ✅ GET service par ID avec vérification propriétaire
    public TouristService getServiceByIdForPartner(Long id, String userEmail) {
        TouristService service = getServiceById(id);

        if (service.getProvider() == null || !service.getProvider().getEmail().equals(userEmail)) {
            throw new RuntimeException("You are not authorized to view this service");
        }

        return service;
    }

    // ✅ GET services rejetés par email du provider
    public List<TouristService> getRejectedServicesByProviderEmail(String email) {
        LocalPartner provider = localPartnerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Provider not found with email: " + email));
        return touristServiceRepository.findByProviderIdAndStatus(provider.getId(), ServiceStatus.REJECTED);
    }

    // ✅ GET services par provider avec vérification
    public List<TouristService> getTouristServicesByProvider(Long providerId, String userEmail) {
        LocalPartner provider = localPartnerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found with id: " + providerId));

        if (!provider.getEmail().equals(userEmail)) {
            throw new RuntimeException("You can only view your own services");
        }

        return touristServiceRepository.findByProviderId(providerId);
    }

    // ✅ GET services rejetés par provider (sans vérification - pour usage interne)
    public List<TouristService> getRejectedServicesByProvider(Long providerId) {
        return touristServiceRepository.findByProviderIdAndStatus(providerId, ServiceStatus.REJECTED);
    }

    // ✅ POST créer un service avec notification (avec email)
    public TouristService createServiceWithNotification(TouristService service, String userEmail) {
        LocalPartner provider = localPartnerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Provider not found with email: " + userEmail));
        service.setProvider(provider);

        service.setStatus(ServiceStatus.PENDING);
        service.setCreatedAt(LocalDateTime.now());
        List<TouristServiceDocument> documents = service.getDocuments();
        service.setDocuments(new ArrayList<>());

        TouristService saved = touristServiceRepository.save(service);
        if (documents != null && !documents.isEmpty()) {
            for (TouristServiceDocument doc : documents) {
                doc.setTouristService(saved);
                doc.setId(null); // S'assurer que c'est une nouvelle entité
                documentRepository.save(doc);
                saved.addDocument(doc);
            }
            saved = touristServiceRepository.save(saved);
        }

        notificationService.notifyAdminNewTouristService(saved);
        return saved;
    }

    // ✅ POST créer un service (sans notification - version simplifiée)
    public TouristService createService(TouristService service) {
        validateAndSetProvider(service);
        service.setStatus(ServiceStatus.PENDING);
        service.setCreatedAt(LocalDateTime.now());
        return touristServiceRepository.save(service);
    }

    // ================ GESTION DES REQUÊTES (EDIT/DELETE) ================

    // ✅ Partenaire: Demander la modification d'un service APPROUVÉ
    @Transactional
    public TouristServiceRequest requestEdit(Long serviceId, String partnerEmail, String reason, String requestedChanges) {
        log.info("📝 EDIT REQUEST for tourist service ID: {} by: {}", serviceId, partnerEmail);

        // Vérification que la raison n'est pas vide
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("Reason is required for edit request. Please explain why you want to modify this service.");
        }

        if (requestedChanges == null || requestedChanges.trim().isEmpty()) {
            throw new RuntimeException("Requested changes are required. Please describe what changes you want to make.");
        }

        TouristService service = touristServiceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Tourist service not found with id: " + serviceId));

        LocalPartner partner = localPartnerRepository.findByEmail(partnerEmail)
                .orElseThrow(() -> new RuntimeException("Partner not found with email: " + partnerEmail));

        if (!service.getProvider().getId().equals(partner.getId())) {
            throw new RuntimeException("You can only request for your own services");
        }

        if (service.getStatus() != ServiceStatus.APPROVED) {
            throw new RuntimeException("Only approved services can be modified. Current status: " + service.getStatus());
        }

        boolean hasPendingRequest = touristServiceRequestRepository.existsByServiceIdAndStatus(serviceId, ServiceStatus.PENDING);
        if (hasPendingRequest) {
            throw new RuntimeException("A request is already pending for this service. Please wait for admin response.");
        }

        TouristServiceRequest request = new TouristServiceRequest(
                service, partner, RequestType.EDIT, reason, requestedChanges
        );

        TouristServiceRequest saved = touristServiceRequestRepository.save(request);

        notificationService.notifyAdminNewTouristRequest(saved);
        log.info("📢 Notification envoyée à l'admin pour demande EDIT ID: {}", saved.getId());

        return saved;
    }

    // ✅ Partenaire: Demander la suppression d'un service APPROUVÉ
    @Transactional
    public TouristServiceRequest requestDelete(Long serviceId, String partnerEmail, String reason) {
        log.info("🗑️ DELETE REQUEST for tourist service ID: {} by: {}", serviceId, partnerEmail);

        // Vérification que la raison n'est pas vide
        if (reason == null || reason.trim().isEmpty()) {
            throw new RuntimeException("Reason is required for delete request. Please explain why you want to delete this service.");
        }

        TouristService service = touristServiceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Tourist service not found with id: " + serviceId));

        LocalPartner partner = localPartnerRepository.findByEmail(partnerEmail)
                .orElseThrow(() -> new RuntimeException("Partner not found with email: " + partnerEmail));

        if (!service.getProvider().getId().equals(partner.getId())) {
            throw new RuntimeException("You can only request for your own services");
        }

        if (service.getStatus() != ServiceStatus.APPROVED) {
            throw new RuntimeException("Only approved services can be deleted. Current status: " + service.getStatus());
        }

        boolean hasPendingRequest = touristServiceRequestRepository.existsByServiceIdAndStatus(serviceId, ServiceStatus.PENDING);
        if (hasPendingRequest) {
            throw new RuntimeException("A request is already pending for this service. Please wait for admin response.");
        }

        TouristServiceRequest request = new TouristServiceRequest(
                service, partner, RequestType.DELETE, reason, null
        );

        TouristServiceRequest saved = touristServiceRequestRepository.save(request);

        notificationService.notifyAdminNewTouristRequest(saved);
        log.info("📢 Notification envoyée à l'admin pour demande DELETE ID: {}", saved.getId());

        return saved;
    }

    // ✅ Partenaire: Annuler sa propre demande
    @Transactional
    public void cancelOwnRequest(Long requestId, String partnerEmail) {
        log.info("🗑️ Partner {} cancels request ID: {}", partnerEmail, requestId);

        TouristServiceRequest request = touristServiceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));

        // Vérifier que la demande appartient au partenaire
        if (!request.getPartner().getEmail().equals(partnerEmail)) {
            throw new RuntimeException("You can only cancel your own requests");
        }

        // Vérifier que la demande est encore en PENDING
        if (request.getStatus() != ServiceStatus.PENDING) {
            throw new RuntimeException("This request has already been processed and cannot be cancelled");
        }

        // ❌ SUPPRIMER DIRECTEMENT LA DEMANDE
        touristServiceRequestRepository.delete(request);
        log.info("✅ Request ID: {} cancelled and deleted by partner", requestId);

        // Notification à l'admin que la demande a été annulée
        String adminTitle = "📋 Request cancelled by partner";
        String adminMessage = String.format(
                "Partner %s %s cancelled their %s request for service '%s'.",
                request.getPartner().getFirstName(),
                request.getPartner().getLastName(),
                request.getRequestType() == RequestType.EDIT ? "edit" : "delete",
                request.getService().getName()
        );
        notificationService.createNotificationForRole(adminTitle, adminMessage, Role.ADMIN, request.getService().getId());
    }

    // ================ ADMIN: Gestion des demandes ================

    // ✅ Admin: Approuver une demande de modification
    @Transactional
    public void approveEditRequest(Long requestId, String adminEmail) {
        log.info("🔐 Admin {} approves EDIT REQUEST ID: {}", adminEmail, requestId);

        TouristServiceRequest request = touristServiceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));

        if (request.getRequestType() != RequestType.EDIT) {
            throw new RuntimeException("This is not an edit request. Request type: " + request.getRequestType());
        }

        if (request.getStatus() != ServiceStatus.PENDING) {
            throw new RuntimeException("This request has already been processed. Current status: " + request.getStatus());
        }

        Admin admin = adminRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found with email: " + adminEmail));

        TouristService service = request.getService();

        LocalDateTime expiryDate = LocalDateTime.now().plusDays(7);
        service.setEditAuthorizedUntil(expiryDate);
        service.setAuthorizedByAdminId(admin.getId());

        touristServiceRepository.save(service);

        // Sauvegarder les infos pour notification avant suppression
        String serviceName = service.getName();
        Long serviceId = service.getId();
        String partnerEmail = request.getPartner().getEmail();
        String partnerFirstName = request.getPartner().getFirstName();
        String partnerLastName = request.getPartner().getLastName();

        // ❌ SUPPRIMER LA DEMANDE APRÈS APPROBATION
        touristServiceRequestRepository.delete(request);
        log.info("✅ Request ID: {} deleted after approval", requestId);

        // Notifications
        String partnerTitle = "✅ Edit request approved";
        String partnerMessage = String.format(
                "Your edit request for service '%s' has been approved by the administrator.\n" +
                        "You can modify this service until %s.",
                serviceName,
                expiryDate.toLocalDate().toString()
        );
        notificationService.createNotificationForUser(partnerTitle, partnerMessage, Role.LOCAL_PARTNER,
                request.getPartner().getId(), serviceId);

               log.info("✅ Edit request approved and deleted - Service editable until {}", expiryDate.toLocalDate());
    }

    // ✅ Admin: Approuver une demande de suppression
    @Transactional
    public void approveDeleteRequest(Long requestId, String adminEmail) {
        log.info("🔐 Admin {} approves DELETE REQUEST ID: {}", adminEmail, requestId);

        TouristServiceRequest request = touristServiceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));

        if (request.getRequestType() != RequestType.DELETE) {
            throw new RuntimeException("This is not a delete request. Request type: " + request.getRequestType());
        }

        if (request.getStatus() != ServiceStatus.PENDING) {
            throw new RuntimeException("This request has already been processed. Current status: " + request.getStatus());
        }

        Admin admin = adminRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found with email: " + adminEmail));

        TouristService service = request.getService();

        service.setDeleteAuthorized(true);
        service.setAuthorizedByAdminId(admin.getId());

        touristServiceRepository.save(service);

        // Sauvegarder les infos pour notification avant suppression
        String serviceName = service.getName();
        Long serviceId = service.getId();
        String partnerEmail = request.getPartner().getEmail();
        String partnerFirstName = request.getPartner().getFirstName();
        String partnerLastName = request.getPartner().getLastName();

        // ❌ SUPPRIMER LA DEMANDE APRÈS APPROBATION
        touristServiceRequestRepository.delete(request);
        log.info("✅ Request ID: {} deleted after approval", requestId);

        // Notifications
        String partnerTitle = "✅ Delete request approved";
        String partnerMessage = String.format(
                "Your delete request for service '%s' has been approved by the administrator.\n" +
                        "You can now delete this service.",
                serviceName
        );
        notificationService.createNotificationForUser(partnerTitle, partnerMessage, Role.LOCAL_PARTNER,
                request.getPartner().getId(), serviceId);

        String adminTitle = "✅ Delete request approved";
        String adminMessage = String.format(
                "You approved the delete request for service '%s'.\n" +
                        "Partner %s %s can now delete this service.",
                serviceName,
                partnerFirstName,
                partnerLastName
        );
        notificationService.createNotificationForUser(adminTitle, adminMessage, Role.ADMIN, admin.getId(), serviceId);
    }

    // ✅ Admin: Rejeter une demande (Tourist Service)
    @Transactional
    public void rejectRequest(Long requestId, String adminEmail, String rejectionReason) {
        log.info("🔐 Admin {} rejects tourist service request ID: {} with reason: {}", adminEmail, requestId, rejectionReason);

        // Validation de la raison
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            log.error("❌ Rejection reason is missing");
            throw new RuntimeException("Rejection reason is required. Please explain why you are rejecting this request.");
        }

        // Récupération de la demande
        TouristServiceRequest request = touristServiceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));

        if (request.getStatus() != ServiceStatus.PENDING) {
            log.error("❌ Request already processed - Status: {}", request.getStatus());
            throw new RuntimeException("This request has already been processed. Current status: " + request.getStatus());
        }

        // Récupération de l'admin
        Admin admin = adminRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin not found with email: " + adminEmail));

        TouristService service = request.getService();

        // Sauvegarder les infos pour notification avant suppression
        String serviceName = service.getName();
        Long serviceId = service.getId();
        String partnerEmail = request.getPartner().getEmail();
        String partnerFirstName = request.getPartner().getFirstName();
        String partnerLastName = request.getPartner().getLastName();
        RequestType requestType = request.getRequestType();
        String initialReason = request.getReason();

        // ✅ Date de rejet
        LocalDateTime rejectedAt = LocalDateTime.now();

        // ❌ SUPPRIMER DIRECTEMENT LA DEMANDE
        touristServiceRequestRepository.delete(request);
        log.info("✅ Request ID: {} deleted after rejection", requestId);

        // ✅ NOTIFICATION au partenaire (message détaillé)
        String type = requestType == RequestType.EDIT ? "modification" : "deletion";
        String title = "❌ " + (requestType == RequestType.EDIT ? "Edit" : "Delete") + " Request Rejected - Tourist Service";

        String message = String.format(
                "Dear %s %s,\n\n" +
                        "Your %s request for tourist service '%s' (ID: %d) has been rejected by the administrator.\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "❌ REJECTION REASON :\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "%s\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "📅 Rejection Date : %s\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                        "Your initial request reason : %s\n\n" +
                        "If you wish to submit a new request, please take the above comments into consideration.\n\n" +
                        "Best regards,\n" +
                        "The Tourism Platform Team",
                partnerFirstName,
                partnerLastName,
                type,
                serviceName,
                serviceId,
                rejectionReason,
                rejectedAt.toLocalDate().toString(),
                initialReason != null ? initialReason : "Not specified"
        );

        notificationService.createNotificationForUser(
                title,
                message,
                Role.LOCAL_PARTNER,
                request.getPartner().getId(),
                serviceId
        );

        log.info("✅ Notification sent to partner {} for rejected {} request: {}",
                partnerEmail, type, rejectionReason);
    }

    // ================ QUERY METHODS POUR LES DEMANDES ================

    public List<TouristServiceRequest> getPartnerRequests(Long partnerId) {
        return touristServiceRequestRepository.findByPartnerId(partnerId);
    }

    public List<TouristServiceRequest> getAllRequests() {
        return touristServiceRequestRepository.findAll();
    }

    public List<TouristServiceRequest> getPendingRequests() {
        return touristServiceRequestRepository.findByStatus(ServiceStatus.PENDING);
    }

    public List<TouristServiceRequest> getRequestsByType(RequestType type) {
        return touristServiceRequestRepository.findByRequestType(type);
    }

    public List<TouristServiceRequest> getRequestsByStatus(ServiceStatus status) {
        return touristServiceRequestRepository.findByStatus(status);
    }

    public List<TouristServiceRequest> getRequestsByTypeAndStatus(RequestType type, ServiceStatus status) {
        return touristServiceRequestRepository.findByRequestTypeAndStatus(type, status);
    }

    public TouristServiceRequest getRequestById(Long requestId) {
        return touristServiceRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found with id: " + requestId));
    }

    public long getPendingRequestsCount() {
        return touristServiceRequestRepository.countByStatus(ServiceStatus.PENDING);
    }

    public long getPendingEditRequestsCount() {
        return touristServiceRequestRepository.countByRequestTypeAndStatus(RequestType.EDIT, ServiceStatus.PENDING);
    }

    public long getPendingDeleteRequestsCount() {
        return touristServiceRequestRepository.countByRequestTypeAndStatus(RequestType.DELETE, ServiceStatus.PENDING);
    }

    public Map<String, Object> getRequestStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", touristServiceRequestRepository.count());
        stats.put("pending", getPendingRequestsCount());
        stats.put("approved", 0L); // Les demandes approuvées sont supprimées
        stats.put("rejected", 0L); // Les demandes rejetées sont supprimées
        stats.put("editRequests", touristServiceRequestRepository.countByRequestType(RequestType.EDIT));
        stats.put("deleteRequests", touristServiceRequestRepository.countByRequestType(RequestType.DELETE));
        stats.put("pendingEdit", getPendingEditRequestsCount());
        stats.put("pendingDelete", getPendingDeleteRequestsCount());
        return stats;
    }

    // ================ SCHEDULED TASK ================

    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void cleanExpiredAuthorizations() {
        log.info("🧹 Cleaning expired authorizations for tourist services");

        LocalDateTime now = LocalDateTime.now();
        List<TouristService> services = touristServiceRepository.findAll();
        int cleanedCount = 0;

        for (TouristService service : services) {
            if (service.getEditAuthorizedUntil() != null && service.getEditAuthorizedUntil().isBefore(now)) {
                service.setEditAuthorizedUntil(null);
                service.setAuthorizedByAdminId(null);
                cleanedCount++;
                log.info("⏰ Expired edit authorization for tourist service ID: {}", service.getId());
            }
        }

        touristServiceRepository.saveAll(services);
        log.info("🧹 Cleaning completed - {} expired authorizations cleaned", cleanedCount);
    }

    // ================ Méthodes utilitaires ================

    public TouristService getServiceById(Long id) {
        return touristServiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found with id: " + id));
    }

    private void validateAndSetProvider(TouristService service) {
        if (service.getProvider() != null && service.getProvider().getId() != null) {
            LocalPartner provider = localPartnerRepository.findById(service.getProvider().getId())
                    .orElseThrow(() -> new RuntimeException("Provider not found with id: " + service.getProvider().getId()));
            service.setProvider(provider);
        } else {
            throw new RuntimeException("Provider is required");
        }
    }

    // ✅ PUT mettre à jour un service (avec vérification email)
    public TouristService updateService(Long id, TouristService updated, String userEmail) {
        TouristService existing = getServiceById(id);
        ServiceStatus oldStatus = existing.getStatus();
        String oldName = existing.getName();

        // Vérifier que le service appartient au partenaire
        if (existing.getProvider() == null || !existing.getProvider().getEmail().equals(userEmail)) {
            throw new RuntimeException("You are not authorized to modify this service");
        }

        // Vérifier que le service est en PENDING ou qu'il a une autorisation EDIT
        boolean wasApproved = false;
        if (existing.getStatus() != ServiceStatus.PENDING) {
            // Si c'est un service APPROUVÉ, vérifier l'autorisation
            if (existing.getStatus() == ServiceStatus.APPROVED) {
                if (existing.getEditAuthorizedUntil() == null ||
                        existing.getEditAuthorizedUntil().isBefore(LocalDateTime.now())) {
                    throw new RuntimeException("You don't have a valid authorization to modify this approved service. Please request edit permission from admin.");
                }
                wasApproved = true; // ✅ On note que c'était un service approuvé en modification
            } else {
                throw new RuntimeException("You can only modify services with PENDING status or with authorization. Current status: " + existing.getStatus());
            }
        }

        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setPrice(updated.getPrice());
        existing.setGroupPrice(updated.getGroupPrice());
        existing.setAvailability(updated.getAvailability());
        existing.setContactPerson(updated.getContactPerson());
        existing.setCategory(updated.getCategory());
        existing.setTargetAudience(updated.getTargetAudience());
        existing.setDurationHours(updated.getDurationHours());
        existing.setMaxCapacity(updated.getMaxCapacity());

        // ✅ CORRECTION POUR LES COLLECTIONS
        if (updated.getIncludedServices() != null) {
            existing.getIncludedServices().clear();
            existing.getIncludedServices().addAll(updated.getIncludedServices());
        }

        if (updated.getAvailableLanguages() != null) {
            existing.getAvailableLanguages().clear();
            existing.getAvailableLanguages().addAll(updated.getAvailableLanguages());
        }

        if (updated.getRegion() != null) {
            existing.setRegion(updated.getRegion());
        }

        // ✅ SI C'ÉTAIT UN SERVICE APPROUVÉ EN MODIFICATION, RÉINITIALISER L'AUTORISATION
        if (wasApproved) {
            log.info("✅ Réinitialisation de l'autorisation de modification pour le service ID: {}", id);
            existing.setEditAuthorizedUntil(null);
            existing.setAuthorizedByAdminId(null);

            // Optionnel: Laisser le statut APPROVED ou le remettre à PENDING?
            // existing.setStatus(ServiceStatus.PENDING); // Si vous voulez qu'il repasse en attente
        }

        // ✅ GESTION DES DOCUMENTS (votre code existant)
        if (updated.getDocuments() != null && !updated.getDocuments().isEmpty()) {
            log.info("📎 Mise à jour des documents pour le service ID: {}", id);

            // Supprimer les anciens documents
            for (TouristServiceDocument oldDoc : existing.getDocuments()) {
                try {
                    fileStorageService.deleteFile(oldDoc.getFilePath(), FileStorageService.ServiceType.TOURIST);
                    log.info("🗑️ Ancien fichier supprimé: {}", oldDoc.getFilePath());
                } catch (Exception e) {
                    log.warn("⚠️ Impossible de supprimer l'ancien fichier: {}", e.getMessage());
                }
                documentRepository.delete(oldDoc);
            }

            existing.getDocuments().clear();

            // Ajouter les nouveaux documents
            for (TouristServiceDocument doc : updated.getDocuments()) {
                TouristServiceDocument newDoc = new TouristServiceDocument();
                newDoc.setFileName(doc.getFileName());
                newDoc.setFileType(doc.getFileType());
                newDoc.setFileSize(doc.getFileSize());
                newDoc.setFilePath(doc.getFilePath());
                newDoc.setDownloadUrl(doc.getDownloadUrl());
                newDoc.setIsPrimary(doc.getIsPrimary());
                newDoc.setTouristService(existing);
                documentRepository.save(newDoc);
                existing.addDocument(newDoc);
            }
            log.info("✅ Documents remplacés avec succès");
        }

        TouristService saved = touristServiceRepository.save(existing);

        // 🔔 NOTIFICATION: Si c'était un service approuvé modifié avec autorisation
        if (oldStatus == ServiceStatus.APPROVED) {
            String adminTitle = "✏️ Tourist service modified";
            String adminMessage = String.format(
                    "Partner %s %s modified the service '%s'.\nOld name: %s",
                    existing.getProvider().getFirstName(),
                    existing.getProvider().getLastName(),
                    saved.getName(),
                    oldName
            );
            notificationService.createNotificationForRole(adminTitle, adminMessage, Role.ADMIN, saved.getId());

            String touristTitle = "🔄 Tourist service updated";
            String touristMessage = String.format(
                    "The service '%s' has been updated.\nPrice: %.2f TND",
                    saved.getName(),
                    saved.getPrice()
            );
            notificationService.createNotificationForRole(touristTitle, touristMessage, Role.TOURIST, saved.getId());
        }

        return saved;
    }
    // ✅ DELETE supprimer un service (avec vérification email)
    public void deleteService(Long id, String userEmail) {
        TouristService service = getServiceById(id);
        ServiceStatus oldStatus = service.getStatus();
        String serviceName = service.getName();

        // Vérifier que le service appartient au partenaire
        if (service.getProvider() == null || !service.getProvider().getEmail().equals(userEmail)) {
            throw new RuntimeException("You are not authorized to delete this service");
        }

        // Vérifier que le service est en PENDING, REJECTED ou avec autorisation DELETE
        boolean canDelete = false;

        if (service.getStatus() == ServiceStatus.PENDING || service.getStatus() == ServiceStatus.REJECTED) {
            canDelete = true;
        } else if (service.getStatus() == ServiceStatus.APPROVED && service.getDeleteAuthorized()) {
            canDelete = true;
        }

        if (!canDelete) {
            throw new RuntimeException("You can only delete services with PENDING, REJECTED status or with authorization. Current status: " + service.getStatus());
        }
        // ✅ NETTOYER D'ABORD LES FAVORIS DES TOURISTES
        cleanAllTouristFavorites(service);

        // ✅ NETTOYER LES DOCUMENTS
        cleanAllDocuments(service);

        // Sauvegarder les infos avant suppression
        Long serviceId = service.getId();
        LocalPartner provider = service.getProvider();

        touristServiceRepository.deleteById(id);

        // 🔔 NOTIFICATION: Si c'était un service approuvé supprimé avec autorisation
        if (oldStatus == ServiceStatus.APPROVED) {
            // Les demandes sont déjà supprimées après approbation, donc plus besoin de les chercher

            // Notifications
            String adminTitle = "🗑️ Tourist service deleted";
            String adminMessage = String.format(
                    "Partner %s %s deleted the service '%s' (authorization approved).",
                    provider.getFirstName(),
                    provider.getLastName(),
                    serviceName
            );
            notificationService.createNotificationForRole(adminTitle, adminMessage, Role.ADMIN, serviceId);

            String touristTitle = "🗑️ Tourist service deleted";
            String touristMessage = String.format(
                    "The service '%s' has been deleted and is no longer available.",
                    serviceName
            );
            notificationService.createNotificationForRole(touristTitle, touristMessage, Role.TOURIST, serviceId);
        }


    }
    // ========================================
// GESTION DES DOCUMENTS
// ========================================

    /**
     * ✅ Ajouter plusieurs documents à un service
     */
    @Transactional
    public TouristService addDocumentsToService(Long serviceId, List<TouristServiceDocument> documents) {
        log.info("📎 Ajout de {} documents au service ID: {}", documents.size(), serviceId);

        TouristService service = getServiceById(serviceId);

        for (TouristServiceDocument doc : documents) {
            doc.setTouristService(service);
            documentRepository.save(doc);
            service.addDocument(doc);
        }

        TouristService updated = touristServiceRepository.save(service);
        log.info("✅ Documents ajoutés avec succès");

        return updated;
    }
    /**
     * ✅ Ajouter un document au service
     */
    @Transactional
    public TouristServiceDocument addDocumentToService(Long serviceId, TouristServiceDocument document) {
        log.info("📎 Ajout d'un document au service ID: {}", serviceId);

        TouristService service = getServiceById(serviceId);

        // Si c'est le premier document, le marquer comme principal
        if (service.getDocuments().isEmpty()) {
            document.setIsPrimary(true);
        }

        document.setTouristService(service);
        TouristServiceDocument savedDoc = documentRepository.save(document);
        service.getDocuments().add(savedDoc);

        // ✅ SI LE SERVICE ÉTAIT EN MODIFICATION AUTORISÉE, RÉINITIALISER
        if (service.getStatus() == ServiceStatus.APPROVED && service.getEditAuthorizedUntil() != null) {
            log.info("✅ Réinitialisation de l'autorisation après ajout de document pour le service ID: {}", serviceId);
            service.setEditAuthorizedUntil(null);
            service.setAuthorizedByAdminId(null);
            touristServiceRepository.save(service);
        }

        log.info("✅ Document ajouté avec succès, ID: {}", savedDoc.getId());
        return savedDoc;
    }
    public TouristServiceDocument getDocumentById(Long documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document non trouvé avec id: " + documentId));
    }

    /**
     * ✅ Supprimer un document spécifique
     */
    @Transactional
    public void deleteDocument(Long documentId) {
        log.info("🗑️ Suppression du document ID: {}", documentId);

        TouristServiceDocument document = getDocumentById(documentId);

        // Détacher du service
        TouristService service = document.getTouristService();
        if (service != null) {
            service.getDocuments().remove(document);
            touristServiceRepository.save(service);
        }

        // Supprimer le fichier physique
        try {
            fileStorageService.deleteFile(document.getFilePath(), FileStorageService.ServiceType.TOURIST);
            log.info("🗑️ Fichier physique supprimé: {}", document.getFilePath());
        } catch (Exception e) {
            log.warn("⚠️ Impossible de supprimer le fichier physique: {}", e.getMessage());
        }

        // Supprimer le document
        documentRepository.delete(document);
        log.info("✅ Document supprimé avec succès");
    }

    /**
     * ✅ Remplacer tous les documents d'un service
     */
    @Transactional
    public TouristService replaceDocuments(Long serviceId, List<TouristServiceDocument> newDocuments) {
        log.info("🔄 Remplacement des documents pour le service ID: {}", serviceId);

        TouristService service = getServiceById(serviceId);

        // Supprimer les anciens documents (physiques + BD)
        for (TouristServiceDocument oldDoc : service.getDocuments()) {
            try {
                fileStorageService.deleteFile(oldDoc.getFilePath(), FileStorageService.ServiceType.TOURIST);
                log.info("🗑️ Fichier supprimé: {}", oldDoc.getFilePath());
            } catch (Exception e) {
                log.warn("⚠️ Impossible de supprimer le fichier: {}", e.getMessage());
            }
            documentRepository.delete(oldDoc);
        }

        // Vider la liste des documents
        service.getDocuments().clear();

        // Ajouter les nouveaux documents
        for (TouristServiceDocument doc : newDocuments) {
            doc.setTouristService(service);
            TouristServiceDocument savedDoc = documentRepository.save(doc);
            service.addDocument(savedDoc);
        }

        TouristService updated = touristServiceRepository.save(service);
        log.info("✅ Documents remplacés avec succès - {} nouveaux documents", newDocuments.size());

        return updated;
    }

    /**
     * ✅ Nettoyer tous les documents d'un service (avant suppression)
     */
    private void cleanAllDocuments(TouristService service) {
        if (service.getDocuments() != null && !service.getDocuments().isEmpty()) {
            log.info("📎 Nettoyage de {} documents", service.getDocuments().size());

            // Supprimer les fichiers physiques
            for (TouristServiceDocument doc : service.getDocuments()) {
                try {
                    fileStorageService.deleteFile(doc.getFilePath(), FileStorageService.ServiceType.TOURIST);
                    log.info("🗑️ Fichier supprimé: {}", doc.getFilePath());
                } catch (Exception e) {
                    log.warn("⚠️ Impossible de supprimer le fichier: {}", e.getMessage());
                }
            }

            // Vider la liste (les entités seront supprimées en cascade)
            service.getDocuments().clear();
        }
    }
    // ========================================
    // ✅ MÉTHODE POUR NETTOYER LES FAVORIS DES TOURISTES
    // ========================================
    private void cleanAllTouristFavorites(TouristService service) {
        log.info(" Nettoyage des favoris pour le service touristique ID: {}", service.getId());

        try {
            // Appeler la méthode du service de favoris pour retirer ce service de tous les touristes
            favoriteTouristService.removeServiceFromAllFavorites(service.getId());
            log.info("✅ Favoris des touristes nettoyés avec succès");
        } catch (Exception e) {
            log.error("❌ Erreur lors du nettoyage des favoris: {}", e.getMessage());
        }
    }

    /**
     * ✅ Révoquer l'autorisation de modification
     */
    @Transactional
    public void revokeEditAuthorization(Long serviceId) {
        log.info("🔓 Révocation autorisation de modification pour service ID: {}", serviceId);

        TouristService service = getServiceById(serviceId);

        // ✅ Révoquer l'autorisation
        service.setEditAuthorizedUntil(null);
        service.setAuthorizedByAdminId(null);
        touristServiceRepository.save(service);
        log.info("✅ Autorisation de modification révoquée pour service ID: {}", serviceId);

        // ✅ Mettre à jour la demande associée
        List<TouristServiceRequest> approvedRequests = touristServiceRequestRepository
                .findByServiceIdAndStatus(serviceId, ServiceStatus.APPROVED);

        for (TouristServiceRequest request : approvedRequests) {
            if (request.getRequestType() == RequestType.EDIT) {
                // Marquer comme utilisée (ou supprimer)
                touristServiceRequestRepository.delete(request);
                log.info("📝 Demande ID: {} supprimée après utilisation", request.getId());
            }
        }
    }
    public TouristService save(TouristService service) {
        return touristServiceRepository.save(service);
    }
}