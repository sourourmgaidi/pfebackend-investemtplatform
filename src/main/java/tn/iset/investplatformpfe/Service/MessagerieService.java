package tn.iset.investplatformpfe.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Repository.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MessagerieService {

    private final MessageRepository messageRepo;
    private final ConversationRepository conversationRepo;
    private final InvestorRepository investorRepo;
    private final EconomicPartnerRepository partenaireEcoRepo;
    private final LocalPartnerRepository localPartnerRepo;
    private final TouristRepository touristRepo;
    private final InternationalCompanyRepository internationalCompanyRepo;

    public MessagerieService(MessageRepository messageRepo,
                             ConversationRepository conversationRepo,
                             InvestorRepository investorRepo,
                             EconomicPartnerRepository partenaireEcoRepo,
                             LocalPartnerRepository localPartnerRepo,
                             TouristRepository touristRepo,
                             InternationalCompanyRepository internationalCompanyRepo) {
        this.messageRepo = messageRepo;
        this.conversationRepo = conversationRepo;
        this.investorRepo = investorRepo;
        this.partenaireEcoRepo = partenaireEcoRepo;
        this.localPartnerRepo = localPartnerRepo;
        this.touristRepo = touristRepo;
        this.internationalCompanyRepo = internationalCompanyRepo;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RECHERCHE
    // ─────────────────────────────────────────────────────────────────────────

    public List<Map<String, Object>> searchLocalPartners(String search) {
        return localPartnerRepo.searchPartners(search).stream()
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", p.getId());
                    map.put("fullName", p.getFirstName() + " " + p.getLastName());
                    map.put("email", p.getEmail());
                    map.put("domain", p.getActivityDomain());
                    return map;
                })
                .limit(10)
                .collect(Collectors.toList());
    }

    public List<Conversation> searchSenderConversations(String email, String search) {
        return conversationRepo.searchSenderConversations(email, search);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENVOYER UN MESSAGE (universel - fonctionne pour tous les rôles)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public Message sendMessage(String senderEmail, String recipientEmail,
                               String content, String senderRole) {

        // Chercher la conversation existante dans les deux sens
        Conversation conversation = conversationRepo
                .findBySenderEmailAndRecipientEmail(senderEmail, recipientEmail)
                .orElseGet(() -> conversationRepo
                        .findBySenderEmailAndRecipientEmail(recipientEmail, senderEmail)
                        .orElseGet(() -> {
                            // Créer une nouvelle conversation
                            String recipientRole = determineRecipientRole(recipientEmail);
                            Conversation nouvelle = new Conversation(
                                    senderRole, senderEmail, recipientEmail, recipientRole
                            );
                            return conversationRepo.save(nouvelle);
                        }));

        // Créer et sauvegarder le message
        Message message = new Message(content, senderEmail, recipientEmail, conversation);
        message = messageRepo.save(message);

        // Mettre à jour la conversation
        conversation.setLastMessage(content);
        conversation.setLastMessageDate(LocalDateTime.now());

        // Marquer comme lu pour l'expéditeur, non lu pour le destinataire
        if (conversation.getSenderEmail().equals(senderEmail)) {
            conversation.setSenderViewed(true);
            conversation.setPartnerViewed(false);
        } else {
            conversation.setSenderViewed(false);
            conversation.setPartnerViewed(true);
        }

        conversationRepo.save(conversation);
        return message;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RÉCUPÉRER UNE CONVERSATION COMPLÈTE
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public List<Message> getConversation(String myEmail, String otherEmail) {

        // Chercher la conversation dans les deux sens
        Conversation conversation = conversationRepo
                .findBySenderEmailAndRecipientEmail(myEmail, otherEmail)
                .orElseGet(() -> conversationRepo
                        .findBySenderEmailAndRecipientEmail(otherEmail, myEmail)
                        .orElseThrow(() -> new RuntimeException("Conversation not found")));

        // Marquer comme lu selon qui consulte
        if (conversation.getSenderEmail().equals(myEmail)) {
            messageRepo.markMessagesAsRead(myEmail, conversation);
            conversation.setSenderViewed(true);
        } else {
            messageRepo.markMessagesAsRead(myEmail, conversation);
            conversation.setPartnerViewed(true);
        }

        conversationRepo.save(conversation);
        return messageRepo.findByConversationOrderBySentDateAsc(conversation);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RÉCUPÉRER TOUTES LES CONVERSATIONS D'UN UTILISATEUR
    // ─────────────────────────────────────────────────────────────────────────

    public List<Conversation> getAllConversations(String email) {
        // Retourne toutes les convs où l'utilisateur est sender OU recipient
        return conversationRepo.findAllByParticipantEmail(email);
    }

    // Compatibilité avec l'ancien code frontend
    public List<Conversation> getSenderConversations(String email) {
        return conversationRepo.findAllByParticipantEmail(email);
    }

    // Compatibilité pour le partenaire local
    public List<Conversation> getPartnerConversations(String email) {
        return conversationRepo.findAllByParticipantEmail(email);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MESSAGES NON LUS
    // ─────────────────────────────────────────────────────────────────────────

    public long countUnreadMessages(String email) {
        return messageRepo.countUnreadByRecipient(email);
    }

    public List<Message> getUnreadMessages(String email) {
        return messageRepo.findByRecipientEmailAndReadFalse(email);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VÉRIFIER SI UNE CONVERSATION EXISTE
    // ─────────────────────────────────────────────────────────────────────────

    public boolean conversationExists(String senderEmail, String recipientEmail) {
        return conversationRepo.findBySenderEmailAndRecipientEmail(senderEmail, recipientEmail).isPresent()
                || conversationRepo.findBySenderEmailAndRecipientEmail(recipientEmail, senderEmail).isPresent();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITAIRE : Déterminer le rôle du destinataire
    // ─────────────────────────────────────────────────────────────────────────


    // InternationalCompanyRepository internationalCompanyRepo

    private String determineRecipientRole(String email) {
        if (localPartnerRepo.findByEmail(email).isPresent()) return "LOCAL_PARTNER";
        if (investorRepo.findByEmail(email).isPresent()) return "INVESTOR";
        if (partenaireEcoRepo.findByEmail(email).isPresent()) return "PARTNER";
        if (touristRepo.findByEmail(email).isPresent()) return "TOURIST";
        if (internationalCompanyRepo.findByEmail(email).isPresent()) return "INTERNATIONAL_COMPANY"; // ✅ AJOUT
        return "UNKNOWN";
    }
}