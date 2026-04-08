package tn.iset.investplatformpfe.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ServiceAcquisitionService {

    private static final Logger log = LoggerFactory.getLogger(ServiceAcquisitionService.class);

    // ⏱️ Délais en heures
    private static final int RESERVATION_EXPIRATION_HOURS = 48;
    private static final int REMINDER_HOURS = 2;

    private final ServiceAcquisitionRepository acquisitionRepo;
    private final InvestmentServiceRepository investmentRepo;
    private final CollaborationServiceRepository collaborationRepo;
    private final FlouciService flouciService;
    private final NotificationService notificationService;

    public ServiceAcquisitionService(
            ServiceAcquisitionRepository acquisitionRepo,
            InvestmentServiceRepository investmentRepo,
            CollaborationServiceRepository collaborationRepo,
            FlouciService flouciService,
            NotificationService notificationService) {
        this.acquisitionRepo = acquisitionRepo;
        this.investmentRepo = investmentRepo;
        this.collaborationRepo = collaborationRepo;
        this.flouciService = flouciService;
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

        // ✅ Restriction des rôles autorisés
        if (acquirerRole != Role.INVESTOR && acquirerRole != Role.INTERNATIONAL_COMPANY) {
            throw new RuntimeException(
                    "Only investors and international companies can acquire services.");
        }

        if (acquisitionRepo.existsByServiceIdAndServiceTypeAndPaymentStatus(
                serviceId, serviceType, PaymentStatus.COMPLETED)) {
            throw new RuntimeException("This service has already been acquired.");
        }

        ServiceAcquisition existingReservation = acquisitionRepo
                .findActiveReservation(serviceId, serviceType, LocalDateTime.now())
                .orElse(null);

        if (existingReservation != null) {
            throw new RuntimeException("This service is currently reserved. Please wait.");
        }

        if (acquisitionRepo.existsByServiceIdAndServiceTypeAndPaymentStatus(
                serviceId, serviceType, PaymentStatus.PENDING_PARTNER_APPROVAL)) {
            throw new RuntimeException("A request is already pending for this service.");
        }

        ServiceInfo serviceInfo = getServiceInfo(serviceType, serviceId);
        if (serviceInfo == null) {
            throw new RuntimeException("Service not found.");
        }

        if (serviceInfo.status != ServiceStatus.APPROVED) {
            throw new RuntimeException("Service is not available for acquisition.");
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
    // Marquer le service comme PENDING_ACQUISITION
    // ========================================
    private void markServiceAsPendingAcquisition(String serviceType, Long serviceId) {
        if ("INVESTMENT".equals(serviceType)) {
            InvestmentService service = investmentRepo.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Service not found"));
            service.setStatus(ServiceStatus.PENDING_ACQUISITION);
            investmentRepo.save(service);
            log.info("✅ InvestmentService {} → PENDING_ACQUISITION", serviceId);
        } else if ("COLLABORATION".equals(serviceType)) {
            CollaborationService service = collaborationRepo.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Service not found"));
            service.setStatus(ServiceStatus.PENDING_ACQUISITION);
            collaborationRepo.save(service);
            log.info("✅ CollaborationService {} → PENDING_ACQUISITION", serviceId);
        }
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

        LocalDateTime expiresAt = LocalDateTime.now().plusHours(RESERVATION_EXPIRATION_HOURS);
        acquisition.setReservationExpiresAt(expiresAt);

        acquisition.setPaymentStatus(PaymentStatus.AWAITING_PAYMENT);
        acquisitionRepo.save(acquisition);

        Map<String, Object> flouciResponse = flouciService.initiatePayment(
                acquisition.getAmount(), acquisition.getOrderId());

        try {
            Map<String, Object> result = (Map<String, Object>) flouciResponse.get("result");
            if (result != null) {
                acquisition.setFlouciPaymentId((String) result.get("payment_id"));
                acquisition.setPaymentUrl((String) result.get("link"));
                acquisitionRepo.save(acquisition);
            }
        } catch (Exception e) {
            log.warn("⚠️ Parse Flouci échoué: {}", e.getMessage());
        }

        String paymentLink = acquisition.getPaymentUrl() != null ? acquisition.getPaymentUrl() : "";

        notificationService.createNotificationForUser(
                "✅ Request approved — Please pay within " + RESERVATION_EXPIRATION_HOURS + " hours",
                "Your request for service '" + acquisition.getServiceName()
                        + "' has been approved by the local partner!\n\n"
                        + "✅ The service is now RESERVED for you for "
                        + RESERVATION_EXPIRATION_HOURS + " hours.\n\n"
                        + "Please complete the payment before "
                        + expiresAt.toLocalDate() + " " + expiresAt.toLocalTime()
                        + " to finalize your acquisition.\n\n"
                        + "Payment link: " + paymentLink,
                acquisition.getAcquirerRole(),
                acquisition.getAcquirerId(),
                acquisition.getServiceId()
        );

        log.info("✅ Demande {} approuvée, service RESERVED jusqu'à {}, paiement initié",
                acquisitionId, expiresAt);
        return flouciResponse;
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

        // ✅ Remettre le service en APPROVED
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
    // Libérer le service PENDING_ACQUISITION → APPROVED
    // ========================================
    private void releasePendingAcquisition(ServiceAcquisition acquisition) {
        String type = acquisition.getServiceType();
        Long serviceId = acquisition.getServiceId();

        if ("INVESTMENT".equals(type)) {
            investmentRepo.findById(serviceId).ifPresent(s -> {
                if (s.getStatus() == ServiceStatus.PENDING_ACQUISITION) {
                    s.setStatus(ServiceStatus.APPROVED);
                    investmentRepo.save(s);
                    log.info("↩️ InvestmentService {} → APPROVED", serviceId);
                }
            });
        } else if ("COLLABORATION".equals(type)) {
            collaborationRepo.findById(serviceId).ifPresent(s -> {
                if (s.getStatus() == ServiceStatus.PENDING_ACQUISITION) {
                    s.setStatus(ServiceStatus.APPROVED);
                    collaborationRepo.save(s);
                    log.info("↩️ CollaborationService {} → APPROVED", serviceId);
                }
            });
        }
    }

    // ========================================
    // ÉTAPE 3 — Confirmer paiement
    // ========================================
    @Transactional
    public Map<String, Object> confirmPayment(String paymentId, String orderId) {
        log.info("✅ Confirmation paiement - PaymentId:{}, OrderId:{}", paymentId, orderId);

        ServiceAcquisition acquisition = null;

        if (paymentId != null && !paymentId.isEmpty())
            acquisition = acquisitionRepo.findByFlouciPaymentId(paymentId).orElse(null);
        if (acquisition == null && orderId != null)
            acquisition = acquisitionRepo.findByOrderId(orderId).orElse(null);
        if (acquisition == null)
            throw new RuntimeException("Acquisition not found");

        if (acquisition.getReservationExpiresAt() != null &&
                acquisition.getReservationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Reservation expired. Please initiate a new request.");
        }

        if (paymentId != null && !paymentId.startsWith("SIM_")) {
            Map<String, Object> verification = flouciService.verifyPayment(paymentId);
            Object resultObj = verification.get("result");
            if (resultObj instanceof Map) {
                String status = (String) ((Map<?, ?>) resultObj).get("status");
                if (!"SUCCESS".equals(status)) {
                    acquisition.setPaymentStatus(PaymentStatus.FAILED);
                    acquisitionRepo.save(acquisition);
                    releaseService(acquisition);
                    throw new RuntimeException("Payment not successful: " + status);
                }
            }
        }

        acquisition.setPaymentStatus(PaymentStatus.COMPLETED);
        acquisition.setPaidAt(LocalDateTime.now());
        if (paymentId != null && acquisition.getFlouciPaymentId() == null)
            acquisition.setFlouciPaymentId(paymentId);
        acquisitionRepo.save(acquisition);

        markServiceAsTaken(acquisition);
        notifyPartnerPaymentReceived(acquisition);

        log.info("✅ Paiement confirmé - Service {} → TAKEN, user:{}",
                acquisition.getServiceId(), acquisition.getAcquirerId());

        return Map.of(
                "message", "Payment confirmed. You can now access the service.",
                "acquisitionId", acquisition.getId(),
                "serviceType", acquisition.getServiceType(),
                "serviceId", acquisition.getServiceId(),
                "serviceName", acquisition.getServiceName()
        );
    }

    // ========================================
    // TÂCHE PLANIFIÉE: Expiration des réservations
    // ========================================
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void checkExpiredReservations() {
        log.info("🔍 Vérification des réservations expirées...");

        LocalDateTime now = LocalDateTime.now();
        List<ServiceAcquisition> expired = acquisitionRepo
                .findExpiredReservations(PaymentStatus.AWAITING_PAYMENT, now);

        int expiredCount = 0;
        for (ServiceAcquisition acquisition : expired) {
            log.info("⏰ Réservation expirée - ID: {}, Service: {}", acquisition.getId(), acquisition.getServiceName());

            releaseService(acquisition);
            acquisition.setPaymentStatus(PaymentStatus.EXPIRED);
            acquisitionRepo.save(acquisition);

            notificationService.createNotificationForUser(
                    "⏰ Reservation expired",
                    "Your reservation for service '" + acquisition.getServiceName()
                            + "' has expired because payment was not completed within "
                            + RESERVATION_EXPIRATION_HOURS + " hours.\n\n"
                            + "The service is now available again for others.",
                    acquisition.getAcquirerRole(),
                    acquisition.getAcquirerId(),
                    acquisition.getServiceId()
            );

            notificationService.createNotificationForUser(
                    "🔄 Service released",
                    "The reservation for service '" + acquisition.getServiceName()
                            + "' has expired. The service is now available again for others.",
                    Role.LOCAL_PARTNER,
                    acquisition.getPartnerId(),
                    acquisition.getServiceId()
            );

            expiredCount++;
        }

        if (expiredCount > 0) {
            log.info("✅ {} réservation(s) expirée(s) et libérée(s)", expiredCount);
        }

        LocalDateTime reminderStart = now.plusHours(REMINDER_HOURS);
        LocalDateTime reminderEnd = reminderStart.plusMinutes(1);

        List<ServiceAcquisition> needingReminder = acquisitionRepo
                .findReservationsNeedingReminder(reminderStart, reminderEnd);

        for (ServiceAcquisition acquisition : needingReminder) {
            sendPaymentReminder(acquisition);
            acquisition.setReminderSent(true);
            acquisitionRepo.save(acquisition);
        }
    }

    private void sendPaymentReminder(ServiceAcquisition acquisition) {
        long hoursLeft = java.time.Duration.between(
                LocalDateTime.now(),
                acquisition.getReservationExpiresAt()).toHours();

        notificationService.createNotificationForUser(
                "⏰ Payment reminder - " + hoursLeft + " hours left",
                "Your reservation for service '" + acquisition.getServiceName()
                        + "' will expire in " + hoursLeft + " hours.\n\n"
                        + "Please complete your payment before "
                        + acquisition.getReservationExpiresAt().toLocalTime()
                        + " to finalize your acquisition.\n\n"
                        + "Payment link: " + acquisition.getPaymentUrl(),
                acquisition.getAcquirerRole(),
                acquisition.getAcquirerId(),
                acquisition.getServiceId()
        );
    }

    // ========================================
    // Marquer le service comme RESERVÉ
    // ========================================
    private void markServiceAsReserved(ServiceAcquisition acquisition) {
        String type = acquisition.getServiceType();
        Long serviceId = acquisition.getServiceId();

        if ("INVESTMENT".equals(type)) {
            InvestmentService service = investmentRepo.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Service not found"));
            if (service.getStatus() == ServiceStatus.PENDING_ACQUISITION) {
                service.setStatus(ServiceStatus.RESERVED);
                investmentRepo.save(service);
                log.info("✅ InvestmentService {} → RESERVED", serviceId);
            } else {
                throw new RuntimeException("Service cannot be reserved. Current status: " + service.getStatus());
            }
        } else if ("COLLABORATION".equals(type)) {
            CollaborationService service = collaborationRepo.findById(serviceId)
                    .orElseThrow(() -> new RuntimeException("Service not found"));
            if (service.getStatus() == ServiceStatus.PENDING_ACQUISITION) {
                service.setStatus(ServiceStatus.RESERVED);
                collaborationRepo.save(service);
                log.info("✅ CollaborationService {} → RESERVED", serviceId);
            } else {
                throw new RuntimeException("Service cannot be reserved. Current status: " + service.getStatus());
            }
        }
    }

    // ========================================
    // Marquer le service comme TAKEN
    // ========================================
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
        }
    }

    // ========================================
    // Libérer le service (RESERVED → APPROVED)
    // ========================================
    private void releaseService(ServiceAcquisition acquisition) {
        String type = acquisition.getServiceType();
        Long serviceId = acquisition.getServiceId();

        if ("INVESTMENT".equals(type)) {
            investmentRepo.findById(serviceId).ifPresent(s -> {
                if (s.getStatus() == ServiceStatus.RESERVED) {
                    s.setStatus(ServiceStatus.APPROVED);
                    investmentRepo.save(s);
                    log.info("↩️ InvestmentService {} → APPROVED", serviceId);
                }
            });
        } else if ("COLLABORATION".equals(type)) {
            collaborationRepo.findById(serviceId).ifPresent(s -> {
                if (s.getStatus() == ServiceStatus.RESERVED) {
                    s.setStatus(ServiceStatus.APPROVED);
                    collaborationRepo.save(s);
                    log.info("↩️ CollaborationService {} → APPROVED", serviceId);
                }
            });
        }
    }

    // ========================================
    // Annuler paiement
    // ========================================
    @Transactional
    public void cancelPayment(String paymentId, String orderId) {
        ServiceAcquisition acq = null;
        if (paymentId != null)
            acq = acquisitionRepo.findByFlouciPaymentId(paymentId).orElse(null);
        if (acq == null && orderId != null)
            acq = acquisitionRepo.findByOrderId(orderId).orElse(null);
        if (acq != null && acq.getPaymentStatus() == PaymentStatus.AWAITING_PAYMENT) {
            acq.setPaymentStatus(PaymentStatus.CANCELLED);
            acquisitionRepo.save(acq);
            releaseService(acq);
            log.info("❌ Paiement annulé, service libéré");
        }
    }

    private void notifyPartnerPaymentReceived(ServiceAcquisition acquisition) {
        notificationService.createNotificationForUser(
                "💰 Payment received",
                acquisition.getAcquirerEmail()
                        + " has completed the payment for your service '"
                        + acquisition.getServiceName() + "'.\n"
                        + "Amount: " + acquisition.getAmount() + " TND\n\n"
                        + "The service is now officially acquired.",
                Role.LOCAL_PARTNER,
                acquisition.getPartnerId(),
                acquisition.getServiceId()
        );
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

    public List<ServiceAcquisition> getUserAcquisitions(Long acquirerId, Role role) {
        log.info("📋 getUserAcquisitions (services payés) - acquirerId={}, role={}", acquirerId, role);

        // Récupérer TOUTES les acquisitions
        List<ServiceAcquisition> all = acquisitionRepo.findAllByAcquirerIdAndRole(acquirerId, role);

        // Filtrer pour garder uniquement les COMPLETED (payés)
        List<ServiceAcquisition> completed = all.stream()
                .filter(a -> a.getPaymentStatus() == PaymentStatus.COMPLETED)
                .collect(java.util.stream.Collectors.toList());

        log.info("✅ {} services payés trouvés sur {} total", completed.size(), all.size());
        completed.forEach(a -> log.info("  → serviceId={} | status={} | type={}",
                a.getServiceId(), a.getPaymentStatus(), a.getServiceType()));

        return completed;
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
    public Map<String, Object> cancelUserRequest(Long acquisitionId, Long acquirerId, String reason) {
        log.info("🚫 User {} annule l'acquisition ID: {}", acquirerId, acquisitionId);

        ServiceAcquisition acquisition = acquisitionRepo.findByIdAndAcquirerId(acquisitionId, acquirerId)
                .orElseThrow(() -> new RuntimeException("Acquisition not found or not yours."));

        PaymentStatus status = acquisition.getPaymentStatus();

        if (status != PaymentStatus.PENDING_PARTNER_APPROVAL
                && status != PaymentStatus.AWAITING_PAYMENT) {
            throw new RuntimeException(
                    "Cannot cancel this request. Status: " + status
                            + ". Only PENDING_PARTNER_APPROVAL or AWAITING_PAYMENT can be cancelled.");
        }

        if (status == PaymentStatus.PENDING_PARTNER_APPROVAL) {
            releasePendingAcquisition(acquisition);
        } else if (status == PaymentStatus.AWAITING_PAYMENT) {
            releaseService(acquisition);
        }

        acquisition.setPaymentStatus(PaymentStatus.CANCELLED);
        acquisition.setRejectionReason("Cancelled by user: " + reason);
        acquisitionRepo.save(acquisition);

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

        log.info("🚫 Acquisition {} annulée par user {} - Raison: {}", acquisitionId, acquirerId, reason);

        return Map.of(
                "message", "Request cancelled successfully. The service is now available again.",
                "acquisitionId", acquisitionId,
                "reason", reason
        );
    }
    @Transactional
    public void deleteAcquisition(Long acquisitionId) {
        acquisitionRepo.deleteById(acquisitionId);
    }


}