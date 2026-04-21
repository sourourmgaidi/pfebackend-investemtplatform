package tn.iset.investplatformpfe.Controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Service.FlouciService;
import tn.iset.investplatformpfe.Service.FlouciSubscriptionService;
import tn.iset.investplatformpfe.Service.MessagerieService;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/messagerie")
public class MessagerieController {

    private final MessagerieService messagerieService;
    private final FlouciService flouciService;
    private final FlouciSubscriptionService flouciSubscriptionService;



    public MessagerieController(MessagerieService messagerieService,
                                FlouciService flouciService,
                                FlouciSubscriptionService flouciSubscriptionService) {
        this.messagerieService = messagerieService;
        this.flouciService = flouciService;
        this.flouciSubscriptionService = flouciSubscriptionService;


    }

    // ─────────────────────────────────────────────────────────────────────────
    // RECHERCHE
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/search-local-partners")
    public ResponseEntity<?> searchLocalPartners(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String q) {

        if (q.length() < 2) {
            return ResponseEntity.badRequest().body(Map.of("error", "Minimum 2 characters required"));
        }
        return ResponseEntity.ok(messagerieService.searchLocalPartners(q));
    }

    @GetMapping("/search-conversations")
    public ResponseEntity<?> searchConversations(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String q) {

        String email = jwt.getClaimAsString("email");
        return ResponseEntity.ok(messagerieService.searchSenderConversations(email, q));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENVOYER UN MESSAGE SANS PIÈCES JOINTES (JSON)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> request) {

        String senderEmail = jwt.getClaimAsString("email");
        String role = getRole(jwt);

        // Accepte "recipientEmail" ET "partnerEmail" pour compatibilité
        String recipientEmail = request.get("recipientEmail");
        if (recipientEmail == null) recipientEmail = request.get("partnerEmail");

        String content = request.get("content");

        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
        }
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Recipient email is required"));
        }

        try {
            Message message = messagerieService.sendMessage(senderEmail, recipientEmail, content, role);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENVOYER UN MESSAGE AVEC PIÈCES JOINTES (multipart/form-data)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping(value = "/send-with-attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> sendMessageWithAttachments(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("recipientEmail") String recipientEmail,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "attachments", required = false) MultipartFile[] attachments) {

        String senderEmail = jwt.getClaimAsString("email");
        String role = getRole(jwt);

        // Vérifier que le destinataire est spécifié
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Recipient email is required"));
        }

        // Vérifier qu'il y a au moins du contenu ou des pièces jointes
        if ((content == null || content.trim().isEmpty()) &&
                (attachments == null || attachments.length == 0)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message or attachments required"));
        }

        try {
            Message message = messagerieService.sendMessageWithAttachments(
                    senderEmail, recipientEmail, content, role, attachments);

            Map<String, Object> response = new HashMap<>();
            response.put("message", message);
            response.put("attachmentCount", message.getAttachments() != null ? message.getAttachments().size() : 0);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors de l'envoi des fichiers: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENVOYER UN MESSAGE (version unifiée - détecte automatiquement le type)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping(value = "/send-unified", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> sendMessageUnified(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(value = "recipientEmail", required = false) String recipientEmailParam,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "attachments", required = false) MultipartFile[] attachments,
            @RequestBody(required = false) Map<String, String> requestBody) {

        String senderEmail = jwt.getClaimAsString("email");
        String role = getRole(jwt);

        String recipientEmail = recipientEmailParam;
        String messageContent = content;

        // Si c'est une requête JSON (pas de paramètres multipart)
        if (recipientEmail == null && requestBody != null) {
            recipientEmail = requestBody.get("recipientEmail");
            if (recipientEmail == null) recipientEmail = requestBody.get("partnerEmail");
            messageContent = requestBody.get("content");
        }

        // Vérifier le destinataire
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Recipient email is required"));
        }

        // Vérifier le contenu
        if ((messageContent == null || messageContent.trim().isEmpty()) &&
                (attachments == null || attachments.length == 0)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message or attachments required"));
        }

        try {
            Message message;
            if (attachments != null && attachments.length > 0) {
                // Envoi avec pièces jointes
                message = messagerieService.sendMessageWithAttachments(
                        senderEmail, recipientEmail, messageContent, role, attachments);
            } else {
                // Envoi simple
                message = messagerieService.sendMessage(
                        senderEmail, recipientEmail, messageContent, role);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("message", message);
            response.put("attachmentCount", message.getAttachments() != null ? message.getAttachments().size() : 0);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors de l'envoi des fichiers: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RÉPONDRE (endpoint unifié - remplace /local-partner/reply)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/reply")
    public ResponseEntity<?> replyMessage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> request) {

        String senderEmail = jwt.getClaimAsString("email");
        String role = getRole(jwt);

        // Accepte "recipientEmail" ET "senderEmail" pour compatibilité
        String recipientEmail = request.get("recipientEmail");
        if (recipientEmail == null) recipientEmail = request.get("senderEmail");

        String content = request.get("content");

        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
        }

        try {
            Message message = messagerieService.sendMessage(senderEmail, recipientEmail, content, role);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Compatibilité avec l'ancien endpoint /local-partner/reply
    @PostMapping("/local-partner/reply")
    public ResponseEntity<?> replyMessageLegacy(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> request) {
        return replyMessage(jwt, request);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LISTE DES CONVERSATIONS (universel - tous les rôles)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/my-conversations")
    public ResponseEntity<?> getMyConversations(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        // Retourne toutes les convs où l'utilisateur est sender OU recipient
        return ResponseEntity.ok(messagerieService.getAllConversations(email));
    }

    // Compatibilité avec l'ancien endpoint /local-partner/my-conversations
    @GetMapping("/local-partner/my-conversations")
    public ResponseEntity<?> getPartnerConversations(@AuthenticationPrincipal Jwt jwt) {
        return getMyConversations(jwt);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RÉCUPÉRER UNE CONVERSATION SPÉCIFIQUE
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/conversation/{otherEmail}")
    public ResponseEntity<?> getConversation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String otherEmail) {

        String myEmail = jwt.getClaimAsString("email");
        try {
            List<Message> messages = messagerieService.getConversation(myEmail, otherEmail);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of()); // Retourner liste vide si pas de conversation
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RÉCUPÉRER UN MESSAGE SPÉCIFIQUE AVEC SES PIÈCES JOINTES
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/message/{messageId}")
    public ResponseEntity<?> getMessage(@PathVariable Long messageId) {
        try {
            Message message = messagerieService.getMessageWithAttachments(messageId);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TÉLÉCHARGER UNE PIÈCE JOINTE
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/attachment/{attachmentId}")
    public ResponseEntity<?> downloadAttachment(@PathVariable Long attachmentId) {
        try {
            MessageAttachment attachment = messagerieService.getAttachment(attachmentId);

            Path filePath = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() && !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Déterminer le type MIME
            MediaType mediaType = MediaType.parseMediaType(attachment.getFileType());

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + attachment.getFileName() + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(attachment.getFileSize()))
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors du téléchargement: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRÉVISUALISER UNE PIÈCE JOINTE (pour les images)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/attachment/{attachmentId}/preview")
    public ResponseEntity<?> previewAttachment(@PathVariable Long attachmentId) {
        try {
            MessageAttachment attachment = messagerieService.getAttachment(attachmentId);

            // Vérifier si c'est une image
            if (!attachment.getFileType().startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Preview only available for images"));
            }

            Path filePath = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() && !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            MediaType mediaType = MediaType.parseMediaType(attachment.getFileType());

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getFileName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors de l'affichage: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUPPRIMER UNE PIÈCE JOINTE
    // ─────────────────────────────────────────────────────────────────────────

    @DeleteMapping("/attachment/{attachmentId}")
    public ResponseEntity<?> deleteAttachment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long attachmentId) {

        try {
            // Vérifier d'abord que l'utilisateur a le droit de supprimer
            MessageAttachment attachment = messagerieService.getAttachment(attachmentId);
            Message message = messagerieService.getMessageWithAttachments(attachment.getMessage().getId());

            String currentUserEmail = jwt.getClaimAsString("email");

            // Vérifier que l'utilisateur est l'expéditeur du message
            if (!message.getSenderEmail().equals(currentUserEmail)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "You can only delete attachments from your own messages"));
            }

            messagerieService.deleteAttachment(attachmentId);
            return ResponseEntity.ok(Map.of("message", "Attachment deleted successfully"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUPPRIMER TOUTES LES PIÈCES JOINTES D'UN MESSAGE
    // ─────────────────────────────────────────────────────────────────────────

    @DeleteMapping("/message/{messageId}/attachments")
    public ResponseEntity<?> deleteAllAttachments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long messageId) {

        try {
            Message message = messagerieService.getMessageWithAttachments(messageId);
            String currentUserEmail = jwt.getClaimAsString("email");

            // Vérifier que l'utilisateur est l'expéditeur du message
            if (!message.getSenderEmail().equals(currentUserEmail)) {
                return ResponseEntity.status(403)
                        .body(Map.of("error", "You can only delete attachments from your own messages"));
            }

            messagerieService.deleteAllAttachments(messageId);
            return ResponseEntity.ok(Map.of("message", "All attachments deleted successfully"));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VÉRIFIER SI UN MESSAGE A DES PIÈCES JOINTES
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/message/{messageId}/has-attachments")
    public ResponseEntity<?> hasAttachments(@PathVariable Long messageId) {
        try {
            boolean hasAttachments = messagerieService.hasAttachments(messageId);
            return ResponseEntity.ok(Map.of("hasAttachments", hasAttachments));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MESSAGES NON LUS
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadMessages(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String email = jwt.getClaimAsString("email");
        long count = messagerieService.countUnreadMessages(email);

        Map<String, Object> response = new HashMap<>();
        response.put("unreadCount", count);
        response.put("messages", messagerieService.getUnreadMessages(email));

        return ResponseEntity.ok(response);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VÉRIFIER SI UNE CONVERSATION EXISTE
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/exists/{recipientEmail}")
    public ResponseEntity<?> conversationExists(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String recipientEmail) {

        String senderEmail = jwt.getClaimAsString("email");
        boolean exists = messagerieService.conversationExists(senderEmail, recipientEmail);

        return ResponseEntity.ok(Map.of("exists", exists));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITAIRES
    // ─────────────────────────────────────────────────────────────────────────

    private boolean hasRole(Jwt jwt, String role) {
        if (jwt == null) return false;
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles != null && roles.contains(role);
        }
        return false;
    }

    private String getRole(Jwt jwt) {
        if (jwt == null) return "UNKNOWN";
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles != null) {
                // Priorité aux rôles métier
                for (String r : List.of("INVESTOR", "PARTNER", "LOCAL_PARTNER", "TOURIST", "ADMIN")) {
                    if (roles.contains(r)) return r;
                }
            }
        }
        return "UNKNOWN";
    }
    @GetMapping("/user-role/{email}")
    public ResponseEntity<Map<String, String>> getUserRole(@PathVariable String email) {
        String role = messagerieService.getUserRole(email);
        Map<String, String> response = new HashMap<>();
        response.put("email", email);
        response.put("role", role);
        return ResponseEntity.ok(response);
    }
    // ─────────────────────────────────────────────────────────────────────────
// MARQUER UNE CONVERSATION COMME LUE (quand l'utilisateur l'ouvre)
// ─────────────────────────────────────────────────────────────────────────

    @PutMapping("/conversation/{otherEmail}/read")
    public ResponseEntity<?> markConversationAsRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String otherEmail) {

        String myEmail = jwt.getClaimAsString("email");

        try {
            messagerieService.markConversationAsRead(myEmail, otherEmail);
            return ResponseEntity.ok(Map.of("message", "Conversation marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    // ─────────────────────────────────────────────────────────────────────────
// COMPTER UNIQUEMENT LE NOMBRE DE MESSAGES NON LUS (sans les marquer)
// ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/unread/count")
    public ResponseEntity<?> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String email = jwt.getClaimAsString("email");
        long count = messagerieService.countUnreadMessages(email);

        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
    // ========================================
// ENDPOINTS CONTACT LOCAL PARTNER AVEC PAIEMENT
// ========================================


    // ========================================
// ENDPOINTS ABONNEMENT MENSUEL
// ========================================

    /**
     * 1. Vérifier l'abonnement actif
     */
    @GetMapping("/subscription/check")
    public ResponseEntity<?> checkSubscription(@AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        String role = getRole(jwt);

        if ("LOCAL_PARTNER".equals(role)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Action non autorisée"));
        }

        return ResponseEntity.ok(messagerieService.checkSubscription(userEmail));
    }

    /**
     * 2. Initier le paiement de l'abonnement mensuel
     */
    @PostMapping("/subscription/subscribe")
    public ResponseEntity<?> initiateSubscription(@AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        String role = getRole(jwt);

        if ("LOCAL_PARTNER".equals(role)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Action non autorisée"));
        }

        try {
            Subscription sub = messagerieService.createSubscriptionSession(userEmail);

            // ✅ Utiliser le nouveau service d'abonnement
            Map<String, Object> flouciResponse = flouciSubscriptionService.initiateSubscriptionPayment(40.0, sub.getPaymentId());

            String paymentUrl = null;
            if (flouciResponse.containsKey("result")) {
                Map<String, Object> result = (Map<String, Object>) flouciResponse.get("result");
                paymentUrl = (String) result.get("link");
            }

            return ResponseEntity.ok(Map.of(
                    "paymentId", sub.getPaymentId(),
                    "paymentUrl", paymentUrl,
                    "amount", 40,
                    "description", "Abonnement mensuel — accès illimité aux Local Partners"
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    /**
     * 3. Callback après paiement réussi
     */
    @GetMapping("/subscription/payment-success")
    public ResponseEntity<?> subscriptionPaymentSuccess(
            @RequestParam String paymentId,
            @RequestParam(required = false) String transaction_id) {

        try {
            // ✅ Utiliser le nouveau service d'abonnement
            Map<String, Object> verification = flouciSubscriptionService.verifySubscriptionPayment(paymentId);

            boolean paymentSuccess = false;
            if (verification.containsKey("result")) {
                Map<String, Object> result = (Map<String, Object>) verification.get("result");
                String status = (String) result.get("status");
                paymentSuccess = "SUCCESS".equals(status);
            } else if (verification.containsKey("simulated")) {
                paymentSuccess = true;
            }

            if (paymentSuccess) {
                Map<String, Object> info = messagerieService.confirmSubscriptionPayment(
                        paymentId, transaction_id, paymentId);
                return ResponseEntity.ok(info);
            } else {
                messagerieService.markSubscriptionFailed(paymentId);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Paiement échoué"));
            }

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

}