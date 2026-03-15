package tn.iset.investplatformpfe.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tn.iset.investplatformpfe.Dto.MultiRoleAttachmentDTO;
import tn.iset.investplatformpfe.Dto.MultiRoleMessageDTO;
import tn.iset.investplatformpfe.Entity.MultiRoleChat;
import tn.iset.investplatformpfe.Entity.MultiRoleChatAttachment;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Repository.MultiRoleChatAttachmentRepository;
import tn.iset.investplatformpfe.Repository.MultiRoleChatRepository;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MultiRoleChatService {

    private final MultiRoleChatRepository chatRepository;
    private final MultiRoleChatAttachmentRepository attachmentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${file.upload-dir:uploadschat}")
    private String uploadDir;

    public MultiRoleChatService(MultiRoleChatRepository chatRepository,
                                MultiRoleChatAttachmentRepository attachmentRepository) {
        this.chatRepository = chatRepository;
        this.attachmentRepository = attachmentRepository;
    }

    // ========================================
    // ENVOYER MESSAGE SIMPLE
    // ========================================
    @Transactional
    public MultiRoleChat sendMessage(Role senderType, Long senderId,
                                     Role receiverType, Long receiverId,
                                     String content) {
        MultiRoleChat chat = MultiRoleChat.builder()
                .senderType(senderType)
                .senderId(senderId)
                .receiverType(receiverType)
                .receiverId(receiverId)
                .content(content)
                .isRead(false)
                .deletedBySender(false)
                .deletedByReceiver(false)
                .attachments(new ArrayList<>())
                .build();
        return chatRepository.save(chat);
    }

    // ========================================
    // ENVOYER MESSAGE AVEC FICHIERS
    // ========================================
    @Transactional
    public MultiRoleChat sendMessageWithAttachments(Role senderType, Long senderId,
                                                    Role receiverType, Long receiverId,
                                                    String content,
                                                    MultipartFile[] attachments) throws IOException {

        // 1. Créer et sauvegarder le chat
        MultiRoleChat chat = MultiRoleChat.builder()
                .senderType(senderType)
                .senderId(senderId)
                .receiverType(receiverType)
                .receiverId(receiverId)
                .content(content)
                .isRead(false)
                .deletedBySender(false)
                .deletedByReceiver(false)
                .attachments(new ArrayList<>())
                .build();

        MultiRoleChat savedChat = chatRepository.save(chat);
        System.out.println("✅ Message MultiRole sauvegardé avec ID: " + savedChat.getId());

        // 2. Traiter les attachments
        if (attachments != null && attachments.length > 0) {
            System.out.println("📦 Traitement de " + attachments.length + " fichier(s)");

            for (MultipartFile file : attachments) {
                if (!file.isEmpty()) {
                    // Sauvegarder le fichier et créer l'attachment
                    MultiRoleChatAttachment attachment = saveAttachment(file);

                    // 🔴 IMPORTANT: Lier l'attachment au chat via la méthode dédiée
                    savedChat.addAttachment(attachment);

                    // Sauvegarder l'attachment
                    attachmentRepository.save(attachment);
                }
            }

            System.out.println("✅ " + attachments.length + " fichier(s) attaché(s)");

            // 3. Sauvegarder le chat avec les attachments
            savedChat = chatRepository.save(savedChat);

            // 4. Vider le cache et recharger
            entityManager.flush();
            entityManager.clear();

            // 5. Recharger le chat avec ses attachments
            savedChat = chatRepository.findById(savedChat.getId())
                    .orElseThrow(() -> new RuntimeException("Chat non trouvé après sauvegarde"));

            System.out.println("🔄 Chat rechargé avec " +
                    (savedChat.getAttachments() != null ? savedChat.getAttachments().size() : 0) +
                    " attachment(s)");
        }

        return savedChat;
    }

    // ========================================
    // SAUVEGARDER UN FICHIER (SANS LIER AU CHAT)
    // ========================================
    private MultiRoleChatAttachment saveAttachment(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            System.out.println("📁 Dossier créé: " + uploadPath);
        }

        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        Path filePath = uploadPath.resolve(uniqueFileName);

        Files.copy(file.getInputStream(), filePath);
        System.out.println("💾 Fichier sauvegardé: " + filePath);

        // Créer l'attachment SANS le chat (sera ajouté via addAttachment)
        return MultiRoleChatAttachment.builder()
                .fileName(originalFileName)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .filePath(filePath.toString())
                .uploadedAt(LocalDateTime.now())
                .build();
    }

    // ========================================
    // RÉCUPÉRER UNE CONVERSATION
    // ========================================
    public Page<MultiRoleChat> getConversation(Role user1Type, Long user1Id,
                                               Role user2Type, Long user2Id,
                                               int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        return chatRepository.findConversation(user1Type, user1Id, user2Type, user2Id, pageable);
    }

    // ========================================
    // MARQUER COMME LU
    // ========================================
    @Transactional
    public void markAsRead(Long messageId, Role userType, Long userId) {
        MultiRoleChat chat = chatRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message non trouvé"));
        if (chat.getReceiverType() == userType && chat.getReceiverId().equals(userId)) {
            chat.setIsRead(true);
            chatRepository.save(chat);
        }
    }

    // ========================================
    // SUPPRIMER POUR SOI
    // ========================================
    @Transactional
    public void deleteForMe(Long messageId, Role userType, Long userId) {
        MultiRoleChat chat = chatRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message non trouvé"));

        boolean isSender = chat.getSenderType() == userType && chat.getSenderId().equals(userId);
        boolean isReceiver = chat.getReceiverType() == userType && chat.getReceiverId().equals(userId);

        if (!isSender && !isReceiver) {
            throw new RuntimeException("Vous n'êtes pas autorisé à supprimer ce message");
        }

        // ✅ NOUVELLE LOGIQUE : Message non lu -> suppression définitive
        if (!chat.getIsRead()) {
            System.out.println("🗑️ Message non lu (ID: " + messageId + ") - Suppression définitive");
            chatRepository.delete(chat);
            return;
        }

        // ✅ Message lu -> suppression pour soi
        System.out.println("📝 Message lu (ID: " + messageId + ") - Suppression pour " + userType);

        if (isSender) {
            chat.setDeletedBySender(true);
        }
        if (isReceiver) {
            chat.setDeletedByReceiver(true);
        }

        chatRepository.save(chat);
    }

    // ========================================
    // COMPTER MESSAGES NON LUS
    // ========================================
    public long countUnreadMessages(Role userType, Long userId) {
        return chatRepository.countUnreadMessages(userType, userId);
    }

    // ========================================
    // CONVERSATION STYLE MESSENGER
    // ========================================
    public Page<MultiRoleMessageDTO> getConversationAsMessenger(Role user1Type, Long user1Id,
                                                                Role user2Type, Long user2Id,
                                                                int page, int size,
                                                                UserNameResolver nameResolver) {

        System.out.println("📱 Récupération conversation MultiRole Messenger");

        Page<MultiRoleChat> chats = getConversation(user1Type, user1Id, user2Type, user2Id, page, size);

        return chats.map(chat -> {
            MultiRoleMessageDTO dto = new MultiRoleMessageDTO();

            dto.setId(chat.getId());
            dto.setContent(chat.getContent());
            dto.setRead(chat.getIsRead());
            dto.setSentAt(chat.getSentAt());
            dto.setSenderType(chat.getSenderType());
            dto.setSenderId(chat.getSenderId());
            dto.setReceiverType(chat.getReceiverType());
            dto.setReceiverId(chat.getReceiverId());

            // Résoudre les noms
            dto.setSenderName(nameResolver.getUserName(chat.getSenderType(), chat.getSenderId()));
            dto.setReceiverName(nameResolver.getUserName(chat.getReceiverType(), chat.getReceiverId()));

            List<MultiRoleAttachmentDTO> attachmentDTOs = chat.getAttachments().stream()
                    .map(att -> {
                        MultiRoleAttachmentDTO attDto = new MultiRoleAttachmentDTO();
                        attDto.setId(att.getId());
                        attDto.setFileName(att.getFileName());
                        attDto.setFileType(att.getFileType());
                        attDto.setFileSize(att.getFileSize());
                        attDto.setDownloadUrl("/api/multirole-chat/attachment/" + att.getId());
                        return attDto;
                    })
                    .collect(Collectors.toList());

            dto.setAttachments(attachmentDTOs);

            return dto;
        });
    }

    // Interface pour résoudre les noms des utilisateurs
    public interface UserNameResolver {
        String getUserName(Role role, Long userId);
    }

    // ========================================
// RECHERCHER DES UTILISATEURS POUR LE CHAT
// ========================================
// RECHERCHER DES UTILISATEURS POUR LE CHAT (VERSION CORRIGÉE)
// ========================================
// ========================================
// RECHERCHER DES UTILISATEURS POUR LE CHAT (VERSION CORRIGÉE)
// ========================================
    public List<Map<String, Object>> searchUsers(String query, String currentUserEmail) {
        List<Map<String, Object>> results = new ArrayList<>();
        String searchPattern = "%" + query + "%";

        try {
            // Requête SQL avec les noms exacts de vos tables
            String sql =
                    // 1. Table investor
                    "SELECT id, email, first_name, last_name, 'INVESTOR' as role, profile_picture as photo " +
                            "FROM investor WHERE email LIKE ? OR first_name LIKE ? OR last_name LIKE ? " +

                            "UNION " +

                            // 2. Table tourist
                            "SELECT id, email, first_name, last_name, 'TOURIST' as role, profile_photo as photo " +
                            "FROM tourist WHERE email LIKE ? OR first_name LIKE ? OR last_name LIKE ? " +

                            "UNION " +

                            // 3. Table economic_partners (attention: c'est "economic_partners" avec "s" à la fin)
                            "SELECT id, email, first_name, last_name, 'PARTNER' as role, profile_photo as photo " +
                            "FROM economic_partners WHERE email LIKE ? OR first_name LIKE ? OR last_name LIKE ? " +

                            "UNION " +

                            // 4. Table local_partner
                            "SELECT id, email, prenom as first_name, nom as last_name, 'LOCAL_PARTNER' as role, photo_profil as photo " +
                            "FROM local_partner WHERE email LIKE ? OR prenom LIKE ? OR nom LIKE ? " +

                            "UNION " +

                            // 5. Table international_company
                            "SELECT id, email, contact_first_name as first_name, contact_last_name as last_name, " +
                            "'INTERNATIONAL_COMPANY' as role, profile_picture as photo " +
                            "FROM international_company WHERE email LIKE ? OR contact_first_name LIKE ? OR contact_last_name LIKE ? " +

                            "UNION " +

                            // 6. Table admin-platform (attention: tiret dans le nom)
                            "SELECT id, email, first_name, last_name, 'ADMIN' as role, profile_photo as photo " +
                            "FROM `admin-platform` WHERE email LIKE ? OR first_name LIKE ? OR last_name LIKE ?";

            List<Object[]> users = entityManager.createNativeQuery(sql)
                    // Investor (3 paramètres)
                    .setParameter(1, searchPattern)
                    .setParameter(2, searchPattern)
                    .setParameter(3, searchPattern)
                    // Tourist (3 paramètres)
                    .setParameter(4, searchPattern)
                    .setParameter(5, searchPattern)
                    .setParameter(6, searchPattern)
                    // Economic Partners (3 paramètres)
                    .setParameter(7, searchPattern)
                    .setParameter(8, searchPattern)
                    .setParameter(9, searchPattern)
                    // Local Partner (3 paramètres)
                    .setParameter(10, searchPattern)
                    .setParameter(11, searchPattern)
                    .setParameter(12, searchPattern)
                    // International Company (3 paramètres)
                    .setParameter(13, searchPattern)
                    .setParameter(14, searchPattern)
                    .setParameter(15, searchPattern)
                    // Admin (3 paramètres)
                    .setParameter(16, searchPattern)
                    .setParameter(17, searchPattern)
                    .setParameter(18, searchPattern)
                    .getResultList();

            for (Object[] user : users) {
                Long id = ((Number) user[0]).longValue();
                String email = (String) user[1];
                String firstName = (String) user[2];
                String lastName = (String) user[3];
                String role = (String) user[4];
                String photo = (String) user[5];

                // Ne pas inclure l'utilisateur courant
                if (!email.equals(currentUserEmail)) {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", id);
                    userMap.put("email", email);
                    userMap.put("firstName", firstName);
                    userMap.put("lastName", lastName);
                    userMap.put("role", role);
                    userMap.put("profilePhoto", photo);

                    // Construire le nom d'affichage
                    String displayName = "";
                    if (firstName != null && lastName != null) {
                        displayName = firstName + " " + lastName;
                    } else if (firstName != null) {
                        displayName = firstName;
                    } else if (lastName != null) {
                        displayName = lastName;
                    } else {
                        displayName = email.split("@")[0];
                    }
                    userMap.put("displayName", displayName.trim());

                    results.add(userMap);
                }
            }

            System.out.println("🔍 Recherche '" + query + "' retourne " + results.size() + " résultats");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la recherche d'utilisateurs: " + e.getMessage());
        }

        return results;
    }
// ========================================
// ========================================
// RÉCUPÉRER TOUS LES UTILISATEURS (pour admin)
// ========================================
public List<Map<String, Object>> getAllUsers(Role currentUserRole, Long currentUserId, String currentUserEmail) {
    if (currentUserRole != Role.ADMIN) {
        throw new RuntimeException("Accès non autorisé");
    }

    // Appel correct de searchUsers avec 2 arguments
    return searchUsers("", currentUserEmail);
}

    // ========================================
// RÉCUPÉRER LES ADMINS
// ========================================
    // ========================================
// RÉCUPÉRER LES ADMINS (VERSION CORRIGÉE)
// ========================================
    public List<Map<String, Object>> getAdmins(String currentUserEmail) {
        List<Map<String, Object>> admins = new ArrayList<>();

        try {
            // Note: le nom de la table est "admin-platform" avec un tiret
            // Il faut utiliser des backticks ` ` pour les noms avec tirets
            String sql = "SELECT id, email, first_name, last_name, profile_photo " +
                    "FROM `admin-platform` WHERE email != ?";

            List<Object[]> adminResults = entityManager.createNativeQuery(sql)
                    .setParameter(1, currentUserEmail)
                    .getResultList();

            for (Object[] admin : adminResults) {
                Map<String, Object> adminMap = new HashMap<>();
                adminMap.put("id", ((Number) admin[0]).longValue());
                adminMap.put("email", (String) admin[1]);
                adminMap.put("firstName", (String) admin[2]);
                adminMap.put("lastName", (String) admin[3]);
                adminMap.put("role", "ADMIN");
                adminMap.put("profilePhoto", (String) admin[4]);

                String displayName = ((String) admin[2] != null ? admin[2] : "") +
                        ((String) admin[3] != null ? " " + admin[3] : "");
                if (displayName.trim().isEmpty()) {
                    displayName = ((String) admin[1]).split("@")[0];
                }
                adminMap.put("displayName", displayName.trim());

                admins.add(adminMap);
            }

            System.out.println("👥 Nombre d'admins trouvés: " + admins.size());

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des admins: " + e.getMessage());
        }

        return admins;
    }



    // ========================================
// SUPPRIMER UN MESSAGE (VERSION AMÉLIORÉE)
// ========================================
    @Transactional
    public Map<String, Object> deleteMessageConditional(Long messageId, Role userType, Long userId) {
        MultiRoleChat chat = chatRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message non trouvé"));

        // Vérifier que l'utilisateur est soit l'expéditeur soit le destinataire
        boolean isSender = chat.getSenderType() == userType && chat.getSenderId().equals(userId);
        boolean isReceiver = chat.getReceiverType() == userType && chat.getReceiverId().equals(userId);

        if (!isSender && !isReceiver) {
            throw new RuntimeException("Vous n'êtes pas autorisé à supprimer ce message");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("messageId", messageId);
        result.put("senderId", chat.getSenderId());
        result.put("receiverId", chat.getReceiverId());
        result.put("isRead", chat.getIsRead());

        // Si le message n'est pas lu, suppression définitive
        if (!chat.getIsRead()) {
            System.out.println("🗑️ Message non lu (ID: " + messageId + ") - Suppression définitive");

            // Supprimer le message (les attachments seront supprimés en cascade)
            chatRepository.delete(chat);

            result.put("status", "PERMANENT_DELETED");
            result.put("message", "Message non lu supprimé définitivement pour tous les participants");
            result.put("forEveryone", true);
            return result;
        }

        // Si le message est lu, suppression seulement pour l'utilisateur
        System.out.println("📝 Message lu (ID: " + messageId + ") - Suppression pour " + userType + " (ID: " + userId + ")");

        if (isSender) {
            chat.setDeletedBySender(true);
            System.out.println("   → Marqué comme supprimé pour l'expéditeur");
        }
        if (isReceiver) {
            chat.setDeletedByReceiver(true);
            System.out.println("   → Marqué comme supprimé pour le destinataire");
        }

        chatRepository.save(chat);

        result.put("status", "DELETED_FOR_ME");
        result.put("message", "Message lu supprimé de votre vue uniquement");
        result.put("forEveryone", false);

        return result;
    }

    // ========================================
// RÉCUPÉRER LES MESSAGES VISIBLES D'UNE CONVERSATION
// ========================================
    public Page<MultiRoleChat> getVisibleMessages(Role user1Type, Long user1Id,
                                                  Role user2Type, Long user2Id,
                                                  int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("sentAt").ascending());

        // Cette méthode doit être ajoutée dans le repository
        return chatRepository.findVisibleMessages(user1Type, user1Id, user2Type, user2Id, pageable);
    }

    // ========================================
// VÉRIFIER SI UN MESSAGE EST VISIBLE POUR UN UTILISATEUR
// ========================================
    public boolean isMessageVisibleForUser(Long messageId, Role userType, Long userId) {
        MultiRoleChat chat = chatRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message non trouvé"));

        boolean isSender = chat.getSenderType() == userType && chat.getSenderId().equals(userId);
        boolean isReceiver = chat.getReceiverType() == userType && chat.getReceiverId().equals(userId);

        if (!isSender && !isReceiver) return false;

        if (isSender && chat.getDeletedBySender()) return false;
        if (isReceiver && chat.getDeletedByReceiver()) return false;

        return true;
    }

    // ========================================
// OBTENIR LE STATUT D'UN MESSAGE POUR UN UTILISATEUR
// ========================================
    public Map<String, Object> getMessageStatus(Long messageId, Role userType, Long userId) {
        MultiRoleChat chat = chatRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message non trouvé"));

        boolean isSender = chat.getSenderType() == userType && chat.getSenderId().equals(userId);
        boolean isReceiver = chat.getReceiverType() == userType && chat.getReceiverId().equals(userId);

        Map<String, Object> status = new HashMap<>();
        status.put("messageId", messageId);
        status.put("isRead", chat.getIsRead());
        status.put("deletedBySender", chat.getDeletedBySender());
        status.put("deletedByReceiver", chat.getDeletedByReceiver());
        status.put("isVisible", isMessageVisibleForUser(messageId, userType, userId));

        if (isSender) {
            status.put("userRole", "SENDER");
            status.put("canDelete", true);
            status.put("yourSideDeleted", chat.getDeletedBySender());
        } else if (isReceiver) {
            status.put("userRole", "RECEIVER");
            status.put("canDelete", true);
            status.put("yourSideDeleted", chat.getDeletedByReceiver());
        } else {
            status.put("userRole", "NONE");
            status.put("canDelete", false);
            status.put("yourSideDeleted", false);
        }

        // Déterminer le type de suppression possible
        if (!chat.getIsRead()) {
            status.put("deleteType", "PERMANENT");
            status.put("deleteMessage", "Ce message n'a pas été lu. La suppression le supprimera définitivement pour tous.");
        } else {
            status.put("deleteType", "FOR_ME");
            status.put("deleteMessage", "Ce message a été lu. La suppression le supprimera seulement de votre vue.");
        }

        return status;
    }

    // ========================================
// NETTOYER LES MESSAGES SUPPRIMÉS PAR TOUS (TÂCHE PLANIFIÉE OPTIONNELLE)
// ========================================
    @Transactional
    public void cleanUpDeletedMessages() {
        // Supprimer les messages où les deux participants ont supprimé
        List<MultiRoleChat> messagesToDelete = chatRepository.findMessagesDeletedByBoth();

        if (!messagesToDelete.isEmpty()) {
            System.out.println("🧹 Nettoyage de " + messagesToDelete.size() + " messages supprimés par tous");
            chatRepository.deleteAll(messagesToDelete);
        }
    }


}