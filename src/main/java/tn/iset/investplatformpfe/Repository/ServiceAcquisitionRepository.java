package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.iset.investplatformpfe.Entity.PaymentStatus;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Entity.ServiceAcquisition;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ServiceAcquisitionRepository
        extends JpaRepository<ServiceAcquisition, Long> {

    Optional<ServiceAcquisition> findByFlouciPaymentId(String flouciPaymentId);
    Optional<ServiceAcquisition> findByOrderId(String orderId);

    boolean existsByServiceIdAndServiceTypeAndPaymentStatus(
            Long serviceId, String serviceType, PaymentStatus status);

    List<ServiceAcquisition> findByAcquirerIdAndAcquirerRole(
            Long acquirerId, Role role);

    List<ServiceAcquisition> findByPartnerIdAndPaymentStatus(
            Long partnerId, PaymentStatus status);

    Optional<ServiceAcquisition> findByServiceIdAndServiceTypeAndPaymentStatus(
            Long serviceId, String serviceType, PaymentStatus status);

    // Trouver toutes les acquisitions d'un service (pour vérifier visibilité)
    List<ServiceAcquisition> findByServiceIdAndServiceType(
            Long serviceId, String serviceType);

    // Vérifier si user a accès à un service (payé)
    @Query("SELECT COUNT(a) > 0 FROM ServiceAcquisition a " +
            "WHERE a.serviceId = :serviceId " +
            "AND a.serviceType = :serviceType " +
            "AND a.acquirerId = :acquirerId " +
            "AND a.paymentStatus = 'COMPLETED'")
    boolean userHasAccessToService(
            @Param("serviceId") Long serviceId,
            @Param("serviceType") String serviceType,
            @Param("acquirerId") Long acquirerId);

    // Vérifier si partner a accès (son propre service pris)
    @Query("SELECT COUNT(a) > 0 FROM ServiceAcquisition a " +
            "WHERE a.serviceId = :serviceId " +
            "AND a.serviceType = :serviceType " +
            "AND a.partnerId = :partnerId " +
            "AND a.paymentStatus = 'COMPLETED'")
    boolean partnerHasAccessToService(
            @Param("serviceId") Long serviceId,
            @Param("serviceType") String serviceType,
            @Param("partnerId") Long partnerId);

    // ✅ Trouver les réservations expirées
    @Query("SELECT a FROM ServiceAcquisition a WHERE a.paymentStatus = :status " +
            "AND a.reservationExpiresAt < :now")
    List<ServiceAcquisition> findExpiredReservations(
            @Param("status") PaymentStatus status,
            @Param("now") LocalDateTime now);

    // ✅ Trouver une réservation active pour un service
    @Query("SELECT a FROM ServiceAcquisition a WHERE a.serviceId = :serviceId " +
            "AND a.serviceType = :serviceType " +
            "AND a.paymentStatus = 'AWAITING_PAYMENT' " +
            "AND a.reservationExpiresAt > :now")
    Optional<ServiceAcquisition> findActiveReservation(
            @Param("serviceId") Long serviceId,
            @Param("serviceType") String serviceType,
            @Param("now") LocalDateTime now);

    // ✅ Trouver les réservations qui nécessitent un rappel
    @Query("SELECT a FROM ServiceAcquisition a WHERE a.paymentStatus = 'AWAITING_PAYMENT' " +
            "AND a.reservationExpiresAt BETWEEN :start AND :end " +
            "AND a.reminderSent = false")
    List<ServiceAcquisition> findReservationsNeedingReminder(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT a FROM ServiceAcquisition a WHERE a.acquirerId = :userId " +
            "AND a.paymentStatus IN ('COMPLETED', 'AWAITING_PAYMENT')")
    List<ServiceAcquisition> findByAcquirerIdAndRoleAndActiveStatuses(
            @Param("userId") Long userId);

    // Trouver les acquisitions d'un user en attente ou réservées
    @Query("SELECT a FROM ServiceAcquisition a WHERE a.acquirerId = :acquirerId AND a.paymentStatus IN :statuses")
    List<ServiceAcquisition> findByAcquirerIdAndPaymentStatusIn(@Param("acquirerId") Long acquirerId, @Param("statuses") List<PaymentStatus> statuses);

    // Trouver une acquisition spécifique d'un user (avec vérification propriétaire)
    Optional<ServiceAcquisition> findByIdAndAcquirerId(Long id, Long acquirerId);

    @Query("SELECT a FROM ServiceAcquisition a WHERE a.acquirerId = :acquirerId AND a.acquirerRole = :role ORDER BY a.acquiredAt DESC")
    List<ServiceAcquisition> findAllByAcquirerIdAndRole(
            @Param("acquirerId") Long acquirerId,
            @Param("role") Role role
    );
}