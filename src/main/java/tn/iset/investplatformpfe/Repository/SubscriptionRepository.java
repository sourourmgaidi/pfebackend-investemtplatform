package tn.iset.investplatformpfe.Repository;

import tn.iset.investplatformpfe.Entity.Subscription;
import tn.iset.investplatformpfe.Entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByPaymentId(String paymentId);

    // Vérifier si l'utilisateur a un abonnement actif (non expiré)
    @Query("""
        SELECT COUNT(s) > 0 FROM Subscription s
        WHERE s.subscriberEmail = :email
          AND s.status = 'COMPLETED'
          AND s.expiresAt > :now
    """)
    boolean hasActiveSubscription(String email, LocalDateTime now);

    // Récupérer l'abonnement actif
    @Query("""
        SELECT s FROM Subscription s
        WHERE s.subscriberEmail = :email
          AND s.status = 'COMPLETED'
          AND s.expiresAt > :now
        ORDER BY s.expiresAt DESC
    """)
    Optional<Subscription> findActiveSubscription(String email, LocalDateTime now);
}
