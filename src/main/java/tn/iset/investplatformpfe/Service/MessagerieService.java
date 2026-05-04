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

    // ✅ CHANGEMENT : FlouciSubscriptionService → KonnectSubscriptionService
    private final KonnectSubscriptionService konnectSubscriptionService;
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

    // ✅ CHANGEMENT : Constructeur mis à jour
    public MessagerieService(MessageRepository messageRepo,
                             KonnectSubscriptionService konnectSubscriptionService,
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
        this.subscriptionRepository = subscriptionRepository;
        this.konnectSubscriptionService = konnectSubscriptionService;
    }

    // ========================================
    // ENVOYER UN MESSAGE SIMPLE (SANS PIÈCES JOINTES)
    // ========================================
    @Transactional
    public Message sendMessage(String senderEmail, String recipientEmail,
                               String content, String senderRole) {
        if (localPartnerRepo.findByEmail(recipientEmail).isPresent()) {
            boolean hasActiveSub = subscriptionRepository.hasActiveSubscription(
                    senderEmail, LocalDateTime.now());
            if (!hasActiveSub) {
                throw new RuntimeException("Vous devez avoir un abonnement actif (40 TND/mois) pour contacter un Local Partner");
            }
        }

        Conversation conversation = findOrCreateConversation(senderEmail, recipientEmail, senderRole);
        if (conversation.getId() == null) {
            conversation = conversationRepo.saveAndFlush(conversation);
        }

        Message message = new Message();
        message.setContent(content);
        message.setSenderEmail(senderEmail);
        message.setRecipientEmail(recipientEmail);
        message.setSentDate(LocalDateTime.now());
        message.setRead(false);
        message.setConversation(conversation);

        Message savedMessage = messageRepo.save(message);
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
        Conversation conversation = findOrCreateConversation(senderEmail, recipientEmail, senderRole);
        if (conversation.getId() == null) {
            conversation = conversationRepo.saveAndFlush(conversation);
        }

        Message message = new Message();
        message.setContent(content);
        message.setSenderEmail(senderEmail);
        message.setRecipientEmail(recipientEmail);
        message.setSentDate(LocalDateTime.now());
        message.setRead(false);
        message.setConversation(conversation);

        Message savedMessage = messageRepo.save(message);

        if (attachments != null && attachments.length > 0) {
            for (MultipartFile file : attachments) {
                if (file != null && !file.isEmpty()) {
                    MessageAttachment attachment = saveAttachment(file);
                    savedMessage.addAttachment(attachment);
                    attachmentRepository.save(attachment);
                }
            }
            savedMessage = messageRepo.save(savedMessage);
        }

        String messagePreview = buildMessagePreview(content, attachments);
        updateConversationMetadata(conversation, messagePreview, senderEmail);
        return savedMessage;
    }

    // ========================================
    // SAUVEGARDER UN FICHIER
    // ========================================
    private MessageAttachment saveAttachment(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        Path filePath = uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath);

        return MessageAttachment.builder()
                .fileName(originalFileName)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .filePath(filePath.toString())
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    private String buildMessagePreview(String content, MultipartFile[] attachments) {
        StringBuilder preview = new StringBuilder();
        if (content != null && !content.trim().isEmpty()) {
            preview.append(content.length() > 50 ? content.substring(0, 50) + "..." : content);
        }
        if (attachments != null && attachments.length > 0) {
            if (preview.length() > 0) preview.append(" ");
            preview.append("📎 ").append(attachments.length == 1 ? "1 pièce jointe" : attachments.length + " pièces jointes");
        }
        return preview.toString();
    }

    @Transactional(readOnly = true)
    public Message getMessageWithAttachments(Long messageId) {
        return messageRepo.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message non trouvé avec l'ID: " + messageId));
    }

    @Transactional(readOnly = true)
    public MessageAttachment getAttachment(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Pièce jointe non trouvée avec l'ID: " + attachmentId));
    }

    @Transactional(readOnly = true)
    public List<MessageAttachment> getAttachmentsByMessageId(Long messageId) {
        return attachmentRepository.findByMessageId(messageId);
    }

    @Transactional
    public void deleteAttachment(Long attachmentId) {
        MessageAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Pièce jointe non trouvée"));
        try {
            Files.deleteIfExists(Paths.get(attachment.getFilePath()));
        } catch (IOException e) {
            System.err.println("⚠️ Erreur suppression fichier: " + e.getMessage());
        }
        attachmentRepository.delete(attachment);
    }

    @Transactional
    public void deleteAllAttachments(Long messageId) {
        List<MessageAttachment> attachments = attachmentRepository.findByMessageId(messageId);
        for (MessageAttachment attachment : attachments) {
            try {
                Files.deleteIfExists(Paths.get(attachment.getFilePath()));
            } catch (IOException e) {
                System.err.println("⚠️ Erreur suppression fichier: " + e.getMessage());
            }
        }
        attachmentRepository.deleteAll(attachments);
    }

    private Conversation findOrCreateConversation(String senderEmail, String recipientEmail, String senderRole) {
        Conversation conversation = conversationRepo
                .findBySenderEmailAndRecipientEmail(senderEmail, recipientEmail)
                .orElseGet(() -> conversationRepo
                        .findBySenderEmailAndRecipientEmail(recipientEmail, senderEmail)
                        .orElse(null));

        if (conversation == null) {
            String recipientRole = determineRecipientRole(recipientEmail);
            conversation = new Conversation(senderRole, senderEmail, recipientEmail, recipientRole);
            conversation = conversationRepo.save(conversation);
        }
        return conversation;
    }

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

    @Transactional
    public List<Message> getConversation(String myEmail, String otherEmail) {
        Conversation conversation = conversationRepo
                .findBySenderEmailAndRecipientEmail(myEmail, otherEmail)
                .orElseGet(() -> conversationRepo
                        .findBySenderEmailAndRecipientEmail(otherEmail, myEmail)
                        .orElseThrow(() -> new RuntimeException("Conversation not found")));
        return messageRepo.findByConversationOrderBySentDateAsc(conversation);
    }

    @Transactional
    public void markConversationAsRead(String myEmail, String otherEmail) {
        Conversation conversation = conversationRepo
                .findBySenderEmailAndRecipientEmail(myEmail, otherEmail)
                .orElseGet(() -> conversationRepo
                        .findBySenderEmailAndRecipientEmail(otherEmail, myEmail)
                        .orElse(null));
        if (conversation == null) return;

        messageRepo.markMessagesAsRead(myEmail, conversation);
        if (conversation.getSenderEmail().equals(myEmail)) {
            conversation.setSenderViewed(true);
        } else {
            conversation.setPartnerViewed(true);
        }
        conversationRepo.save(conversation);
    }

    public List<Conversation> getAllConversations(String email) {
        return conversationRepo.findAllByParticipantEmail(email);
    }

    public List<Conversation> getSenderConversations(String email) {
        return conversationRepo.findAllByParticipantEmail(email);
    }

    public List<Conversation> getPartnerConversations(String email) {
        return conversationRepo.findAllByParticipantEmail(email);
    }

    public long countUnreadMessages(String email) {
        return messageRepo.countUnreadByRecipient(email);
    }

    public List<Message> getUnreadMessages(String email) {
        return messageRepo.findByRecipientEmailAndReadFalse(email);
    }

    public boolean conversationExists(String senderEmail, String recipientEmail) {
        return conversationRepo.findBySenderEmailAndRecipientEmail(senderEmail, recipientEmail).isPresent()
                || conversationRepo.findBySenderEmailAndRecipientEmail(recipientEmail, senderEmail).isPresent();
    }

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

    private String determineRecipientRole(String email) {
        if (localPartnerRepo.findByEmail(email).isPresent()) return "LOCAL_PARTNER";
        if (investorRepo.findByEmail(email).isPresent()) return "INVESTOR";
        if (partenaireEcoRepo.findByEmail(email).isPresent()) return "PARTNER";
        if (touristRepo.findByEmail(email).isPresent()) return "TOURIST";
        if (internationalCompanyRepo.findByEmail(email).isPresent()) return "INTERNATIONAL_COMPANY";
        return "UNKNOWN";
    }

    public boolean hasAttachments(Long messageId) {
        Message message = messageRepo.findById(messageId).orElse(null);
        return message != null && message.getAttachments() != null && !message.getAttachments().isEmpty();
    }

    public String getUserRole(String email) {
        if (localPartnerRepo.findByEmail(email).isPresent()) return "LOCAL_PARTNER";
        if (investorRepo.findByEmail(email).isPresent()) return "INVESTOR";
        if (partenaireEcoRepo.findByEmail(email).isPresent()) return "PARTNER";
        if (touristRepo.findByEmail(email).isPresent()) return "TOURIST";
        if (internationalCompanyRepo.findByEmail(email).isPresent()) return "INTERNATIONAL_COMPANY";
        return "UNKNOWN";
    }

    // ========================================
    // MÉTHODES ABONNEMENT - KONNECT
    // ========================================

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

    @Transactional
    public Map<String, Object> initiateSubscriptionPayment(String subscriberEmail) {
        // 1. Créer l'abonnement en base avec statut PENDING
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
        subscriptionRepository.save(sub);

        // ✅ 2. Appeler Konnect pour obtenir le lien de paiement
        Map<String, Object> konnectResponse = konnectSubscriptionService
                .initiateSubscriptionPayment(40.0, paymentId);

        // 3. Retourner payUrl + paymentId au frontend Angular
        Map<String, Object> result = new HashMap<>(konnectResponse);
        result.put("paymentId", paymentId); // notre ID interne
        return result;
    }

    @Transactional
    public Map<String, Object> confirmSubscriptionPayment(String paymentId,
                                                          String transactionId,
                                                          String konnectPaymentRef) {
        Subscription sub = subscriptionRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new RuntimeException("Abonnement non trouvé"));

        if (sub.getStatus() == PaymentStatus.COMPLETED) {
            // ✅ Déjà confirmé → retourner les infos sans erreur
            return Map.of(
                    "success", true,
                    "subscriberEmail", sub.getSubscriberEmail(),
                    "expiresAt", sub.getExpiresAt().toString(),
                    "daysRemaining", 30,
                    "message", "Abonnement déjà actif"
            );
        }

        if (sub.getStatus() == PaymentStatus.FAILED) {
            throw new RuntimeException("Ce paiement a échoué");
        }

        // ✅ PLUS DE VÉRIFICATION KONNECT ICI
        // La vérification est déjà faite dans le Controller avant d'appeler cette méthode
        LocalDateTime now = LocalDateTime.now();
        sub.setStatus(PaymentStatus.COMPLETED);
        sub.setPaidAt(now);
        sub.setExpiresAt(now.plusMonths(1));
        sub.setTransactionId(transactionId);
        sub.setFlouciPaymentId(konnectPaymentRef);
        subscriptionRepository.save(sub);

        return Map.of(
                "success", true,
                "subscriberEmail", sub.getSubscriberEmail(),
                "expiresAt", sub.getExpiresAt().toString(),
                "daysRemaining", 30,
                "message", "Abonnement activé avec succès"
        );
    }

    public void markSubscriptionFailed(String paymentId) {
        subscriptionRepository.findByPaymentId(paymentId).ifPresent(sub -> {
            if (sub.getStatus() == PaymentStatus.PENDING) {
                sub.setStatus(PaymentStatus.FAILED);
                subscriptionRepository.save(sub);
            }
        });
    }
}