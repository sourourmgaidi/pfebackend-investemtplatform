package tn.iset.investplatformpfe.Repository;

import tn.iset.investplatformpfe.Entity.ContactPayment;
import tn.iset.investplatformpfe.Entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ContactPaymentRepository extends JpaRepository<ContactPayment, Long> {
    Optional<ContactPayment> findByPaymentId(String paymentId);
    Optional<ContactPayment> findByFlouciPaymentId(String flouciPaymentId);
    boolean existsByPayerEmailAndLocalPartnerEmailAndStatus(
            String payerEmail, String localPartnerEmail, PaymentStatus status);
    boolean existsByPayerEmailAndLocalPartnerEmailAndServiceIdAndStatus(
            String payerEmail, String localPartnerEmail, String serviceId, PaymentStatus status);
}