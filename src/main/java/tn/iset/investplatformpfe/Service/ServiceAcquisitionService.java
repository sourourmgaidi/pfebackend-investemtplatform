package tn.iset.investplatformpfe.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Entity.TouristService;
import tn.iset.investplatformpfe.Repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ServiceAcquisitionService {

    private static final Logger log = LoggerFactory.getLogger(ServiceAcquisitionService.class);

    private static final int REMINDER_AFTER_HOURS = 24;

    private final ServiceAcquisitionRepository acquisitionRepo;
    private final InvestmentServiceRepository investmentRepo;
    private final CollaborationServiceRepository collaborationRepo;
    private final TouristServiceRepository touristServiceRepo;
    private final NotificationService notificationService;

    public ServiceAcquisitionService(
            ServiceAcquisitionRepository acquisitionRepo,
            InvestmentServiceRepository investmentRepo,
            CollaborationServiceRepository collaborationRepo,
            TouristServiceRepository touristServiceRepo,
            NotificationService notificationService) {
        this.acquisitionRepo = acquisitionRepo;
        this.investmentRepo = investmentRepo;
        this.collaborationRepo = collaborationRepo;
        this.touristServiceRepo = touristServiceRepo;
        this.notificationService = notificationService;
    }

    // ========================================
    // ÉTAPE 1 — User envoie une demande
    // ========================================
    @Transactional
    public Map<String, Object> initiateAcquisition(
            String serviceType, Long serviceId, Long acquirerId,
            String acquirerEmail, Role acquirerRole, double amount) {

        log.info("🔵 Demande acquisition - Type:{}, ServiceId:{}, Acquirer:{}",
                serviceType, serviceId, acquirerEmail);

        // ✅ Vérification du rôle selon le type de service
        if ("TOURIST".equals(serviceType)) {
            if (acquirerRole != Role.TOURIST) {
                throw new RuntimeException(
                        "Only tourists can acquire tourist services.");
            }
        } else {
            if (acquirerRole != Role.INVESTOR
                    && acquirerRole != Role.INTERNATIONAL_COMPANY
                    && acquirerRole != Role.PARTNER) {
                throw new RuntimeException(
                        "Only investors, international companies and economic partners can acquire services.");
            }
        }

        if (acquisitionRepo.existsByServiceIdAndServiceTypeAndPaymentStatus(
                serviceId, serviceType, PaymentStatus.COMPLETED)) {
            throw new RuntimeException("This service has already been acquired.");
        }

        // ✅ Nettoyer les anciennes acquisitions CANCELLED ou PARTNER_REJECTED
        List<ServiceAcquisition> oldCancelled = acquisitionRepo
                .findByServiceIdAndServiceTypeAndAcquirerId(serviceId, serviceType, acquirerId);

        oldCancelled.stream()
                .filter(a -> a.getPaymentStatus() == PaymentStatus.CANCELLED
                        || a.getPaymentStatus() == PaymentStatus.PARTNER_REJECTED)
                .forEach(acquisitionRepo::delete);

        if (acquisitionRepo.existsByServiceIdAndServiceTypeAndPaymentStatus(
                serviceId, serviceType, PaymentStatus.PENDING_PARTNER_APPROVAL)) {
            throw new RuntimeException("A request is already pending for this service.");
        }

        if (acquisitionRepo.existsByServiceIdAndServiceTypeAndPaymentStatus(
                serviceId, serviceType, PaymentStatus.AWAITING_VALIDATION)) {
            throw new RuntimeException("This service is awaiting partner validation.");
        }

        ServiceInfo serviceInfo = getServiceInfo(serviceType, serviceId);
        if (serviceInfo == null) {
            throw new RuntimeException("Service not found.");
        }

        log.info("🔍 Service status: {}", serviceInfo.status);

        if (serviceInfo.status != ServiceStatus.APPROVED) {
            log.error("❌ Service non disponible - Status: {}", serviceInfo.status);
            throw new RuntimeException("Service is not available for acquisition. Status: " + serviceInfo.status);
        }

        markServiceAsPendingAcquisition(serviceType, serviceId);

        String orderId = "ACQ-" + serviceType + "-" + serviceId + "-" + System.currentTimeMillis();

        ServiceAcquisition acquisition = new ServiceAcquisition();
        acquisition.setServiceType(serviceType);
        acquisition.setServiceId(serviceId);
        acquisition.setServiceName(serviceInfo.name);
        acquisition.setAcquirerId(acquirerId);
        acquisition.setAcquirerEmail(acquirerEmail);
        acquisition.setAcquirerRole(acquirerRole);
        acquisition.setAmount(amount);
        acquisition.setPartnerId(serviceInfo.partnerId);
        acquisition.setOrderId(orderId);
        acquisition.setPaymentStatus(PaymentStatus.PENDING_PARTNER_APPROVAL);
        acquisitionRepo.save(acquisition);

        notificationService.createNotificationForUser(
                "📥 New acquisition request",
                acquirerEmail + " wants to acquire your service '" + serviceInfo.name
                        + "'. Please review and approve or reject in your dashboard.",
                Role.LOCAL_PARTNER,
                serviceInfo.partnerId,
                serviceId
        );

        log.info("✅ Demande créée - partner ID:{}, service → PENDING_ACQUISITION", serviceInfo.partnerId);
        return Map.of(
                "message", "Your request has been sent to the local partner for approval.",
                "status", "PENDING_PARTNER_APPROVAL"
        );
    }

    // ========================================
    // ÉTAPE 2A — Local Partner APPROUVE
    // ========================================
    @Transactional
    public Map<String, Object> partnerApprove(Long acquisitionId, Long partnerId) {
        ServiceAcquisition acquisition = acquisitionRepo.findById(acquisitionId)
                .orElseThrow(() -> new RuntimeException("Acquisition not found"));

        if (!acquisition.getPartnerId().equals(partnerId))
            throw new RuntimeException("Not authorized to approve this request.");

        if (acquisition.getPaymentStatus() != PaymentStatus.PENDING_PARTNER_APPROVAL)
            throw new RuntimeException("This request is not pending approval.");

        markServiceAsReserved(acquisition);

        acquisition.setPaymentStatus(PaymentStatus.AWAITING_VALIDATION);
        acquisition.setApprovedAt(LocalDateTime.now());
        acquisitionRepo.save(acquisition);

        notificationService.createNotificationForUser(
                "✅ Request approved — Please proceed with payment",
                "Your request for service '" + acquisition.getServiceName()
                        + "' has been approved by the local partner!\n\n"
                        + "✅ The service is now RESERVED for you.\n\n"
                        + "Please arrange your payment with the local partner and notify them once done. "
                        + "The partner will then validate and finalize your acquisition.",
                acquisition.getAcquirerRole(),
                acquisition.getAcquirerId(),
                acquisition.getServiceId()
        );

        log.info("✅ Demande {} approuvée → AWAITING_VALIDATION, service RESERVED", acquisitionId);
        return Map.of(
                "message", "Request approved. User has been notified. Click 'Validate' once payment is received.",
                "status", "AWAITING_VALIDATION"
        );
    }

    // ========================================
    // ÉTAPE 3 — Local Partner VALIDE
    // ========================================
    @Transactional
    public Map<String, Object> partnerValidate(Long acquisitionId, Long partnerId) {
        ServiceAcquisition acquisition = acquisitionRepo.findById(acquisitionId)
                .orElseThrow(() -> new RuntimeException("Acquisition not found"));

        if (!acquisition.getPartnerId().equals(partnerId))
            throw new RuntimeException("Not authorized to validate this acquisition.");

        if (acquisition.getPaymentStatus() != PaymentStatus.AWAITING_VALIDATION)
            throw new RuntimeException("This acquisition is not awaiting validation. Status: "
                    + acquisition.getPaymentStatus());

        acquisition.setPaymentStatus(PaymentStatus.COMPLETED);
        acquisition.setPaidAt(LocalDateTime.now());
        acquisitionRepo.save(acquisition);

        markServiceAsTaken(acquisition);

        notificationService.createNotificationForUser(
                "🎉 Service acquisition confirmed!",
                "The local partner has validated your acquisition of service '"
                        + acquisition.getServiceName() + "'.\n\n"
                        + "✅ The service is now officially yours (TAKEN).\n"
                        + "You can now access it from your dashboard.",
                acquisition.getAcquirerRole(),
                acquisition.getAcquirerId(),
                acquisition.getServiceId()
        );

        log.info("✅ Acquisition {} validée par partner {} → COMPLETED, service TAKEN",
                acquisitionId, partnerId);
        return Map.of(
                "message", "Service acquisition validated. Service is now TAKEN.",
                "acquisitionId", acquisition.getId(),
                "serviceType", acquisition.getServiceType(),
                "serviceId", acquisition.getServiceId(),
                "serviceName", acquisition.getServiceName()
        );
    }

    // ========================================
    // ÉTAPE 2B — Local Partner REFUSE
    // ========================================
    @Transactional
    public Map<String, Object> partnerReject(Long acquisitionId, Long partnerId, String reason) {
        ServiceAcquisition acquisition = acquisitionRepo.findById(acquisitionId)
                .orElseThrow(() -> new RuntimeException("Acquisition not found"));

        if (!acquisition.getPartnerId().equals(partnerId))
            throw new RuntimeException("Not authorized to reject this request.");

        if (acquisition.getPaymentStatus() != PaymentStatus.PENDING_PARTNER_APPROVAL)
            throw new RuntimeException("This request is not pending approval.");

        acquisition.setPaymentStatus(PaymentStatus.PARTNER_REJECTED);
        acquisition.setRejectionReason(reason);
        acquisitionRepo.save(acquisition);

        // ✅ Remet le service en APPROVED + notifie les utilisateurs concernés
        releasePendingAcquisition(acquisition);

        notificationService.createNotificationForUser(
                "❌ Acquisition request rejected",
                "Your request for service '" + acquisition.getServiceName()
                        + "' has been rejected by the local partner.\n\nReason: " + reason,
                acquisition.getAcquirerRole(),
                acquisition.getAcquirerId(),
                acquisition.getServiceId()
        );

        log.info("❌ Demande {} rejetée - Raison: {}, service → APPROVED", acquisitionId, reason);
        return Map.of("message", "Request rejected.", "reason", reason);
    }

    // ========================================
    // TÂCHE PLANIFIÉE — Reminder
    // ========================================
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void sendPaymentReminders() {
        LocalDateTime reminderThreshold = LocalDateTime.now().minusHours(REMINDER_AFTER_HOURS);

        List<ServiceAcquisition> toRemind = acquisitionRepo
                .findAwaitingValidationNeedingReminder(reminderThreshold);

        if (toRemind.isEmpty()) return;

        log.info("⏰ {} reminder(s) à envoyer...", toRemind.size());

        for (ServiceAcquisition acquisition : toRemind) {
            notificationService.createNotificationForUser(
                    "⏰ Reminder — Please finalize your acquisition",
                    "This is a reminder that your request for service '"
                            + acquisition.getServiceName()
                            + "' has been approved by the local partner.\n\n"
                            + "✅ The service is still RESERVED for you.\n\n"
                            + "Please contact the local partner to arrange your payment "
                            + "and finalize the acquisition as soon as possible.",
                    acquisition.getAcquirerRole(),
                    acquisition.getAcquirerId(),
                    acquisition.getServiceId()
            );

            acquisition.setReminderSent(true);
            acquisitionRepo.save(acquisition);

            log.info("✅ Reminder envoyé → user:{}, acquisition:{}", acquisition.getAcquirerId(), acquisition.getId());
        }
    }

    // ========================================
    // Annulation par le user
    // ========================================
    @Transactional
    public Map<String, Object> cancelUserRequest(Long acquisitionId, Long acquirerId, String reason) {
        log.info("🚫 User {} annule l'acquisition ID: {}", acquirerId, acquisitionId);

        ServiceAcquisition acquisition = acquisitionRepo.findByIdAndAcquirerId(acquisitionId, acquirerId)
                .orElseThrow(() -> new RuntimeException("Acquisition not found or not yours."));

        PaymentStatus status = acquisition.getPaymentStatus();

        if (status != PaymentStatus.PENDING_PARTNER_APPROVAL
                && status != PaymentStatus.AWAITING_VALIDATION) {
            throw new RuntimeException("Cannot cancel this request. Status: " + status);
        }

        // ✅ Remettre le service en APPROVED + notifier les utilisateurs concernés
        if (status == PaymentStatus.PENDING_PARTNER_APPROVAL) {
            releasePendingAcquisition(acquisition);
        } else if (status == PaymentStatus.AWAITING_VALIDATION) {
            releaseService(acquisition);
        }

        // ✅ Notifier le local partner
        notificationService.createNotificationForUser(
                "🚫 Acquisition request cancelled",
                "The user " + acquisition.getAcquirerEmail()
                        + " has cancelled their request for service '"
                        + acquisition.getServiceName() + "'.\n\nReason: " + reason
                        + "\n\nThe service is now available again.",
                Role.LOCAL_PARTNER,
                acquisition.getPartnerId(),
                acquisition.getServiceId()
        );

        // ✅ Supprimer directement au lieu de mettre CANCELLED
        acquisitionRepo.delete(acquisition);

        log.info("🗑️ Acquisition {} supprimée - service remis en APPROVED", acquisitionId);
        return Map.of(
                "message", "Request cancelled and deleted successfully. The service is now available again.",
                "acquisitionId", acquisitionId,
                "reason", reason
        );
    }

    // ========================================
    // Helpers privés — status du service
    // ========================================
    private void markServiceAsPendingAcquisition(String serviceType, Long serviceId) {
        if ("INVESTMENT".equals(serviceType)) {
            InvestmentService service = investmentRepo.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Service not found"));
            service.setStatus(ServiceStatus.PENDING_ACQUISITION);
            investmentRepo.save(service);
        } else if ("COLLABORATION".equals(serviceType)) {
            CollaborationService service = collaborationRepo.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Service not found"));
            service.setStatus(ServiceStatus.PENDING_ACQUISITION);
            collaborationRepo.save(service);
        } else if ("TOURIST".equals(serviceType)) {
            TouristService service = touristServiceRepo.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Service not found"));
            service.setStatus(ServiceStatus.PENDING_ACQUISITION);
            touristServiceRepo.save(service);
        }
    }

    private void markServiceAsReserved(ServiceAcquisition acquisition) {
        String type = acquisition.getServiceType();
        Long serviceId = acquisition.getServiceId();

        if ("INVESTMENT".equals(type)) {
            InvestmentService service = investmentRepo.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Service not found"));
            if (service.getStatus() == ServiceStatus.PENDING_ACQUISITION) {
                service.setStatus(ServiceStatus.RESERVED);
                investmentRepo.save(service);
            } else {
                throw new RuntimeException("Service cannot be reserved. Status: " + service.getStatus());
            }
        } else if ("COLLABORATION".equals(type)) {
            CollaborationService service = collaborationRepo.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Service not found"));
            if (service.getStatus() == ServiceStatus.PENDING_ACQUISITION) {
                service.setStatus(ServiceStatus.RESERVED);
                collaborationRepo.save(service);
            } else {
                throw new RuntimeException("Service cannot be reserved. Status: " + service.getStatus());
            }
        } else if ("TOURIST".equals(type)) {
            TouristService service = touristServiceRepo.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Service not found"));
            if (service.getStatus() == ServiceStatus.PENDING_ACQUISITION) {
                service.setStatus(ServiceStatus.RESERVED);
                touristServiceRepo.save(service);
            } else {
                throw new RuntimeException("Service cannot be reserved. Status: " + service.getStatus());
            }
        }
    }

    private void markServiceAsTaken(ServiceAcquisition acquisition) {
        String type = acquisition.getServiceType();
        Long serviceId = acquisition.getServiceId();

        if ("INVESTMENT".equals(type)) {
            InvestmentService service = investmentRepo.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Service not found"));
            service.setStatus(ServiceStatus.TAKEN);
            investmentRepo.save(service);
            log.info("✅ InvestmentService {} → TAKEN", serviceId);
        } else if ("COLLABORATION".equals(type)) {
            CollaborationService service = collaborationRepo.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Service not found"));
            service.setStatus(ServiceStatus.TAKEN);
            collaborationRepo.save(service);
            log.info("✅ CollaborationService {} → TAKEN", serviceId);
        } else if ("TOURIST".equals(type)) {
            TouristService service = touristServiceRepo.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Service not found"));
            service.setStatus(ServiceStatus.TAKEN);
            touristServiceRepo.save(service);
            log.info("✅ TouristService {} → TAKEN", serviceId);
        }
    }

    // ========================================
    // Helper privé — remet PENDING_ACQUISITION → APPROVED
    // + notifie les utilisateurs concernés
    // ========================================
    private void releasePendingAcquisition(ServiceAcquisition acquisition) {
        String type = acquisition.getServiceType();
        Long serviceId = acquisition.getServiceId();

        if ("INVESTMENT".equals(type)) {
            investmentRepo.findById(serviceId).ifPresent(s -> {
                if (s.getStatus() == ServiceStatus.PENDING_ACQUISITION) {
                    s.setStatus(ServiceStatus.APPROVED);
                    investmentRepo.save(s);
                    log.info("✅ InvestmentService {} → APPROVED (depuis PENDING_ACQUISITION)", serviceId);
                }
            });
        } else if ("COLLABORATION".equals(type)) {
            collaborationRepo.findById(serviceId).ifPresent(s -> {
                if (s.getStatus() == ServiceStatus.PENDING_ACQUISITION) {
                    s.setStatus(ServiceStatus.APPROVED);
                    collaborationRepo.save(s);
                    log.info("✅ CollaborationService {} → APPROVED (depuis PENDING_ACQUISITION)", serviceId);
                }
            });
        } else if ("TOURIST".equals(type)) {
            touristServiceRepo.findById(serviceId).ifPresent(s -> {
                if (s.getStatus() == ServiceStatus.PENDING_ACQUISITION) {
                    s.setStatus(ServiceStatus.APPROVED);
                    touristServiceRepo.save(s);
                    log.info("✅ TouristService {} → APPROVED (depuis PENDING_ACQUISITION)", serviceId);
                }
            });
        }

        // ✅ Notifier les utilisateurs concernés que le service est à nouveau disponible
        notifyUsersServiceAvailableAgain(type, serviceId, acquisition.getServiceName());
    }

    // ========================================
    // Helper privé — remet RESERVED → APPROVED
    // + notifie les utilisateurs concernés
    // ========================================
    private void releaseService(ServiceAcquisition acquisition) {
        String type = acquisition.getServiceType();
        Long serviceId = acquisition.getServiceId();

        if ("INVESTMENT".equals(type)) {
            investmentRepo.findById(serviceId).ifPresent(s -> {
                if (s.getStatus() == ServiceStatus.RESERVED) {
                    s.setStatus(ServiceStatus.APPROVED);
                    investmentRepo.save(s);
                    log.info("✅ InvestmentService {} → APPROVED (depuis RESERVED)", serviceId);
                }
            });
        } else if ("COLLABORATION".equals(type)) {
            collaborationRepo.findById(serviceId).ifPresent(s -> {
                if (s.getStatus() == ServiceStatus.RESERVED) {
                    s.setStatus(ServiceStatus.APPROVED);
                    collaborationRepo.save(s);
                    log.info("✅ CollaborationService {} → APPROVED (depuis RESERVED)", serviceId);
                }
            });
        } else if ("TOURIST".equals(type)) {
            touristServiceRepo.findById(serviceId).ifPresent(s -> {
                if (s.getStatus() == ServiceStatus.RESERVED) {
                    s.setStatus(ServiceStatus.APPROVED);
                    touristServiceRepo.save(s);
                    log.info("✅ TouristService {} → APPROVED (depuis RESERVED)", serviceId);
                }
            });
        }

        // ✅ Notifier les utilisateurs concernés que le service est à nouveau disponible
        notifyUsersServiceAvailableAgain(type, serviceId, acquisition.getServiceName());
    }

    // ========================================
    // Helper privé — envoi des notifications
    // selon le type de service retourné APPROVED
    //
    // INVESTMENT   → INVESTOR + INTERNATIONAL_COMPANY
    // COLLABORATION → PARTNER + INTERNATIONAL_COMPANY
    // TOURIST      → TOURIST uniquement
    // ========================================
    private void notifyUsersServiceAvailableAgain(String serviceType, Long serviceId, String serviceName) {
        log.info("🔔 Envoi notifications retour disponibilité - Type:{}, ServiceId:{}", serviceType, serviceId);

        switch (serviceType) {

            case "INVESTMENT" -> {
                // Notifier les investisseurs
                notificationService.createNotificationForRole(
                        "✅ Investment Service Available Again",
                        "The investment service '" + serviceName + "' is now available again. "
                                + "You can submit a new acquisition request from your dashboard.",
                        Role.INVESTOR,
                        serviceId
                );
                // Notifier les sociétés internationales
                notificationService.createNotificationForRole(
                        "✅ Investment Service Available Again",
                        "The investment service '" + serviceName + "' is now available again. "
                                + "You can submit a new acquisition request from your dashboard.",
                        Role.INTERNATIONAL_COMPANY,
                        serviceId
                );
                log.info("🔔 Notifications envoyées → INVESTOR + INTERNATIONAL_COMPANY pour service INVESTMENT {}", serviceId);
            }

            case "COLLABORATION" -> {
                // Notifier les partenaires économiques
                notificationService.createNotificationForRole(
                        "✅ Collaboration Service Available Again",
                        "The collaboration service '" + serviceName + "' is now available again. "
                                + "You can submit a new acquisition request from your dashboard.",
                        Role.PARTNER,
                        serviceId
                );
                // Notifier les sociétés internationales
                notificationService.createNotificationForRole(
                        "✅ Collaboration Service Available Again",
                        "The collaboration service '" + serviceName + "' is now available again. "
                                + "You can submit a new acquisition request from your dashboard.",
                        Role.INTERNATIONAL_COMPANY,
                        serviceId
                );
                log.info("🔔 Notifications envoyées → PARTNER + INTERNATIONAL_COMPANY pour service COLLABORATION {}", serviceId);
            }

            case "TOURIST" -> {
                // Notifier uniquement les touristes
                notificationService.createNotificationForRole(
                        "✅ Tourist Service Available Again",
                        "The tourist service '" + serviceName + "' is now available again. "
                                + "You can submit a new acquisition request from your dashboard.",
                        Role.TOURIST,
                        serviceId
                );
                log.info("🔔 Notifications envoyées → TOURIST pour service TOURIST {}", serviceId);
            }

            default -> log.warn("⚠️ Type de service inconnu pour notification retour disponibilité: {}", serviceType);
        }
    }

    private ServiceInfo getServiceInfo(String serviceType, Long serviceId) {
        if ("INVESTMENT".equals(serviceType)) {
            return investmentRepo.findById(serviceId)
                    .map(s -> new ServiceInfo(
                            s.getTitle() != null ? s.getTitle() : s.getName(),
                            s.getProvider().getId(),
                            s.getStatus()))
                    .orElse(null);
        } else if ("COLLABORATION".equals(serviceType)) {
            return collaborationRepo.findById(serviceId)
                    .map(s -> new ServiceInfo(s.getName(), s.getProvider().getId(), s.getStatus()))
                    .orElse(null);
        } else if ("TOURIST".equals(serviceType)) {
            return touristServiceRepo.findById(serviceId)
                    .map(s -> new ServiceInfo(s.getName(), s.getProvider().getId(), s.getStatus()))
                    .orElse(null);
        }
        return null;
    }

    private static class ServiceInfo {
        final String name;
        final Long partnerId;
        final ServiceStatus status;

        ServiceInfo(String name, Long partnerId, ServiceStatus status) {
            this.name = name;
            this.partnerId = partnerId;
            this.status = status;
        }
    }

    // ========================================
    // API Publiques
    // ========================================
    public List<ServiceAcquisition> getPendingRequestsForPartner(Long partnerId) {
        return acquisitionRepo.findByPartnerIdAndPaymentStatus(
                partnerId, PaymentStatus.PENDING_PARTNER_APPROVAL);
    }

    public List<ServiceAcquisition> getAwaitingValidationForPartner(Long partnerId) {
        return acquisitionRepo.findByPartnerIdAndPaymentStatus(
                partnerId, PaymentStatus.AWAITING_VALIDATION);
    }

    public List<ServiceAcquisition> getUserAcquisitions(Long acquirerId, Role role) {
        List<ServiceAcquisition> all = acquisitionRepo.findAllByAcquirerIdAndRole(acquirerId, role);
        return all.stream()
                .filter(a -> a.getPaymentStatus() == PaymentStatus.COMPLETED)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<ServiceAcquisition> getAllUserAcquisitions(Long acquirerId, Role role) {
        return acquisitionRepo.findAllByAcquirerIdAndRole(acquirerId, role);
    }

    public boolean isServiceTaken(Long serviceId, String serviceType) {
        return acquisitionRepo.existsByServiceIdAndServiceTypeAndPaymentStatus(
                serviceId, serviceType, PaymentStatus.COMPLETED);
    }

    public boolean userHasAccess(Long serviceId, String serviceType, Long userId) {
        return acquisitionRepo.userHasAccessToService(serviceId, serviceType, userId);
    }

    public boolean partnerHasAccess(Long serviceId, String serviceType, Long partnerId) {
        return acquisitionRepo.partnerHasAccessToService(serviceId, serviceType, partnerId);
    }

    public boolean isMyService(Long serviceId, String serviceType,
                               Long acquirerId, Role role) {
        return acquisitionRepo
                .findByServiceIdAndServiceTypeAndPaymentStatus(
                        serviceId, serviceType, PaymentStatus.COMPLETED)
                .map(a -> a.getAcquirerId().equals(acquirerId)
                        && a.getAcquirerRole() == role)
                .orElse(false);
    }
    @Transactional
    public void partnerRejectAndDelete(Long acquisitionId, Long partnerId, String reason) {
        ServiceAcquisition acq = acquisitionRepo.findById(acquisitionId)
                .orElseThrow(() -> new RuntimeException("Acquisition not found"));

        if (!acq.getPartnerId().equals(partnerId))
            throw new RuntimeException("Not authorized");

        if (acq.getPaymentStatus() != PaymentStatus.AWAITING_VALIDATION)
            throw new RuntimeException("Acquisition is not awaiting validation");

        // ✅ Remettre le service RESERVED → APPROVED
        releaseService(acq);

        // ✅ Notifier l'acquéreur
        notificationService.createNotificationForUser(
                "❌ Payment rejected",
                "Your payment for service '" + acq.getServiceName()
                        + "' has been rejected by the local partner.\n\nReason: " + reason
                        + "\n\nThe service is now available again.",
                acq.getAcquirerRole(),
                acq.getAcquirerId(),
                acq.getServiceId()
        );

        // ✅ Suppression de la base de données
        acquisitionRepo.delete(acq);

        log.info("🗑️ Acquisition {} rejetée et supprimée - service remis en APPROVED", acquisitionId);
    }
    public List<ServiceAcquisition> getTakenServicesForPartner(Long partnerId) {
        return acquisitionRepo.findByPartnerIdAndPaymentStatus(
                partnerId, PaymentStatus.COMPLETED);
    }

    @Transactional
    public void deleteAcquisition(Long acquisitionId) {
        acquisitionRepo.deleteById(acquisitionId);
    }
}