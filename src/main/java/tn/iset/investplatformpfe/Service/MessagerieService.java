package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Repository.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MessagerieService {

    private final FlouciSubscriptionService flouciSubscriptionService;
    private final MessageRepository messageRepo;
    private final ConversationRepository conversationRepo;
    private final MessageAttachmentRepository attachmentRepository;
    private final InvestorRepository investorRepo;
    private final EconomicPartnerRepository partenaireEcoRepo;
    private final LocalPartnerRepository localPartnerRepo;
    private final TouristRepository touristRepo;
    private final InternationalCompanyRepository internationalCompanyRepo;
    private final ContactPaymentRepository contactPaymentRepository;
    private final SubscriptionRepository subscriptionRepository;


    @Value("${file.upload-dir.messages:uploads/messages}")
    private String uploadDir;
    public MessagerieService(MessageRepository messageRepo,
                             FlouciSubscriptionService flouciSubscriptionService,
                             ContactPaymentRepository contactPaymentRepository,
                             ConversationRepository conversationRepo,
                             MessageAttachmentRepository attachmentRepository,
                             InvestorRepository investorRepo,
                             EconomicPartnerRepository partenaireEcoRepo,
                             LocalPartnerRepository localPartnerRepo,
                             TouristRepository touristRepo,
                             SubscriptionRepository subscriptionRepository,
                             InternationalCompanyRepository internationalCompanyRepo) {
        this.messageRepo = messageRepo;
        this.conversationRepo = conversationRepo;
        this.attachmentRepository = attachmentRepository;
        this.investorRepo = investorRepo;
        this.partenaireEcoRepo = partenaireEcoRepo;
        this.localPartnerRepo = localPartnerRepo;
        this.touristRepo = touristRepo;
        this.internationalCompanyRepo = internationalCompanyRepo;
        this.contactPaymentRepository = contactPaymentRepository;
        this.subscriptionRepository=subscriptionRepository;
        this.flouciSubscriptionService = flouciSubscriptionService;
    }

    // ========================================
    // ENVOYER UN MESSAGE SIMPLE (SANS PIÈCES JOINTES)
    // ========================================
    @Transactional
    public Message sendMessage(String senderEmail, String recipientEmail,
                               String content, String senderRole) {

        // ========================================
        // 🔥 BLOQUER L'ENVOI SI DESTINATAIRE EST LOCAL_PARTNER SANS ABONNEMENT ACTIF
        // ========================================
        if (localPartnerRepo.findByEmail(recipientEmail).isPresent()) {
            boolean hasActiveSub = subscriptionRepository.hasActiveSubscription(
                    senderEmail, LocalDateTime.now());

            if (!hasActiveSub) {
                throw new RuntimeException("Vous devez avoir un abonnement actif (40 TND/mois) pour contacter un Local Partner");
            }
        }
        // ========================================

        // 1. Trouver ou créer la conversation
        Conversation conversation = findOrCreateConversation(senderEmail, recipientEmail, senderRole);

        if (conversation.getId() == null) {
            conversation = conversationRepo.saveAndFlush(conversation);
        }

        // 2. Créer le message
        Message message = new Message();
        message.setContent(content);
        message.setSenderEmail(senderEmail);
        message.setRecipientEmail(recipientEmail);
        message.setSentDate(LocalDateTime.now());
        message.setRead(false);
        message.setConversation(conversation);

        // 3. Sauvegarder le message
        Message savedMessage = messageRepo.save(message);

        // 4. Mettre à jour la conversation
        updateConversationMetadata(conversation, content, senderEmail);

        return savedMessage;
    }
    // ========================================
    // ENVOYER UN MESSAGE AVEC PIÈCES JOINTES
    // ========================================
    @Transactional
    public Message sendMessageWithAttachments(String senderEmail, String recipientEmail,
                                              String content, String senderRole,
                                              MultipartFile[] attachments) throws IOException {

        // 1. Trouver ou créer la conversation
        Conversation conversation = findOrCreateConversation(senderEmail, recipientEmail, senderRole);

        // Forcer la sauvegarde si nouvellement créée
        if (conversation.getId() == null) {
            conversation = conversationRepo.saveAndFlush(conversation);
        }

        // 2. Créer le message
        Message message = new Message();
        message.setContent(content);
        message.setSenderEmail(senderEmail);
        message.setRecipientEmail(recipientEmail);
        message.setSentDate(LocalDateTime.now());
        message.setRead(false);
        message.setConversation(conversation);

        // 3. Sauvegarder le message d'abord
        Message savedMessage = messageRepo.save(message);
        System.out.println("✅ Message sauvegardé avec ID: " + savedMessage.getId());

        // 4. Traiter les attachments
        if (attachments != null && attachments.length > 0) {
            System.out.println("📦 Traitement de " + attachments.length + " fichier(s)");

            for (MultipartFile file : attachments) {
                if (file != null && !file.isEmpty()) {
                    // Sauvegarder le fichier et créer l'attachment
                    MessageAttachment attachment = saveAttachment(file);

                    // Lier l'attachment au message
                    savedMessage.addAttachment(attachment);

                    // Sauvegarder l'attachment
                    attachmentRepository.save(attachment);
                }
            }

            System.out.println("✅ " + attachments.length + " fichier(s) attaché(s)");

            // Sauvegarder le message avec les attachments
            savedMessage = messageRepo.save(savedMessage);
        }

        // 5. Mettre à jour la conversation avec un aperçu du message
        String messagePreview = buildMessagePreview(content, attachments);
        updateConversationMetadata(conversation, messagePreview, senderEmail);

        return savedMessage;
    }

    // ========================================
    // SAUVEGARDER UN FICHIER (MÉTHODE PRIVÉE)
    // ========================================
    private MessageAttachment saveAttachment(MultipartFile file) throws IOException {
        // Créer le dossier si nécessaire
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            System.out.println("📁 Dossier créé: " + uploadPath.toAbsolutePath());
        }

        // Générer un nom de fichier unique
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        Path filePath = uploadPath.resolve(uniqueFileName);

        // Sauvegarder le fichier
        Files.copy(file.getInputStream(), filePath);
        System.out.println("💾 Fichier sauvegardé: " + filePath.toAbsolutePath());

        // Créer et retourner l'attachment
        return MessageAttachment.builder()
                .fileName(originalFileName)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .filePath(filePath.toString())
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    // ========================================
    // CONSTRUIRE L'APERÇU DU MESSAGE POUR LA CONVERSATION
    // ========================================
    private String buildMessagePreview(String content, MultipartFile[] attachments) {
        StringBuilder preview = new StringBuilder();

        if (content != null && !content.trim().isEmpty()) {
            String truncatedContent = content.length() > 50 ?
                    content.substring(0, 50) + "..." : content;
            preview.append(truncatedContent);
        }

        if (attachments != null && attachments.length > 0) {
            if (preview.length() > 0) {
                preview.append(" ");
            }
            preview.append("📎 ");
            if (attachments.length == 1) {
                preview.append("1 pièce jointe");
            } else {
                preview.append(attachments.length).append(" pièces jointes");
            }
        }

        return preview.toString();
    }

    // ========================================
    // RÉCUPÉRER UN MESSAGE AVEC SES PIÈCES JOINTES
    // ========================================
    @Transactional(readOnly = true)
    public Message getMessageWithAttachments(Long messageId) {
        return messageRepo.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message non trouvé avec l'ID: " + messageId));
    }

    // ========================================
    // RÉCUPÉRER UNE PIÈCE JOINTE PAR SON ID
    // ========================================
    @Transactional(readOnly = true)
    public MessageAttachment getAttachment(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Pièce jointe non trouvée avec l'ID: " + attachmentId));
    }

    // ========================================
    // RÉCUPÉRER TOUTES LES PIÈCES JOINTES D'UN MESSAGE
    // ========================================
    @Transactional(readOnly = true)
    public List<MessageAttachment> getAttachmentsByMessageId(Long messageId) {
        return attachmentRepository.findByMessageId(messageId);
    }

    // ========================================
    // SUPPRIMER UNE PIÈCE JOINTE (AVEC LE FICHIER PHYSIQUE)
    // ========================================
    @Transactional
    public void deleteAttachment(Long attachmentId) {
        MessageAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Pièce jointe non trouvée"));

        try {
            // Supprimer le fichier physique
            Path filePath = Paths.get(attachment.getFilePath());
            Files.deleteIfExists(filePath);
            System.out.println("🗑️ Fichier supprimé: " + filePath);
        } catch (IOException e) {
            System.err.println("⚠️ Erreur lors de la suppression du fichier: " + e.getMessage());
        }

        // Supprimer l'entrée en base de données
        attachmentRepository.delete(attachment);
        System.out.println("✅ Pièce jointe supprimée de la base de données");
    }

    // ========================================
    // SUPPRIMER TOUTES LES PIÈCES JOINTES D'UN MESSAGE
    // ========================================
    @Transactional
    public void deleteAllAttachments(Long messageId) {
        List<MessageAttachment> attachments = attachmentRepository.findByMessageId(messageId);

        for (MessageAttachment attachment : attachments) {
            try {
                // Supprimer le fichier physique
                Path filePath = Paths.get(attachment.getFilePath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                System.err.println("⚠️ Erreur lors de la suppression du fichier: " + e.getMessage());
            }
        }

        // Supprimer toutes les entrées en base de données
        attachmentRepository.deleteAll(attachments);
        System.out.println("✅ " + attachments.size() + " pièce(s) jointe(s) supprimée(s)");
    }

    // ========================================
    // TROUVER OU CRÉER UNE CONVERSATION
    // ========================================
    private Conversation findOrCreateConversation(String senderEmail, String recipientEmail, String senderRole) {

        // Chercher si une conversation existe déjà (dans les deux sens)
        Conversation conversation = conversationRepo
                .findBySenderEmailAndRecipientEmail(senderEmail, recipientEmail)
                .orElseGet(() -> conversationRepo
                        .findBySenderEmailAndRecipientEmail(recipientEmail, senderEmail)
                        .orElse(null));

        // Si pas de conversation, en créer une nouvelle
        if (conversation == null) {
            String recipientRole = determineRecipientRole(recipientEmail);
            conversation = new Conversation(senderRole, senderEmail, recipientEmail, recipientRole);

            // Sauvegarder immédiatement
            conversation = conversationRepo.save(conversation);
        }

        return conversation;
    }

    // ========================================
    // METTRE À JOUR LES MÉTADONNÉES DE LA CONVERSATION
    // ========================================
    private void updateConversationMetadata(Conversation conversation, String content, String senderEmail) {
        conversation.setLastMessage(content);
        conversation.setLastMessageDate(LocalDateTime.now());

        if (conversation.getSenderEmail().equals(senderEmail)) {
            conversation.setSenderViewed(true);
            conversation.setPartnerViewed(false);
        } else {
            conversation.setSenderViewed(false);
            conversation.setPartnerViewed(true);
        }

        conversationRepo.save(conversation);
    }

    // ========================================
    // RÉCUPÉRER UNE CONVERSATION COMPLÈTE AVEC PIÈCES JOINTES
    // ========================================
    @Transactional
    public List<Message> getConversation(String myEmail, String otherEmail) {

        Conversation conversation = conversationRepo
                .findBySenderEmailAndRecipientEmail(myEmail, otherEmail)
                .orElseGet(() -> conversationRepo
                        .findBySenderEmailAndRecipientEmail(otherEmail, myEmail)
                        .orElseThrow(() -> new RuntimeException("Conversation not found")));

        // Récupérer tous les messages de la conversation
        List<Message> messages = messageRepo.findByConversationOrderBySentDateAsc(conversation);

        return messages;
    }

    // ========================================
// MARQUER LES MESSAGES D'UNE CONVERSATION COMME LUS
// (À appeler uniquement quand l'utilisateur ouvre la conversation)
// ========================================
    @Transactional
    public void markConversationAsRead(String myEmail, String otherEmail) {

        Conversation conversation = conversationRepo
                .findBySenderEmailAndRecipientEmail(myEmail, otherEmail)
                .orElseGet(() -> conversationRepo
                        .findBySenderEmailAndRecipientEmail(otherEmail, myEmail)
                        .orElse(null));

        if (conversation == null) {
            return; // Conversation n'existe pas
        }

        // Marquer les messages comme lus
        if (conversation.getSenderEmail().equals(myEmail)) {
            messageRepo.markMessagesAsRead(myEmail, conversation);
            conversation.setSenderViewed(true);
        } else {
            messageRepo.markMessagesAsRead(myEmail, conversation);
            conversation.setPartnerViewed(true);
        }

        conversationRepo.save(conversation);
    }

    // ========================================
    // RÉCUPÉRER TOUTES LES CONVERSATIONS D'UN UTILISATEUR
    // ========================================
    public List<Conversation> getAllConversations(String email) {
        List<Conversation> conversations = conversationRepo.findAllByParticipantEmail(email);

        // Ajouter le nombre de pièces jointes non lues ou d'autres métadonnées si nécessaire
        for (Conversation conv : conversations) {
            // Vous pouvez ajouter ici des métadonnées supplémentaires
            long unreadCount = messageRepo.countUnreadByRecipient(email);
            // conv.setUnreadCount(unreadCount); // À ajouter si nécessaire dans l'entité
        }

        return conversations;
    }

    public List<Conversation> getSenderConversations(String email) {
        return conversationRepo.findAllByParticipantEmail(email);
    }

    public List<Conversation> getPartnerConversations(String email) {
        return conversationRepo.findAllByParticipantEmail(email);
    }

    // ========================================
    // MESSAGES NON LUS
    // ========================================
    public long countUnreadMessages(String email) {
        return messageRepo.countUnreadByRecipient(email);
    }

    public List<Message> getUnreadMessages(String email) {
        return messageRepo.findByRecipientEmailAndReadFalse(email);
    }

    // ========================================
    // VÉRIFIER SI UNE CONVERSATION EXISTE
    // ========================================
    public boolean conversationExists(String senderEmail, String recipientEmail) {
        return conversationRepo.findBySenderEmailAndRecipientEmail(senderEmail, recipientEmail).isPresent()
                || conversationRepo.findBySenderEmailAndRecipientEmail(recipientEmail, senderEmail).isPresent();
    }

    // ========================================
    // RECHERCHE
    // ========================================
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

    // ========================================
    // UTILITAIRE : Déterminer le rôle du destinataire
    // ========================================
    private String determineRecipientRole(String email) {
        if (localPartnerRepo.findByEmail(email).isPresent()) return "LOCAL_PARTNER";
        if (investorRepo.findByEmail(email).isPresent()) return "INVESTOR";
        if (partenaireEcoRepo.findByEmail(email).isPresent()) return "PARTNER";
        if (touristRepo.findByEmail(email).isPresent()) return "TOURIST";
        if (internationalCompanyRepo.findByEmail(email).isPresent()) return "INTERNATIONAL_COMPANY";
        return "UNKNOWN";
    }

    // ========================================
    // MÉTHODES UTILITAIRES SUPPLÉMENTAIRES
    // ========================================

    /**
     * Vérifier si un message a des pièces jointes
     */
    public boolean hasAttachments(Long messageId) {
        Message message = messageRepo.findById(messageId).orElse(null);
        return message != null && message.getAttachments() != null && !message.getAttachments().isEmpty();
    }

    /**
     * Compter le nombre total de pièces jointes pour un utilisateur
     */
    public long countTotalAttachmentsForUser(String email) {
        // Cette méthode nécessiterait une requête personnalisée
        // Pour l'instant, retourne 0
        return 0;
    }

    /**
     * Nettoyer les fichiers orphelins (pièces jointes sans message associé)
     * À appeler périodiquement (ex: tâche planifiée)
     */
    @Transactional
    public void cleanupOrphanedAttachments() {
        List<MessageAttachment> allAttachments = attachmentRepository.findAll();
        int deletedCount = 0;

        for (MessageAttachment attachment : allAttachments) {
            if (attachment.getMessage() == null) {
                try {
                    // Supprimer le fichier physique
                    Path filePath = Paths.get(attachment.getFilePath());
                    Files.deleteIfExists(filePath);

                    // Supprimer l'entrée en base
                    attachmentRepository.delete(attachment);
                    deletedCount++;

                    System.out.println("🗑️ Attachment orphelin supprimé: " + attachment.getFileName());
                } catch (IOException e) {
                    System.err.println("⚠️ Erreur lors de la suppression du fichier orphelin: " + e.getMessage());
                }
            }
        }

        if (deletedCount > 0) {
            System.out.println("✅ Nettoyage terminé: " + deletedCount + " fichier(s) orphelin(s) supprimé(s)");
        }
    }

    // ========================================
// RÉCUPÉRER LE RÔLE D'UN UTILISATEUR PAR EMAIL
// ========================================
    public String getUserRole(String email) {
        if (localPartnerRepo.findByEmail(email).isPresent()) return "LOCAL_PARTNER";
        if (investorRepo.findByEmail(email).isPresent()) return "INVESTOR";
        if (partenaireEcoRepo.findByEmail(email).isPresent()) return "PARTNER";
        if (touristRepo.findByEmail(email).isPresent()) return "TOURIST";
        if (internationalCompanyRepo.findByEmail(email).isPresent()) return "INTERNATIONAL_COMPANY";
        return "UNKNOWN";
    }

    // ========================================
// MÉTHODES POUR PAIEMENT LOCAL PARTNER
// ========================================



// ========================================
// MÉTHODES POUR ABONNEMENT MENSUEL
// ========================================

    /**
     * Vérifier si un utilisateur a un abonnement actif
     */
    public Map<String, Object> checkSubscription(String userEmail) {
        Optional<Subscription> active = subscriptionRepository
                .findActiveSubscription(userEmail, LocalDateTime.now());

        if (active.isPresent()) {
            Subscription sub = active.get();
            long daysRemaining = java.time.temporal.ChronoUnit.DAYS
                    .between(LocalDateTime.now(), sub.getExpiresAt());
            return Map.of(
                    "hasActiveSubscription", true,
                    "expiresAt", sub.getExpiresAt().toString(),
                    "daysRemaining", daysRemaining
            );
        }

        return Map.of(
                "hasActiveSubscription", false,
                "requiresPayment", true,
                "amount", 40,
                "currency", "TND"
        );
    }

    /**
     * Créer une session de paiement d'abonnement
     */
    @Transactional
    public Subscription createSubscriptionSession(String subscriberEmail) {
        String paymentId = "SUB_" + System.currentTimeMillis() + "_" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Subscription sub = Subscription.builder()
                .paymentId(paymentId)
                .subscriberEmail(subscriberEmail)
                .amount(40.0)
                .currency("TND")
                .status(PaymentStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        return subscriptionRepository.save(sub);
    }

    /**
     * Confirmer le paiement et activer l'abonnement pour 1 mois
     */
    @Transactional
    public Map<String, Object> confirmSubscriptionPayment(String paymentId,
                                                          String transactionId,
                                                          String flouciPaymentId) {
        Subscription sub = subscriptionRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Abonnement non trouvé"));

        if (sub.getStatus() != PaymentStatus.PENDING) {
            throw new RuntimeException("Paiement déjà traité");
        }

        LocalDateTime now = LocalDateTime.now();
        sub.setStatus(PaymentStatus.COMPLETED);
        sub.setPaidAt(now);
        sub.setExpiresAt(now.plusMonths(1));   // ← 1 mois d'accès
        sub.setTransactionId(transactionId);
        sub.setFlouciPaymentId(flouciPaymentId);
        subscriptionRepository.save(sub);

        return Map.of(
                "success", true,
                "subscriberEmail", sub.getSubscriberEmail(),
                "expiresAt", sub.getExpiresAt().toString(),
                "daysRemaining", 30
        );
    }

    /**
     * Marquer un abonnement comme échoué
     */
    public void markSubscriptionFailed(String paymentId) {
        subscriptionRepository.findByPaymentId(paymentId).ifPresent(sub -> {
            if (sub.getStatus() == PaymentStatus.PENDING) {
                sub.setStatus(PaymentStatus.FAILED);
                subscriptionRepository.save(sub);
            }
        });
    }
}