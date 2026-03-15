package tn.iset.investplatformpfe.Repository;
import tn.iset.investplatformpfe.Entity.Notification;
import tn.iset.investplatformpfe.Entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Notifications pour un utilisateur spécifique
    List<Notification> findByRecipientRoleAndRecipientIdOrderByCreatedAtDesc(Role role, Long recipientId);

    // Notifications pour tous les utilisateurs d'un rôle (broadcast)
    List<Notification> findByRecipientRoleAndRecipientIdIsNullOrderByCreatedAtDesc(Role role);

    // Notifications non lues pour un utilisateur
    List<Notification> findByRecipientRoleAndRecipientIdAndIsReadFalseOrderByCreatedAtDesc(Role role, Long recipientId);

    // Notifications pour un service spécifique
    List<Notification> findByServiceIdOrderByCreatedAtDesc(Long serviceId);
}
