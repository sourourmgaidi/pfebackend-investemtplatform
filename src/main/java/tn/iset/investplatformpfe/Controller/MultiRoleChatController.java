package tn.iset.investplatformpfe.Controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.iset.investplatformpfe.Dto.MultiRoleMessageDTO;
import tn.iset.investplatformpfe.Entity.MultiRoleChat;
import tn.iset.investplatformpfe.Entity.MultiRoleChatAttachment;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Repository.MultiRoleChatAttachmentRepository;
import tn.iset.investplatformpfe.Repository.MultiRoleChatRepository;
import tn.iset.investplatformpfe.Service.MultiRoleChatService;
import tn.iset.investplatformpfe.Service.UserService;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/multirole-chat")
@CrossOrigin(origins = "http://localhost:4200")
public class MultiRoleChatController {

    private final MultiRoleChatService chatService;
    private final UserService userService;
    private final MultiRoleChatAttachmentRepository attachmentRepository;
    private final MultiRoleChatRepository chatRepository;

    public MultiRoleChatController(MultiRoleChatService chatService,
                                   UserService userService,
                                   MultiRoleChatAttachmentRepository attachmentRepository,
                                   MultiRoleChatRepository chatRepository) {
        this.chatService = chatService;
        this.userService = userService;
        this.attachmentRepository = attachmentRepository;
        this.chatRepository = chatRepository;
    }

    // ========================================
    // EXTRAIRE LE RÔLE DU JWT
    // ========================================
    private Role extractRole(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles != null) {
                if (roles.contains("ADMIN")) return Role.ADMIN;
                if (roles.contains("LOCAL_PARTNER")) return Role.LOCAL_PARTNER;
                if (roles.contains("PARTNER")) return Role.PARTNER;
                if (roles.contains("INTERNATIONAL_COMPANY")) return Role.INTERNATIONAL_COMPANY;
                if (roles.contains("INVESTOR")) return Role.INVESTOR;
                if (roles.contains("TOURIST")) return Role.TOURIST;
            }
        }
        throw new RuntimeException("Rôle non trouvé dans le token");
    }

    // ========================================
    // ENVOYER MESSAGE SIMPLE
    // ========================================
    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, Object> payload) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            Role senderRole = extractRole(jwt);
            Long senderId = userService.getUserIdByEmailAndRole(
                    jwt.getClaimAsString("email"), senderRole);

            Role receiverRole = Role.valueOf((String) payload.get("receiverRole"));
            Long receiverId = Long.valueOf(payload.get("receiverId").toString());
            String content = (String) payload.get("content");

            MultiRoleChat chat = chatService.sendMessage(
                    senderRole, senderId, receiverRole, receiverId, content);

            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // ENVOYER MESSAGE AVEC FICHIERS
    // ========================================
    @PostMapping(value = "/send-with-attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> sendMessageWithAttachments(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("receiverRole") String receiverRoleStr,
            @RequestParam("receiverId") Long receiverId,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "attachments", required = false) MultipartFile[] attachments) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            Role senderRole = extractRole(jwt);
            Long senderId = userService.getUserIdByEmailAndRole(
                    jwt.getClaimAsString("email"), senderRole);

            Role receiverRole = Role.valueOf(receiverRoleStr);

            MultiRoleChat chat = chatService.sendMessageWithAttachments(
                    senderRole, senderId, receiverRole, receiverId, content, attachments);

            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // VOIR CONVERSATION STYLE MESSENGER
    // ========================================
    @GetMapping("/conversation/messenger/{targetRole}/{targetId}")
    public ResponseEntity<?> getConversationAsMessenger(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Role targetRole,
            @PathVariable Long targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            Role userRole = extractRole(jwt);
            Long userId = userService.getUserIdByEmailAndRole(
                    jwt.getClaimAsString("email"), userRole);

            Page<MultiRoleMessageDTO> conversation = chatService.getConversationAsMessenger(
                    userRole, userId, targetRole, targetId, page, size,
                    (role, id) -> userService.getUserFullName(
                            userService.getUserEmailByIdAndRole(id, role), role)
            );

            return ResponseEntity.ok(conversation);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // VOIR CONVERSATION SIMPLE
    // ========================================
    @GetMapping("/conversation/{targetRole}/{targetId}")
    public ResponseEntity<?> getConversation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Role targetRole,
            @PathVariable Long targetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            Role userRole = extractRole(jwt);
            Long userId = userService.getUserIdByEmailAndRole(
                    jwt.getClaimAsString("email"), userRole);

            Page<MultiRoleChat> conversation = chatService.getConversation(
                    userRole, userId, targetRole, targetId, page, size);

            return ResponseEntity.ok(conversation);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // VOIR LES FICHIERS D'UN MESSAGE
    // ========================================
    @GetMapping("/{chatId}/attachments")
    public ResponseEntity<?> getChatAttachments(@PathVariable Long chatId) {
        try {
            List<MultiRoleChatAttachment> attachments = attachmentRepository.findByChatId(chatId);
            return ResponseEntity.ok(attachments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // TÉLÉCHARGER UN FICHIER
    // ========================================
    // ========================================
// TÉLÉCHARGER UN FICHIER (AVEC AUTHENTIFICATION)
// ========================================
    @GetMapping("/attachment/{attachmentId}")
    public ResponseEntity<?> downloadAttachment(
            @AuthenticationPrincipal Jwt jwt,  // ← AJOUTER CETTE LIGNE
            @PathVariable Long attachmentId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            MultiRoleChatAttachment attachment = attachmentRepository.findById(attachmentId)
                    .orElseThrow(() -> new RuntimeException("Attachment non trouvé"));

            Path filePath = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + attachment.getFileName() + "\"")
                    .contentType(MediaType.parseMediaType(attachment.getFileType()))
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    // ========================================
    // MARQUER UN MESSAGE COMME LU
    // ========================================
    @PutMapping("/{messageId}/read")
    public ResponseEntity<?> markAsRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long messageId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            Role userRole = extractRole(jwt);
            Long userId = userService.getUserIdByEmailAndRole(
                    jwt.getClaimAsString("email"), userRole);

            chatService.markAsRead(messageId, userRole, userId);
            return ResponseEntity.ok(Map.of("message", "Message marqué comme lu"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // SUPPRIMER UN MESSAGE POUR SOI
    // ========================================
    @DeleteMapping("/{messageId}")
    public ResponseEntity<?> deleteForMe(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long messageId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            Role userRole = extractRole(jwt);
            Long userId = userService.getUserIdByEmailAndRole(
                    jwt.getClaimAsString("email"), userRole);

            // Récupérer le message pour vérifier s'il est lu avant suppression
            MultiRoleChat chat = chatRepository.findById(messageId)
                    .orElseThrow(() -> new RuntimeException("Message non trouvé"));

            boolean wasUnread = !chat.getIsRead();

            chatService.deleteForMe(messageId, userRole, userId);

            Map<String, Object> response = new HashMap<>();
            response.put("messageId", messageId);

            if (wasUnread) {
                response.put("message", "Message non lu supprimé définitivement pour tous");
                response.put("type", "PERMANENT");
            } else {
                response.put("message", "Message lu supprimé de votre vue");
                response.put("type", "FOR_ME");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // COMPTER LES MESSAGES NON LUS
    // ========================================
    @GetMapping("/unread/count")
    public ResponseEntity<?> countUnread(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            Role userRole = extractRole(jwt);
            Long userId = userService.getUserIdByEmailAndRole(
                    jwt.getClaimAsString("email"), userRole);

            long count = chatService.countUnreadMessages(userRole, userId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // LISTER TOUTES LES CONVERSATIONS
    // ========================================
    @GetMapping("/conversations")
    public ResponseEntity<?> getUserConversations(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            Role userRole = extractRole(jwt);
            Long userId = userService.getUserIdByEmailAndRole(
                    jwt.getClaimAsString("email"), userRole);

            List<Object[]> conversations = chatRepository.findUserConversations(userRole, userId);
            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
// RECHERCHER DES UTILISATEURS POUR LE CHAT
// ========================================
    @GetMapping("/users/search")
    public ResponseEntity<?> searchUsers(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String q) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            String currentUserEmail = jwt.getClaimAsString("email");

            // ✅ CORRIGÉ: Appel avec seulement 2 arguments
            List<Map<String, Object>> users = chatService.searchUsers(q, currentUserEmail);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    // ========================================
// RÉCUPÉRER TOUS LES UTILISATEURS (pour admin)
// ========================================
    @GetMapping("/users/all")
    public ResponseEntity<?> getAllUsers(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            Role currentUserRole = extractRole(jwt);
            String currentUserEmail = jwt.getClaimAsString("email");
            Long currentUserId = userService.getUserIdByEmailAndRole(currentUserEmail, currentUserRole);

            // Seul l'admin peut voir tous les utilisateurs
            if (currentUserRole != Role.ADMIN) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));
            }

            List<Map<String, Object>> users = chatService.getAllUsers(currentUserRole, currentUserId, currentUserEmail);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
// RÉCUPÉRER LES ADMINS (pour les autres utilisateurs)
// ========================================
    @GetMapping("/users/admins")
    public ResponseEntity<?> getAdmins(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            String currentUserEmail = jwt.getClaimAsString("email");

            // Appel de la méthode corrigée
            List<Map<String, Object>> admins = chatService.getAdmins(currentUserEmail);
            return ResponseEntity.ok(admins);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
// DÉMARRER UNE NOUVELLE CONVERSATION
// ========================================
    @PostMapping("/conversation/start")
    public ResponseEntity<?> startConversation(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, Object> payload) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            Role senderRole = extractRole(jwt);
            Long senderId = userService.getUserIdByEmailAndRole(
                    jwt.getClaimAsString("email"), senderRole);

            Role receiverRole = Role.valueOf((String) payload.get("receiverRole"));
            Long receiverId = Long.valueOf(payload.get("receiverId").toString());
            String content = (String) payload.get("content");

            MultiRoleChat chat = chatService.sendMessage(
                    senderRole, senderId, receiverRole, receiverId, content);

            return ResponseEntity.ok(chat);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }


}
