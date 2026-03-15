package tn.iset.investplatformpfe.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.investplatformpfe.Entity.Notification;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Entity.CollaborationService;
import tn.iset.investplatformpfe.Repository.NotificationRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // ========================================
    // CRÉER UNE NOTIFICATION POUR UN UTILISATEUR SPÉCIFIQUE
    // ========================================
    @Transactional
    public Notification createNotificationForUser(String title, String message,
                                                  Role recipientRole, Long recipientId,
                                                  Long serviceId) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRecipientRole(recipientRole);
        notification.setRecipientId(recipientId);
        notification.setServiceId(serviceId);
        notification.setRead(false);
        // createdAt sera set automatiquement par @PrePersist

        return notificationRepository.save(notification);
    }

    // ========================================
    // CRÉER UNE NOTIFICATION POUR TOUS LES UTILISATEURS D'UN RÔLE (BROADCAST)
    // ========================================
    @Transactional
    public Notification createNotificationForRole(String title, String message,
                                                  Role recipientRole,
                                                  Long serviceId) {
        Notification notification = new Notification();
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setRecipientRole(recipientRole);
        notification.setRecipientId(null); // null = pour tous les utilisateurs de ce rôle
        notification.setServiceId(serviceId);
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    // ========================================
    // NOTIFICATION 1: Nouveau service créé (vers ADMIN)
    // ========================================
    @Transactional
    public void notifyAdminNewService(CollaborationService service) {
        String title = "New Service Pending Approval";
        String message = String.format("Service '%s' created by %s %s is waiting for your approval",
                service.getName(),
                service.getProvider().getPrenom(),
                service.getProvider().getNom());

        createNotificationForRole(title, message, Role.ADMIN, service.getId());
    }

    // ========================================
    // NOTIFICATION 2: Service approuvé (vers le créateur)
    // ========================================
    @Transactional
    public void notifyLocalPartnerServiceApproved(CollaborationService service) {
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
    // NOTIFICATION 3: Service rejeté (vers le créateur)
    // ========================================
    @Transactional
    public void notifyLocalPartnerServiceRejected(CollaborationService service) {
        String title = "Service Rejected";
        String message = String.format("Your service '%s' has been rejected. Please contact admin for more information.",
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
    // NOTIFICATION 4: Nouvelle opportunité (vers PARTNER et INTERNATIONAL_COMPANY)
    // ========================================
    @Transactional
    public void notifyPartnersAndCompaniesNewOpportunity(CollaborationService service) {
        String title = "New Collaboration Opportunity!";
        String message = String.format("A new collaboration opportunity '%s' is now available in %s region",
                service.getName(),
                service.getRegion());

        // Pour tous les PARTNER
        createNotificationForRole(title, message, Role.PARTNER, service.getId());

        // Pour tous les INTERNATIONAL_COMPANY
        createNotificationForRole(title, message, Role.INTERNATIONAL_COMPANY, service.getId());
    }

    // ========================================
    // RÉCUPÉRER LES NOTIFICATIONS D'UN UTILISATEUR (PERSONNELLES + BROADCAST)
    // ========================================
    public List<Notification> getUserNotifications(Role role, Long userId) {
        // Récupérer les notifications personnelles (destinées spécifiquement à cet utilisateur)
        List<Notification> personal = notificationRepository
                .findByRecipientRoleAndRecipientIdOrderByCreatedAtDesc(role, userId);

        // Récupérer les notifications broadcast (pour tout le rôle, recipientId = null)
        List<Notification> broadcast = notificationRepository
                .findByRecipientRoleAndRecipientIdIsNullOrderByCreatedAtDesc(role);

        // Combiner les deux listes
        personal.addAll(broadcast);

        // Trier par date de création (du plus récent au plus ancien)
        personal.sort((n1, n2) -> n2.getCreatedAt().compareTo(n1.getCreatedAt()));

        return personal;
    }

    // ========================================
    // RÉCUPÉRER LES NOTIFICATIONS NON LUES D'UN UTILISATEUR
    // ========================================
    public List<Notification> getUnreadNotifications(Role role, Long userId) {
        // Récupérer les notifications personnelles non lues
        List<Notification> personal = notificationRepository
                .findByRecipientRoleAndRecipientIdAndIsReadFalseOrderByCreatedAtDesc(role, userId);

        // Récupérer les notifications broadcast non lues
        List<Notification> broadcast = notificationRepository
                .findByRecipientRoleAndRecipientIdIsNullOrderByCreatedAtDesc(role)
                .stream()
                .filter(n -> !n.isRead())
                .collect(Collectors.toList());

        // Combiner les deux listes
        personal.addAll(broadcast);

        // Trier par date de création
        personal.sort((n1, n2) -> n2.getCreatedAt().compareTo(n1.getCreatedAt()));

        return personal;
    }

    // ========================================
    // MARQUER UNE NOTIFICATION COMME LUE
    // ========================================
    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found with id: " + notificationId));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    // ========================================
    // MARQUER TOUTES LES NOTIFICATIONS COMME LUES POUR UN UTILISATEUR
    // ========================================
    @Transactional
    public void markAllAsRead(Role role, Long userId) {
        List<Notification> unread = getUnreadNotifications(role, userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    // ========================================
    // COMPTER LES NOTIFICATIONS NON LUES
    // ========================================
    public long countUnread(Role role, Long userId) {
        return getUnreadNotifications(role, userId).size();
    }

    // ========================================
    // RÉCUPÉRER TOUTES LES NOTIFICATIONS (POUR DEBUG)
    // ========================================
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }
}