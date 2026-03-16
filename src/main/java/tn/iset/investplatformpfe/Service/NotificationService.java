package tn.iset.investplatformpfe.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Repository.InternationalCompanyRepository;
import tn.iset.investplatformpfe.Repository.InvestmentServiceRepository;
import tn.iset.investplatformpfe.Repository.NotificationRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import tn.iset.investplatformpfe.Entity.CollaborationService;
import tn.iset.investplatformpfe.Entity.InvestmentService;

import java.util.List;
import java.util.stream.Collectors;
import tn.iset.investplatformpfe.Entity.TouristService;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final InvestmentServiceRepository investmentServiceRepository;
    private final InternationalCompanyRepository internationalCompanyRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               InvestmentServiceRepository investmentServiceRepository,InternationalCompanyRepository internationalCompanyRepository) {
        this.notificationRepository = notificationRepository;
        this.investmentServiceRepository = investmentServiceRepository;
        this.internationalCompanyRepository = internationalCompanyRepository;
    }

    // ========================================
    // CRÉER UNE NOTIFICATION POUR UN UTILISATEUR SPÉCIFIQUE
    // ========================================
    @Transactional
    public Notification createNotificationForUser(String title, String message,
                                                  Role recipientRole, Long recipientId,
                                                  Long serviceId) {
        log.info("📝 Création notification utilisateur - Rôle: {}, RecipientId: {}, ServiceId: {}",
                recipientRole, recipientId, serviceId);

        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRecipientRole(recipientRole);
        notification.setRecipientId(recipientId);
        notification.setServiceId(serviceId);
        notification.setRead(false);

        Notification saved = notificationRepository.save(notification);
        log.info("✅ Notification utilisateur créée avec ID: {}", saved.getId());

        return saved;
    }

    // ========================================
    // CRÉER UNE NOTIFICATION POUR TOUS LES UTILISATEURS D'UN RÔLE (BROADCAST)
    // ========================================
    @Transactional
    public Notification createNotificationForRole(String title, String message,
                                                  Role recipientRole,
                                                  Long serviceId) {
        log.info("📢 Création notification broadcast - Rôle: {}, ServiceId: {}", recipientRole, serviceId);

        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRecipientRole(recipientRole);
        notification.setRecipientId(null);
        notification.setServiceId(serviceId);
        notification.setRead(false);

        Notification saved = notificationRepository.save(notification);
        log.info("✅ Notification broadcast créée avec ID: {}", saved.getId());

        return saved;
    }

    // ========================================
    // NOTIFICATION: Nouveau service d'investissement créé (vers ADMIN)
    // ========================================
    @Transactional
    public void notifyAdminNewInvestmentService(InvestmentService service) {
        log.info("👤 Notification ADMIN - Nouveau service en attente: {}", service.getTitle());

        String title = "Nouveau service d'investissement en attente";
        String message = String.format("Le service d'investissement '%s' créé par %s %s est en attente d'approbation",
                service.getTitle(),
                service.getProvider().getFirstName(),
                service.getProvider().getLastName());

        createNotificationForRole(title, message, Role.ADMIN, service.getId());
    }

    // ========================================
    // NOTIFICATION: Service d'investissement approuvé (vers LOCAL_PARTNER)
    // ========================================
    @Transactional
    public void notifyLocalPartnerInvestmentApproved(InvestmentService service) {
        log.info("👤 Notification LOCAL_PARTNER - Service approuvé: {}", service.getTitle());

        String title = "Service d'investissement approuvé !";
        String message = String.format("Votre service d'investissement '%s' a été approuvé et est maintenant visible",
                service.getTitle());

        createNotificationForUser(
                title,
                message,
                Role.LOCAL_PARTNER,
                service.getProvider().getId(),
                service.getId()
        );
    }

    // ========================================
    // NOTIFICATION: Service d'investissement rejeté (vers LOCAL_PARTNER)
    // ========================================
    // ========================================
// NOTIFICATION: Investment Service Rejected (to LOCAL_PARTNER) - Professional version
// ========================================
    @Transactional
    public void notifyLocalPartnerInvestmentRejected(InvestmentService service) {
        log.info("👤 Notification LOCAL_PARTNER - Service rejected: {}", service.getTitle());

        String title = "❌ Investment Service Rejected";
        String message = String.format(
                "Dear %s %s,\n\n" +
                        "We regret to inform you that your investment service '%s' (ID: %d) has been rejected.\n\n" +
                        "Rejection Reason:\n" +
                        "-----------------\n" +
                        "%s\n\n" +
                        "Rejection Date: %s\n\n" +
                        "If you wish to resubmit a corrected version, please ensure that all requirements are met.\n\n" +
                        "Best regards,\n" +
                        "Investment Platform Team",
                service.getProvider().getFirstName(),
                service.getProvider().getLastName(),
                service.getTitle(),
                service.getId(),
                service.getRejectionReason() != null ? service.getRejectionReason() : "Reason not specified",
                service.getRejectedAt() != null ? service.getRejectedAt().toLocalDate().toString() : "Date not specified"
        );

        createNotificationForUser(
                title,
                message,
                Role.LOCAL_PARTNER,
                service.getProvider().getId(),
                service.getId()
        );
    }

    // ========================================
    // NOTIFICATION: Nouveau service investissement approuvé (vers tous les INVESTOR)
    // ========================================
    @Transactional
    public void notifyInvestorsNewInvestmentService(InvestmentService service) {
        log.info("📢 Notification INVESTOR - Nouvelle opportunité: {}", service.getTitle());

        String title = "New Investment Opportunity Available!";
        String message = String.format("A new investment opportunity '%s' is now available",
                service.getTitle());

        createNotificationForRole(title, message, Role.INVESTOR, service.getId());
    }

    // ========================================
    // ✅ CORRECTION: NOTIFICATION pour INTERNATIONAL_COMPANY
    // ========================================
// ========================================
// NOTIFICATION: Nouveau service investissement approuvé (vers INTERNATIONAL_COMPANY)
// ========================================
// ========================================
// NOTIFICATION: Nouveau service investissement approuvé (vers INTERNATIONAL_COMPANY)
// ========================================
    @Transactional
    public void notifyInternationalCompaniesNewInvestmentService(InvestmentService service) {
        log.info("██████████████████████████████████████████████████████████████");
        log.info("██                                                        ██");
        log.info("██   📢 NOTIFICATION INTERNATIONAL COMPANY - APPROBATION  ██");
        log.info("██   Nouveau service d'investissement disponible         ██");
        log.info("██                                                        ██");
        log.info("██████████████████████████████████████████████████████████████");

        if (service == null) {
            log.error("❌ Service est null - impossible d'envoyer notification");
            return;
        }

        log.info("📌 ID du service: {}", service.getId());
        log.info("📌 Titre: {}", service.getTitle());
        log.info("📌 Provider: {} {}", service.getProvider().getFirstName(), service.getProvider().getLastName());
        log.info("📌 Date: {}", LocalDateTime.now());

        // ✅ MÊME FORMAT QUE LA NOTIFICATION DE MODIFICATION
        String title = "🔄 Nouveau service d'investissement disponible";
        String message = String.format(
                "Un nouveau service d'investissement '%s' a été approuvé et est maintenant disponible.\n\n" +
                        "📊 Détails du service :\n" +
                        "• Titre : %s\n" +
                        "• Montant total : %,.2f TND\n" +
                        "• Montant minimum : %,.2f TND\n" +
                        "• Région : %s\n" +
                        "• Secteur : %s\n" +
                        "• Partenaire : %s %s\n" +
                        "• Disponibilité : %s\n\n" +
                        "Connectez-vous pour voir cette nouvelle opportunité !",
                service.getTitle(),
                service.getTitle(),
                service.getTotalAmount() != null ? service.getTotalAmount() : BigDecimal.ZERO,
                service.getMinimumAmount() != null ? service.getMinimumAmount() : BigDecimal.ZERO,
                service.getRegion() != null ? service.getRegion().getName() : "Non spécifiée",
                service.getEconomicSector() != null ? service.getEconomicSector().getName() : "Non spécifié",
                service.getProvider().getFirstName(),
                service.getProvider().getLastName(),
                service.getAvailability()
        );

        try {
            // ✅ CRÉER LA NOTIFICATION AVEC LE BON TITRE (COMME POUR LA MODIFICATION)
            Notification notification = createNotificationForRole(
                    title,
                    message,
                    Role.INTERNATIONAL_COMPANY,
                    service.getId()
            );

            notification.setServiceType("INVESTMENT");
            notificationRepository.save(notification);

            log.info("✅ Notification d'approbation envoyée à toutes les sociétés internationales");
            log.info("✅ Titre: {}", title);
            log.info("✅ Notification ID: {}", notification.getId());

        } catch (Exception e) {
            log.error("❌ ERREUR lors de l'envoi de la notification d'approbation: {}", e.getMessage());
            e.printStackTrace();
        }

        log.info("==============================================================");
    }

    // ========================================
    // NOTIFICATION: Nouveau service collaboration approuvé (vers INTERNATIONAL_COMPANY)
    // ========================================
    @Transactional
    public void notifyInternationalCompaniesNewCollaborationService(CollaborationService service) {
        log.info("📢 Notification INTERNATIONAL_COMPANY - Nouvelle collaboration: {}", service.getName());

        String title = "New Collaboration Opportunity!";
        String message = String.format(
                "A new collaboration service '%s' is now available in %s region",
                service.getName(),
                service.getRegion() != null ? service.getRegion().getName() : "your area"
        );

        Notification notif = createNotificationForRole(title, message, Role.INTERNATIONAL_COMPANY, service.getId());
        notif.setServiceType("COLLABORATION");
        notificationRepository.save(notif);

        log.info("✅ Notification collaboration créée avec ID: {}", notif.getId());
    }

    // ========================================
    // NOTIFICATION: Nouveau service collaboration approuvé (vers PARTNER)
    // ========================================
    @Transactional
    public void notifyPartnersNewCollaborationService(CollaborationService service) {
        log.info("📢 Notification PARTNER - Nouvelle collaboration: {}", service.getName());

        String title = "New Collaboration Service Available!";
        String message = String.format("A new collaboration service '%s' is now available",
                service.getName());

        createNotificationForRole(title, message, Role.PARTNER, service.getId());
    }

    // ========================================
    // NOTIFICATION: Nouveau service touristique approuvé (vers TOURIST)
    // ========================================
    @Transactional
    public void notifyTouristsNewService(TouristService service) {
        log.info("📢 Notification TOURIST - Nouveau service: {}", service.getName());

        String title = "New Tourist Service Available!";
        String message = String.format("A new tourist service '%s' is now available in %s",
                service.getName(),
                service.getRegion() != null ? service.getRegion().getName() : "your area");

        createNotificationForRole(title, message, Role.TOURIST, service.getId());
    }

    // ========================================
    // RÉCUPÉRER LES NOTIFICATIONS D'UN UTILISATEUR
    // ========================================
    public List<Notification> getUserNotifications(Role role, Long userId) {
        log.debug("Récupération notifications - Rôle: {}, UserId: {}", role, userId);

        List<Notification> personal = notificationRepository
                .findByRecipientRoleAndRecipientIdOrderByCreatedAtDesc(role, userId);

        List<Notification> broadcast = notificationRepository
                .findByRecipientRoleAndRecipientIdIsNullOrderByCreatedAtDesc(role);

        personal.addAll(broadcast);
        personal.sort((n1, n2) -> n2.getCreatedAt().compareTo(n1.getCreatedAt()));

        log.debug("{} notifications trouvées", personal.size());
        return personal;
    }

    // ========================================
    // RÉCUPÉRER LES NOTIFICATIONS NON LUES
    // ========================================
    public List<Notification> getUnreadNotifications(Role role, Long userId) {
        log.debug("Récupération notifications non lues - Rôle: {}, UserId: {}", role, userId);

        List<Notification> personal = notificationRepository
                .findByRecipientRoleAndRecipientIdAndReadFalseOrderByCreatedAtDesc(role, userId);

        List<Notification> broadcast = notificationRepository
                .findByRecipientRoleAndRecipientIdIsNullOrderByCreatedAtDesc(role)
                .stream()
                .filter(n -> !n.isRead())
                .collect(Collectors.toList());

        personal.addAll(broadcast);
        personal.sort((n1, n2) -> n2.getCreatedAt().compareTo(n1.getCreatedAt()));

        log.debug("{} notifications non lues trouvées", personal.size());
        return personal;
    }

    // ========================================
    // MARQUER UNE NOTIFICATION COMME LUE
    // ========================================
    @Transactional
    public void markAsRead(Long notificationId) {
        log.info("Marquage notification {} comme lue", notificationId);

        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + notificationId));

        notification.setRead(true);
        notificationRepository.save(notification);

        log.info("Notification {} marquée comme lue", notificationId);
    }

    // ========================================
    // MARQUER TOUTES LES NOTIFICATIONS COMME LUES
    // ========================================
    @Transactional
    public void markAllAsRead(Role role, Long userId) {
        log.info("Marquage toutes notifications comme lues - Rôle: {}, UserId: {}", role, userId);

        List<Notification> unread = getUnreadNotifications(role, userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);

        log.info("{} notifications marquées comme lues", unread.size());
    }

    // ========================================
    // COMPTER LES NOTIFICATIONS NON LUES
    // ========================================
    public long countUnread(Role role, Long userId) {
        long count = getUnreadNotifications(role, userId).size();
        log.debug("Nombre de notifications non lues pour {}: {}", role, count);
        return count;
    }

    // ========================================
    // RÉCUPÉRER TOUTES LES NOTIFICATIONS (POUR DEBUG)
    // ========================================
    public List<Notification> getAllNotifications() {
        log.debug("Récupération de toutes les notifications");
        return notificationRepository.findAll();
    }

    // ========================================
    // NOTIFICATION: Nouveau service créé (vers ADMIN) - Collaboration
    // ========================================
    @Transactional
    public void notifyAdminNewService(CollaborationService service) {
        log.info("👤 Notification ADMIN - Nouveau service collaboration: {}", service.getName());

        String title = "New Service Pending Approval";
        String message = String.format("Service '%s' created by %s %s is waiting for your approval",
                service.getName(),
                service.getProvider().getFirstName(),
                service.getProvider().getLastName());

        createNotificationForRole(title, message, Role.ADMIN, service.getId());
    }

    // ========================================
    // NOTIFICATION: Service collaboration approuvé (vers LOCAL_PARTNER)
    // ========================================
    @Transactional
    public void notifyLocalPartnerServiceApproved(CollaborationService service) {
        log.info("👤 Notification LOCAL_PARTNER - Service collaboration approuvé: {}", service.getName());

        String title = "Service Approved!";
        String message = String.format("Your service '%s' has been approved and is now visible to everyone",
                service.getName());

        createNotificationForUser(
                title,
                message,
                Role.LOCAL_PARTNER,
                service.getProvider().getId(),
                service.getId()
        );
    }

    // ========================================
    // NOTIFICATION: Service collaboration rejeté (vers LOCAL_PARTNER)
    // ========================================
    @Transactional
    public void notifyLocalPartnerServiceRejected(CollaborationService service) {
        log.info("👤 Notification LOCAL_PARTNER - Collaboration service rejected: {}", service.getName());

        // ✅ Get the reason (with default value if null)
        String rejectionReason = service.getRejectionReason();
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            rejectionReason = "Reason not specified by administrator";
        }

        // ✅ Get the rejection date
        String rejectedDate = service.getRejectedAt() != null ?
                service.getRejectedAt().toLocalDate().toString() : "Date not specified";

        // ✅ Build detailed message with reason
        String title = "❌ Collaboration Service Rejected";
        String message = String.format(
                "Dear %s %s,\n\n" +
                        "We regret to inform you that your collaboration service '%s' (ID: %d) has been rejected.\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "❌ REJECTION REASON :\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "%s\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "📅 Rejection Date : %s\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                        "Service Details:\n" +
                        "• Requested Budget: %,.2f TND\n" +
                        "• Region: %s\n" +
                        "• Activity Domain: %s\n\n" +
                        "If you wish to resubmit a corrected version, please ensure that all requirements are met.\n\n" +
                        "Best regards,\n" +
                        "Investment Platform Team",
                service.getProvider().getFirstName(),
                service.getProvider().getLastName(),
                service.getName(),
                service.getId(),
                rejectionReason,
                rejectedDate,
                service.getRequestedBudget() != null ? service.getRequestedBudget() : BigDecimal.ZERO,
                service.getRegion() != null ? service.getRegion().getName() : "Not specified",
                service.getActivityDomain() != null ? service.getActivityDomain().toString() : "Not specified"
        );

        createNotificationForUser(
                title,
                message,
                Role.LOCAL_PARTNER,
                service.getProvider().getId(),
                service.getId()
        );

        log.info("✅ Notification sent to partner {} with reason: {}",
                service.getProvider().getEmail(), rejectionReason);
    }

    // ========================================
    // NOTIFICATION: Nouvelle opportunité collaboration (vers PARTNER et INTERNATIONAL_COMPANY)
    // ========================================
    @Transactional
    public void notifyPartnersAndCompaniesNewOpportunity(CollaborationService service) {
        log.info("📢 Notification PARTNER/INTERNATIONAL_COMPANY - Nouvelle opportunité: {}", service.getName());

        String title = "New Collaboration Opportunity!";
        String message = String.format("A new collaboration opportunity '%s' is now available in %s region",
                service.getName(),
                service.getRegion().getName());

        createNotificationForRole(title, message, Role.PARTNER, service.getId());
        createNotificationForRole(title, message, Role.INTERNATIONAL_COMPANY, service.getId());
    }

    // ========================================
    // NOTIFICATION: Nouveau service touristique créé (vers ADMIN)
    // ========================================
    @Transactional
    public void notifyAdminNewTouristService(TouristService service) {
        log.info("👤 Notification ADMIN - Nouveau service touristique: {}", service.getName());

        String title = "New Tourist Service Pending Approval";
        String message = String.format("Tourist service '%s' created by %s %s is waiting for your approval",
                service.getName(),
                service.getProvider().getFirstName(),
                service.getProvider().getLastName());

        createNotificationForRole(title, message, Role.ADMIN, service.getId());
    }

    // ========================================
    // NOTIFICATION: Service touristique approuvé (vers LOCAL_PARTNER)
    // ========================================
    @Transactional
    public void notifyLocalPartnerTouristApproved(TouristService service) {
        log.info("👤 Notification LOCAL_PARTNER - Service touristique approuvé: {}", service.getName());

        String title = "Tourist Service Approved!";
        String message = String.format("Your tourist service '%s' has been approved and is now visible to everyone",
                service.getName());

        createNotificationForUser(title, message, Role.LOCAL_PARTNER,
                service.getProvider().getId(), service.getId());
    }

    // ========================================
    // NOTIFICATION: Service touristique rejeté (vers LOCAL_PARTNER)
    // ========================================
    @Transactional
    public void notifyLocalPartnerTouristRejected(TouristService service) {
        log.info("👤 Notification LOCAL_PARTNER - Tourist service rejected: {}", service.getName());

        // ✅ Get the reason (with default value if null)
        String rejectionReason = service.getRejectionReason();
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            rejectionReason = "Reason not specified by administrator";
        }

        // ✅ Get the rejection date
        String rejectedDate = service.getRejectedAt() != null ?
                service.getRejectedAt().toLocalDate().toString() : "Date not specified";

        // ✅ Build detailed message with reason
        String title = "❌ Tourist Service Rejected";
        String message = String.format(
                "Dear %s %s,\n\n" +
                        "We regret to inform you that your tourist service '%s' (ID: %d) has been rejected.\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "❌ REJECTION REASON :\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "%s\n\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "📅 Rejection Date : %s\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
                        "Service Details:\n" +
                        "• Price: %,.2f TND\n" +
                        "• Category: %s\n" +
                        "• Region: %s\n" +
                        "• Duration: %d hours\n" +
                        "• Max Capacity: %d persons\n\n" +
                        "If you wish to resubmit a corrected version, please ensure that all requirements are met.\n\n" +
                        "Best regards,\n" +
                        "Tourism Platform Team",
                service.getProvider().getFirstName(),
                service.getProvider().getLastName(),
                service.getName(),
                service.getId(),
                rejectionReason,
                rejectedDate,
                service.getPrice() != null ? service.getPrice() : BigDecimal.ZERO,
                service.getCategory() != null ? service.getCategory().toString() : "Not specified",
                service.getRegion() != null ? service.getRegion().getName() : "Not specified",
                service.getDurationHours() != null ? service.getDurationHours() : 0,
                service.getMaxCapacity() != null ? service.getMaxCapacity() : 0
        );

        createNotificationForUser(
                title,
                message,
                Role.LOCAL_PARTNER,
                service.getProvider().getId(),
                service.getId()
        );

        log.info("✅ Notification sent to partner {} with reason: {}",
                service.getProvider().getEmail(), rejectionReason);
    }

    // =========================================================================
    // ✅ NOUVELLES MÉTHODES AJOUTÉES POUR LA GESTION DES DEMANDES
    // =========================================================================

    // ========================================
    // NOTIFICATION: Admin - Nouvelle demande reçue
    // ========================================
    @Transactional
    public void notifyAdminNewRequest(ServiceRequest request) {
        log.info("👤 Notification ADMIN - Nouvelle demande: {}", request.getId());

        String type = request.getRequestType() == RequestType.EDIT ? "modification" : "suppression";
        String title = "Nouvelle demande de " + type;
        String message = String.format(
                "Le partenaire %s %s demande la %s du service '%s'.\nRaison: %s",
                request.getPartner().getFirstName(),
                request.getPartner().getLastName(),
                type,
                request.getService().getTitle(),
                request.getReason()
        );

        createNotificationForRole(title, message, Role.ADMIN, request.getService().getId());
    }

    // ========================================
    // NOTIFICATION: Partenaire - Demande de modification approuvée
    // ========================================
    @Transactional
    public void notifyPartnerEditApproved(ServiceRequest request) {
        log.info("👤 Notification PARTENAIRE - Demande de modification approuvée: {}", request.getId());

        String title = "✅ Demande de modification approuvée";
        String message = String.format(
                "Votre demande de modification pour le service '%s' a été approuvée.\n" +
                        "Vous avez jusqu'au %s pour effectuer les modifications.\n" +
                        "Raison de votre demande: %s",
                request.getService().getTitle(),
                request.getExecutionDate() != null ? request.getExecutionDate().toLocalDate().toString() : "7 jours",
                request.getReason()
        );

        createNotificationForUser(
                title,
                message,
                Role.LOCAL_PARTNER,
                request.getPartner().getId(),
                request.getService().getId()
        );
    }

    // ========================================
    // NOTIFICATION: Partenaire - Demande de suppression approuvée
    // ========================================
    @Transactional
    public void notifyPartnerDeleteApproved(ServiceRequest request) {
        log.info("👤 Notification PARTENAIRE - Demande de suppression approuvée: {}", request.getId());

        String title = "✅ Demande de suppression approuvée";
        String message = String.format(
                "Votre demande de suppression pour le service '%s' a été approuvée.\n" +
                        "Vous avez jusqu'au %s pour supprimer ce service.\n" +
                        "Raison de votre demande: %s",
                request.getService().getTitle(),
                request.getExecutionDate() != null ? request.getExecutionDate().toLocalDate().toString() : "3 jours",
                request.getReason()
        );

        createNotificationForUser(
                title,
                message,
                Role.LOCAL_PARTNER,
                request.getPartner().getId(),
                request.getService().getId()
        );
    }

    // ========================================
    // NOTIFICATION: Partenaire - Demande rejetée
    // ========================================
    @Transactional
    public void notifyPartnerRequestRejected(ServiceRequest request) {
        log.info("👤 Notification PARTNER - Request rejected: {}", request.getId());

        String type = request.getRequestType() == RequestType.EDIT ? "modification" : "deletion";

        // ✅ Get rejection reason from admin
        String rejectionReason = request.getRejectionReason();
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            rejectionReason = "Reason not specified by administrator";
        }

        // ✅ Get rejection date
        String rejectedDate = request.getRejectedAt() != null ?
                request.getRejectedAt().toLocalDate().toString() : "Date not specified";

        String title = "❌ " + (type.equals("modification") ? "Modification" : "Deletion") + " Request Rejected";

        String message = String.format(
                "Dear %s %s,\n\n" +
                        "Your %s request for service '%s' (ID: %d) has been rejected by the administrator.\n\n" +
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
                        "The Administration Team",
                request.getPartner().getFirstName(),
                request.getPartner().getLastName(),
                type,
                request.getService().getTitle(),
                request.getService().getId(),
                rejectionReason,
                rejectedDate,
                request.getReason() != null ? request.getReason() : "Not specified"
        );

        createNotificationForUser(
                title,
                message,
                Role.LOCAL_PARTNER,
                request.getPartner().getId(),
                request.getService().getId()
        );

        log.info("✅ Notification sent to partner {} with reason: {}",
                request.getPartner().getEmail(), rejectionReason);
    }

    // ========================================
    // NOTIFICATION: Partenaire - Modification effectuée avec succès
    // ========================================
    @Transactional
    public void notifyPartnerEditCompleted(LocalPartner partner, InvestmentService service) {
        log.info("👤 Notification PARTENAIRE - Modification complétée: {}", service.getId());

        String title = "✏️ Modification effectuée avec succès";
        String message = String.format(
                "Votre modification du service '%s' a été enregistrée.\n" +
                        "Le service est à nouveau visible pour tous les investisseurs.",
                service.getTitle()
        );

        createNotificationForUser(
                title,
                message,
                Role.LOCAL_PARTNER,
                partner.getId(),
                service.getId()
        );
    }

    // ========================================
    // NOTIFICATION: Partenaire - Service supprimé par admin
    // ========================================
    @Transactional
    public void notifyPartnerServiceDeletedByAdmin(InvestmentService service) {
        log.info("👤 Notification PARTENAIRE - Service supprimé par admin: {}", service.getId());

        String title = "🗑️ Service supprimé par l'admin";
        String message = String.format(
                "Votre service '%s' a été supprimé par un administrateur.",
                service.getTitle()
        );

        createNotificationForUser(
                title,
                message,
                Role.LOCAL_PARTNER,
                service.getProvider().getId(),
                service.getId()
        );
    }

    // ========================================
    // NOTIFICATION: Admin - Service modifié par le partenaire
    // ========================================
    @Transactional
    public void notifyAdminServiceEdited(InvestmentService service, LocalPartner partner, InvestmentService oldService) {
        log.info("👤 Notification ADMIN - Service modifié: {}", service.getId());

        String title = "✏️ Service modifié par le partenaire";
        String message = String.format(
                "Le service '%s' a été modifié par %s %s.\n" +
                        "Veuillez vérifier les modifications.",
                service.getTitle(),
                partner.getFirstName(),
                partner.getLastName()
        );

        createNotificationForRole(title, message, Role.ADMIN, service.getId());
    }

    // ========================================
    // NOTIFICATION: Admin - Suppression effectuée
    // ========================================
    @Transactional
    public void notifyAdminDeleteCompleted(Admin admin, LocalPartner partner, String serviceName) {
        log.info("👤 Notification ADMIN - Suppression complétée");

        String title = "🗑️ Suppression effectuée";
        String message = String.format(
                "Le service '%s' a été supprimé suite à la demande de %s %s.",
                serviceName,
                partner.getFirstName(),
                partner.getLastName()
        );

        createNotificationForUser(
                title,
                message,
                Role.ADMIN,
                admin.getId(),
                null
        );
    }

    // ========================================
    // NOTIFICATION: Investisseurs intéressés - Service mis à jour
    // ========================================
    @Transactional
    public void notifyInterestedInvestorsServiceUpdated(InvestmentService service) {
        log.info("📢 Notification INVESTORS intéressés - Service mis à jour: {}", service.getId());

        if (service.getInterestedInvestors() == null || service.getInterestedInvestors().isEmpty()) {
            log.info("Aucun investisseur intéressé pour ce service");
            return;
        }

        String title = "🔄 Service mis à jour";
        String message = String.format(
                "Le service '%s' dans lequel vous êtes intéressé a été mis à jour.\n" +
                        "Consultez les nouvelles informations.",
                service.getTitle()
        );

        for (Investor investor : service.getInterestedInvestors()) {
            createNotificationForUser(
                    title,
                    message,
                    Role.INVESTOR,
                    investor.getId(),
                    service.getId()
            );
        }

        log.info("✅ Notifications envoyées à {} investisseurs intéressés",
                service.getInterestedInvestors().size());
    }

    // ========================================
    // NOTIFICATION: Sociétés internationales - Service mis à jour
    // ========================================
    @Transactional
    public void notifyInternationalCompaniesServiceUpdated(InvestmentService service) {
        log.info("📢 Notification INTERNATIONAL_COMPANY - Service mis à jour: {}", service.getId());

        String title = "🔄 Service d'investissement mis à jour";
        String message = String.format(
                "Le service d'investissement '%s' a été mis à jour.\n" +
                        "Consultez les nouvelles informations.",
                service.getTitle()
        );

        createNotificationForRole(title, message, Role.INTERNATIONAL_COMPANY, service.getId());
    }

    // ========================================
    // NOTIFICATION: Rappel d'expiration d'autorisation
    // ========================================
    @Transactional
    public void notifyPartnerAuthorizationExpiring(ServiceRequest request) {
        log.info("👤 Notification PARTENAIRE - Autorisation bientôt expirée: {}", request.getId());

        String type = request.getRequestType() == RequestType.EDIT ? "modifier" : "supprimer";
        String title = "⚠️ Autorisation bientôt expirée";
        String message = String.format(
                "L'autorisation de %s le service '%s' expire le %s.\n" +
                        "Effectuez l'action rapidement.",
                type,
                request.getService().getTitle(),
                request.getExecutionDate() != null ? request.getExecutionDate().toLocalDate().toString() : "bientôt"
        );

        createNotificationForUser(
                title,
                message,
                Role.LOCAL_PARTNER,
                request.getPartner().getId(),
                request.getService().getId()
        );
    }

    // ========================================
    // NOTIFICATION: Autorisation expirée
    // ========================================
    @Transactional
    public void notifyPartnerAuthorizationExpired(ServiceRequest request) {
        log.info("👤 Notification PARTENAIRE - Autorisation expirée: {}", request.getId());

        String type = request.getRequestType() == RequestType.EDIT ? "modifier" : "supprimer";
        String title = "⏰ Autorisation expirée";
        String message = String.format(
                "L'autorisation de %s le service '%s' a expiré.\n" +
                        "Vous devez faire une nouvelle demande si vous souhaitez toujours %s ce service.",
                type,
                request.getService().getTitle(),
                type
        );

        createNotificationForUser(
                title,
                message,
                Role.LOCAL_PARTNER,
                request.getPartner().getId(),
                request.getService().getId()
        );
    }
    // ========================================
// MÉTHODE 1: SUPPRIMER UNE NOTIFICATION SPÉCIFIQUE
// ========================================
    @Transactional
    public void deleteNotification(Long notificationId) {
        log.info("🗑️ Suppression notification ID: {}", notificationId);

        if (!notificationRepository.existsById(notificationId)) {
            throw new RuntimeException("Notification non trouvée avec id: " + notificationId);
        }

        notificationRepository.deleteById(notificationId);
        log.info("✅ Notification {} supprimée avec succès", notificationId);
    }

    // ========================================
// MÉTHODE 2: SUPPRIMER TOUTES LES NOTIFICATIONS SAUF LES NON LUES
// ========================================
    @Transactional
    public void deleteAllReadNotifications(Role userRole, Long userId) {
        log.info("🗑️ Suppression de toutes les notifications lues pour {} ID: {}", userRole, userId);

        // Récupérer les notifications personnelles lues
        List<Notification> readPersonal = notificationRepository
                .findByRecipientRoleAndRecipientIdAndReadTrue(userRole, userId);

        // Récupérer les notifications broadcast lues
        List<Notification> readBroadcast = notificationRepository
                .findByRecipientRoleAndRecipientIdIsNullAndReadTrue(userRole);

        readPersonal.addAll(readBroadcast);

        if (readPersonal.isEmpty()) {
            log.info("Aucune notification lue à supprimer pour {} ID: {}", userRole, userId);
            return;
        }

        notificationRepository.deleteAll(readPersonal);
        log.info("✅ {} notifications lues supprimées", readPersonal.size());
    }



    @Transactional
    public void notifyAdminNewCollaborationRequest(CollaborationServiceRequest request) {
        log.info("👤 Notification ADMIN - Nouvelle demande de collaboration: {}", request.getId());

        String type = request.getRequestType() == RequestType.EDIT ? "modification" : "suppression";
        String title = "Nouvelle demande de " + type + " (Collaboration)";
        String message = String.format(
                "Le partenaire %s %s demande la %s du service de collaboration '%s'.\nRaison: %s",
                request.getPartner().getFirstName(),
                request.getPartner().getLastName(),
                type,
                request.getService().getName(),
                request.getReason()
        );

        createNotificationForRole(title, message, Role.ADMIN, request.getService().getId());
    }

    /**
     * Partenaire local - Demande de modification approuvée (Collaboration)
     */
    @Transactional
    public void notifyPartnerCollaborationEditApproved(CollaborationServiceRequest request) {
        log.info("👤 Notification PARTENAIRE - Demande de modification collaboration approuvée: {}", request.getId());

        String title = "✅ Demande de modification approuvée (Collaboration)";
        String message = String.format(
                "Votre demande de modification pour le service de collaboration '%s' a été approuvée.\n" +
                        "Vous avez jusqu'au %s pour effectuer les modifications.\n" +
                        "Raison de votre demande: %s",
                request.getService().getName(),
                request.getExecutionDate() != null ? request.getExecutionDate().toLocalDate().toString() : "7 jours",
                request.getReason()
        );

        createNotificationForUser(
                title,
                message,
                Role.LOCAL_PARTNER,
                request.getPartner().getId(),
                request.getService().getId()
        );
    }

    /**
     * Partenaire local - Demande de suppression approuvée (Collaboration)
     */
    @Transactional
    public void notifyPartnerCollaborationDeleteApproved(CollaborationServiceRequest request) {
        log.info("👤 Notification PARTENAIRE - Demande de suppression collaboration approuvée: {}", request.getId());

        String title = "✅ Demande de suppression approuvée (Collaboration)";
        String message = String.format(
                "Votre demande de suppression pour le service de collaboration '%s' a été approuvée.\n" +
                        "Vous avez jusqu'au %s pour supprimer ce service.\n" +
                        "Raison de votre demande: %s",
                request.getService().getName(),
                request.getExecutionDate() != null ? request.getExecutionDate().toLocalDate().toString() : "3 jours",
                request.getReason()
        );

        createNotificationForUser(
                title,
                message,
                Role.LOCAL_PARTNER,
                request.getPartner().getId(),
                request.getService().getId()
        );
    }

    /**
     * Partenaire local - Demande rejetée (Collaboration)
     */
    @Transactional
    public void notifyPartnerCollaborationRequestRejected(CollaborationServiceRequest request) {
        log.info("👤 Notification PARTNER - Collaboration request rejected: {}", request.getId());

        String type = request.getRequestType() == RequestType.EDIT ? "modification" : "deletion";

        // ✅ Get rejection reason from admin
        String rejectionReason = request.getRejectionReason();
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            rejectionReason = "Reason not specified by administrator";
        }

        // ✅ Get rejection date
        String rejectedDate = request.getRejectedAt() != null ?
                request.getRejectedAt().toLocalDate().toString() : "Date not specified";

        String title = "❌ " + (type.equals("modification") ? "Modification" : "Deletion") + " Request Rejected (Collaboration)";

        String message = String.format(
                "Dear %s %s,\n\n" +
                        "Your %s request for collaboration service '%s' (ID: %d) has been rejected by the administrator.\n\n" +
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
                        "The Administration Team",
                request.getPartner().getFirstName(),
                request.getPartner().getLastName(),
                type,
                request.getService().getName(),
                request.getService().getId(),
                rejectionReason,
                rejectedDate,
                request.getReason() != null ? request.getReason() : "Not specified"
        );

        createNotificationForUser(
                title,
                message,
                Role.LOCAL_PARTNER,
                request.getPartner().getId(),
                request.getService().getId()
        );

        log.info("✅ Notification sent to partner {} with reason: {}",
                request.getPartner().getEmail(), rejectionReason);
    }
    /**
     * Partenaire local - Service de collaboration supprimé par admin
     */
    @Transactional
    public void notifyPartnerCollaborationServiceDeletedByAdmin(CollaborationService service) {
        log.info("👤 Notification PARTENAIRE - Service collaboration supprimé par admin: {}", service.getId());

        String title = "🗑️ Service de collaboration supprimé par l'admin";
        String message = String.format(
                "Votre service de collaboration '%s' a été supprimé par un administrateur.",
                service.getName()
        );

        createNotificationForUser(
                title,
                message,
                Role.LOCAL_PARTNER,
                service.getProvider().getId(),
                service.getId()
        );
    }

    /**
     * Partenaire local - Rappel d'expiration d'autorisation (Collaboration)
     */
    @Transactional
    public void notifyPartnerCollaborationAuthorizationExpiring(CollaborationServiceRequest request) {
        log.info("👤 Notification PARTENAIRE - Autorisation collaboration bientôt expirée: {}", request.getId());

        String type = request.getRequestType() == RequestType.EDIT ? "modifier" : "supprimer";
        String title = "⚠️ Autorisation bientôt expirée (Collaboration)";
        String message = String.format(
                "L'autorisation de %s le service de collaboration '%s' expire le %s.\n" +
                        "Effectuez l'action rapidement.",
                type,
                request.getService().getName(),
                request.getExecutionDate() != null ? request.getExecutionDate().toLocalDate().toString() : "bientôt"
        );

        createNotificationForUser(
                title,
                message,
                Role.LOCAL_PARTNER,
                request.getPartner().getId(),
                request.getService().getId()
        );
    }

    /**
     * Partenaire local - Autorisation expirée (Collaboration)
     */
    @Transactional
    public void notifyPartnerCollaborationAuthorizationExpired(CollaborationServiceRequest request) {
        log.info("👤 Notification PARTENAIRE - Autorisation collaboration expirée: {}", request.getId());

        String type = request.getRequestType() == RequestType.EDIT ? "modifier" : "supprimer";
        String title = "⏰ Autorisation expirée (Collaboration)";
        String message = String.format(
                "L'autorisation de %s le service de collaboration '%s' a expiré.\n" +
                        "Vous devez faire une nouvelle demande si vous souhaitez toujours %s ce service.",
                type,
                request.getService().getName(),
                type
        );

        createNotificationForUser(
                title,
                message,
                Role.LOCAL_PARTNER,
                request.getPartner().getId(),
                request.getService().getId()
        );
    }

    /**
     * Admin - Service de collaboration modifié par le partenaire
     */
    @Transactional
    public void notifyAdminCollaborationServiceEdited(CollaborationService service, LocalPartner partner, CollaborationService oldService) {
        log.info("👤 Notification ADMIN - Service collaboration modifié: {}", service.getId());

        String title = "✏️ Service de collaboration modifié par le partenaire";
        String message = String.format(
                "Le service de collaboration '%s' a été modifié par %s %s.\n" +
                        "Veuillez vérifier les modifications.",
                service.getName(),
                partner.getFirstName(),
                partner.getLastName()
        );

        createNotificationForRole(title, message, Role.ADMIN, service.getId());
    }

    /**
     * Admin - Demande de collaboration traitée (approbation/rejet)
     */
    @Transactional
    public void notifyAdminCollaborationRequestProcessed(Admin admin, CollaborationServiceRequest request, String action) {
        log.info("👤 Notification ADMIN - Demande collaboration traitée: {}", request.getId());

        String title = action.equals("approuvée") ? "✅ Demande approuvée" : "❌ Demande rejetée";
        String message = String.format(
                "Vous avez %s la demande de %s pour le service '%s'.",
                action,
                request.getRequestType() == RequestType.EDIT ? "modification" : "suppression",
                request.getService().getName()
        );

        createNotificationForUser(
                title,
                message,
                Role.ADMIN,
                admin.getId(),
                request.getService().getId()
        );
    }

    @Transactional
    public void notifyCollaborationServiceModified(CollaborationService service, LocalPartner partner) {
        log.info("📢 Notification de modification de service collaboration - ID: {}", service.getId());

        // 1. NOTIFICATION À L'ADMIN
        String adminTitle = "✏️ Service de collaboration modifié";
        String adminMessage = String.format(
                "Le partenaire %s %s a modifié le service de collaboration '%s'.\n" +
                        "Veuillez vérifier les modifications.",
                partner.getFirstName(),
                partner.getLastName(),
                service.getName()
        );

        createNotificationForRole(adminTitle, adminMessage, Role.ADMIN, service.getId());

        // 2. NOTIFICATION AUX PARTENAIRES ÉCONOMIQUES
        String partnerTitle = "🔄 Service de collaboration mis à jour";
        String partnerMessage = String.format(
                "Le service de collaboration '%s' a été mis à jour.\n" +
                        "Budget: %s TND | Région: %s | Domaine: %s",
                service.getName(),
                service.getRequestedBudget(),
                service.getRegion() != null ? service.getRegion().getName() : "N/A",
                service.getActivityDomain() != null ? service.getActivityDomain().toString() : "N/A"
        );

        createNotificationForRole(partnerTitle, partnerMessage, Role.PARTNER, service.getId());

        // 3. NOTIFICATION AUX SOCIÉTÉS INTERNATIONALES
        String companyTitle = "🔄 Service de collaboration mis à jour";
        String companyMessage = String.format(
                "Le service de collaboration '%s' a été mis à jour.\n" +
                        "Budget: %s TND | Région: %s | Domaine: %s",
                service.getName(),
                service.getRequestedBudget(),
                service.getRegion() != null ? service.getRegion().getName() : "N/A",
                service.getActivityDomain() != null ? service.getActivityDomain().toString() : "N/A"
        );

        createNotificationForRole(companyTitle, companyMessage, Role.INTERNATIONAL_COMPANY, service.getId());

        log.info("✅ Notifications de modification envoyées - Admin, Partenaires économiques, Sociétés internationales");
    }

    /**
     * Notifier toutes les parties prenantes qu'un service de collaboration a été supprimé
     */
    @Transactional
    public void notifyCollaborationServiceDeleted(String serviceName, Long serviceId, LocalPartner partner, boolean isAdmin) {
        log.info("📢 Notification de suppression de service collaboration - ID: {}", serviceId);

        // 1. NOTIFICATION À L'ADMIN
        String adminTitle = "🗑️ Service de collaboration supprimé";
        String adminMessage;

        if (isAdmin) {
            adminMessage = String.format(
                    "Vous avez supprimé le service de collaboration '%s'.",
                    serviceName
            );
        } else {
            adminMessage = String.format(
                    "Le partenaire %s %s a supprimé le service de collaboration '%s' (autorisation approuvée).",
                    partner.getFirstName(),
                    partner.getLastName(),
                    serviceName
            );
        }

        createNotificationForRole(adminTitle, adminMessage, Role.ADMIN, serviceId);

        // 2. NOTIFICATION AUX PARTENAIRES ÉCONOMIQUES
        String partnerTitle = "🗑️ Service de collaboration supprimé";
        String partnerMessage = String.format(
                "Le service de collaboration '%s' a été supprimé et n'est plus disponible.",
                serviceName
        );

        createNotificationForRole(partnerTitle, partnerMessage, Role.PARTNER, serviceId);

        // 3. NOTIFICATION AUX SOCIÉTÉS INTERNATIONALES
        String companyTitle = "🗑️ Service de collaboration supprimé";
        String companyMessage = String.format(
                "Le service de collaboration '%s' a été supprimé et n'est plus disponible.",
                serviceName
        );

        createNotificationForRole(companyTitle, companyMessage, Role.INTERNATIONAL_COMPANY, serviceId);

        log.info("✅ Notifications de suppression envoyées - Admin, Partenaires économiques, Sociétés internationales");
    }

    // ========================================
// NOTIFICATIONS POUR DEMANDES TOURISTIQUES
// ========================================

    // 1. Admin - Nouvelle demande (EDIT ou DELETE)
    public void notifyAdminNewTouristRequest(TouristServiceRequest request) {
        String type = request.getRequestType() == RequestType.EDIT ? "modification" : "suppression";
        String title = "📋 Nouvelle demande de " + type + " - Service touristique";
        String message = String.format(
                "Le partenaire %s %s demande la %s du service '%s'.\n" +
                        "Raison: %s\n" +
                        "Détails: %s",
                request.getPartner().getFirstName(),
                request.getPartner().getLastName(),
                type,
                request.getService().getName(),
                request.getReason(),
                request.getRequestedChanges() != null ? request.getRequestedChanges() : "N/A"
        );

        createNotificationForRole(title, message, Role.ADMIN, request.getService().getId());
    }

    // 2. Partenaire - Demande de modification approuvée
    public void notifyPartnerTouristEditApproved(TouristServiceRequest request) {
        String title = "✅ Demande de modification approuvée";
        String message = String.format(
                "Votre demande de modification pour le service '%s' a été approuvée par l'administrateur.\n" +
                        "Vous pouvez modifier ce service jusqu'au %s.\n" +
                        "Raison initiale: %s",
                request.getService().getName(),
                request.getService().getEditAuthorizedUntil().toLocalDate().toString(),
                request.getReason()
        );

        createNotificationForUser(title, message, Role.LOCAL_PARTNER,
                request.getPartner().getId(), request.getService().getId());
    }

    // 3. Partenaire - Demande de suppression approuvée
    public void notifyPartnerTouristDeleteApproved(TouristServiceRequest request) {
        String title = "✅ Demande de suppression approuvée";
        String message = String.format(
                "Votre demande de suppression pour le service '%s' a été approuvée par l'administrateur.\n" +
                        "Vous pouvez supprimer ce service.\n" +
                        "Raison initiale: %s",
                request.getService().getName(),
                request.getReason()
        );

        createNotificationForUser(title, message, Role.LOCAL_PARTNER,
                request.getPartner().getId(), request.getService().getId());
    }

    // 4. Partenaire - Demande rejetée
    public void notifyPartnerTouristRequestRejected(TouristServiceRequest request) {
        String type = request.getRequestType() == RequestType.EDIT ? "modification" : "suppression";
        String title = "❌ Demande de " + type + " rejetée";
        String message = String.format(
                "Votre demande de %s pour le service '%s' a été rejetée par l'administrateur.\n" +
                        "Raison de votre demande: %s\n" +
                        "Commentaire de l'admin: %s",
                type,
                request.getService().getName(),
                request.getReason(),
                request.getAdminComment() != null ? request.getAdminComment() : "Aucun commentaire"
        );

        createNotificationForUser(title, message, Role.LOCAL_PARTNER,
                request.getPartner().getId(), request.getService().getId());
    }

}