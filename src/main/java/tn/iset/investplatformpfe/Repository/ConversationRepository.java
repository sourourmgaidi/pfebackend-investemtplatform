package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.iset.investplatformpfe.Entity.Conversation;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    // Chercher une conversation dans un sens précis
    Optional<Conversation> findBySenderEmailAndRecipientEmail(
            String senderEmail, String recipientEmail);

    // ✅ Toutes les conversations où l'utilisateur participe (sender OU recipient)
    @Query("SELECT c FROM Conversation c WHERE c.senderEmail = :email OR c.recipientEmail = :email ORDER BY c.lastMessageDate DESC")
    List<Conversation> findAllByParticipantEmail(@Param("email") String email);

    // Recherche dans les conversations
    @Query("SELECT c FROM Conversation c WHERE c.senderEmail = :email AND LOWER(c.lastMessage) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<Conversation> searchSenderConversations(@Param("email") String email,
                                                 @Param("search") String search);
    // ✅ Toutes les convs où je suis expéditeur
    List<Conversation> findBySenderEmailOrderByLastMessageDateDesc(String senderEmail);

    // ✅ Toutes les convs où je suis destinataire
    List<Conversation> findByRecipientEmailOrderByLastMessageDateDesc(String recipientEmail);


}