package tn.iset.investplatformpfe.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.iset.investplatformpfe.Entity.MultiRoleChat;
import tn.iset.investplatformpfe.Entity.Role;


import java.util.List;

public interface MultiRoleChatRepository extends JpaRepository<MultiRoleChat, Long> {

    // Récupérer la conversation entre deux utilisateurs
    @Query("SELECT c FROM MultiRoleChat c WHERE " +
            "(c.senderType = :senderType AND c.senderId = :senderId AND c.receiverType = :receiverType AND c.receiverId = :receiverId AND c.deletedBySender = false) " +
            "OR (c.senderType = :receiverType AND c.senderId = :receiverId AND c.receiverType = :senderType AND c.receiverId = :senderId AND c.deletedByReceiver = false) " +
            "ORDER BY c.sentAt DESC")
    Page<MultiRoleChat> findConversation(
            @Param("senderType") Role senderType,
            @Param("senderId") Long senderId,
            @Param("receiverType") Role receiverType,
            @Param("receiverId") Long receiverId,
            Pageable pageable);

    // Lister toutes les conversations d'un utilisateur
    @Query("SELECT DISTINCT " +
            "CASE WHEN c.senderType = :userRole AND c.senderId = :userId THEN c.receiverType ELSE c.senderType END as otherRole, " +
            "CASE WHEN c.senderType = :userRole AND c.senderId = :userId THEN c.receiverId ELSE c.senderId END as otherId, " +
            "MAX(c.sentAt) as lastMessageDate " +
            "FROM MultiRoleChat c " +
            "WHERE (c.senderType = :userRole AND c.senderId = :userId AND c.deletedBySender = false) " +
            "OR (c.receiverType = :userRole AND c.receiverId = :userId AND c.deletedByReceiver = false) " +
            "GROUP BY otherRole, otherId " +
            "ORDER BY lastMessageDate DESC")
    List<Object[]> findUserConversations(
            @Param("userRole") Role userRole,
            @Param("userId") Long userId);

    // Compter les messages non lus
    @Query("SELECT COUNT(c) FROM MultiRoleChat c WHERE " +
            "c.receiverType = :userRole AND c.receiverId = :userId " +
            "AND c.isRead = false AND c.deletedByReceiver = false")
    long countUnreadMessages(
            @Param("userRole") Role userRole,
            @Param("userId") Long userId);
    // ========================================
// RÉCUPÉRER LES MESSAGES VISIBLES D'UNE CONVERSATION
// ========================================
    @Query("SELECT c FROM MultiRoleChat c WHERE " +
            "((c.senderType = :user1Type AND c.senderId = :user1Id AND c.receiverType = :user2Type AND c.receiverId = :user2Id AND c.deletedBySender = false) " +
            "OR (c.senderType = :user2Type AND c.senderId = :user2Id AND c.receiverType = :user1Type AND c.receiverId = :user1Id AND c.deletedByReceiver = false)) " +
            "ORDER BY c.sentAt ASC")
    Page<MultiRoleChat> findVisibleMessages(
            @Param("user1Type") Role user1Type,
            @Param("user1Id") Long user1Id,
            @Param("user2Type") Role user2Type,
            @Param("user2Id") Long user2Id,
            Pageable pageable);

    // ========================================
// TROUVER LES MESSAGES SUPPRIMÉS PAR LES DEUX PARTICIPANTS
// ========================================
    @Query("SELECT c FROM MultiRoleChat c WHERE c.deletedBySender = true AND c.deletedByReceiver = true")
    List<MultiRoleChat> findMessagesDeletedByBoth();
}

