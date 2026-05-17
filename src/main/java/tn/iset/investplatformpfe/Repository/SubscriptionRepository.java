package tn.iset.investplatformpfe.Repository;

import tn.iset.investplatformpfe.Entity.Subscription;
import tn.iset.investplatformpfe.Entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByPaymentId(String paymentId);

    @Query("""
        SELECT COUNT(s) > 0 FROM Subscription s
        WHERE s.subscriberEmail = :email
          AND s.status = 'COMPLETED'
          AND s.expiresAt > :now
    """)
    boolean hasActiveSubscription(String email, LocalDateTime now);

    @Query("""
        SELECT s FROM Subscription s
        WHERE s.subscriberEmail = :email
          AND s.status = 'COMPLETED'
          AND s.expiresAt > :now
        ORDER BY s.expiresAt DESC
    """)
    Optional<Subscription> findActiveSubscription(String email, LocalDateTime now);

    @Query("""
        SELECT s FROM Subscription s
        WHERE s.status = 'COMPLETED'
          AND s.expiresAt BETWEEN :now AND :in2Days
          AND (s.expirationNotified IS NULL OR s.expirationNotified = false)
    """)
    List<Subscription> findExpiringSubscriptions(@Param("now") LocalDateTime now,
                                                 @Param("in2Days") LocalDateTime in2Days);

    // ✅ CORRIGÉ : filtre sur expiredNotified = false pour ne notifier qu'une seule fois
    @Query("""
        SELECT s FROM Subscription s
        WHERE s.status = 'COMPLETED'
          AND s.expiresAt < :now
          AND (s.expiredNotified IS NULL OR s.expiredNotified = false)
    """)
    List<Subscription> findJustExpiredSubscriptions(@Param("now") LocalDateTime now);
}