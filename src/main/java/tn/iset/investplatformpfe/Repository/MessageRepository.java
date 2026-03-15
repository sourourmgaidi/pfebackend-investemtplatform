package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.iset.investplatformpfe.Entity.Conversation;
import tn.iset.investplatformpfe.Entity.Message;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    // ✅ FIX PRINCIPAL : retourne TOUS les messages triés du plus ancien au plus récent
    List<Message> findByConversationOrderBySentDateAsc(Conversation conversation);

    // Messages non lus d'un destinataire
    List<Message> findByRecipientEmailAndReadFalse(String recipientEmail);

    // Compter les messages non lus
    @Query("SELECT COUNT(m) FROM Message m WHERE m.recipientEmail = :email AND m.read = false")
    long countUnreadByRecipient(@Param("email") String email);

    // Marquer les messages comme lus (ceux reçus par myEmail dans cette conversation)
    @Modifying
    @Query("UPDATE Message m SET m.read = true WHERE m.recipientEmail = :recipientEmail AND m.conversation = :conversation")
    void markMessagesAsRead(@Param("recipientEmail") String recipientEmail,
                            @Param("conversation") Conversation conversation);
}