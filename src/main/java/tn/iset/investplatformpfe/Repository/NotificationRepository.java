package tn.iset.investplatformpfe.Repository;

import tn.iset.investplatformpfe.Entity.Notification;
import tn.iset.investplatformpfe.Entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Notifications individuelles
    List<Notification> findByRecipientRoleAndRecipientIdOrderByCreatedAtDesc(Role role, Long recipientId);

    // Notifications broadcast
    List<Notification> findByRecipientRoleAndRecipientIdIsNullOrderByCreatedAtDesc(Role role);

    // Notifications non lues
    List<Notification> findByRecipientRoleAndRecipientIdAndReadFalseOrderByCreatedAtDesc(Role role, Long recipientId);

    List<Notification> findByRecipientRoleAndRecipientIdIsNullAndReadFalseOrderByCreatedAtDesc(Role role);

    List<Notification> findByServiceIdOrderByCreatedAtDesc(Long serviceId);
    // Pour la méthode 2
    List<Notification> findByRecipientRoleAndRecipientIdAndReadTrue(Role role, Long recipientId);
    List<Notification> findByRecipientRoleAndRecipientIdIsNullAndReadTrue(Role role);
}