package tn.iset.investplatformpfe.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import tn.iset.investplatformpfe.Service.KonnectSubscriptionService;
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
    private static final Logger log = LoggerFactory.getLogger(MessagerieController.class);

    private final MessagerieService messagerieService;
    // ✅ Remplace FlouciService + FlouciSubscriptionService
    private final KonnectSubscriptionService konnectSubscriptionService;

    public MessagerieController(MessagerieService messagerieService,
                                KonnectSubscriptionService konnectSubscriptionService) {
        this.messagerieService = messagerieService;
        this.konnectSubscriptionService = konnectSubscriptionService;
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
    // ENVOYER UN MESSAGE SANS PIÈCES JOINTES
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> request) {

        String senderEmail = jwt.getClaimAsString("email");
        String role = getRole(jwt);

        String recipientEmail = request.get("recipientEmail");
        if (recipientEmail == null) recipientEmail = request.get("partnerEmail");
        String content = request.get("content");

        if (content == null || content.trim().isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
        if (recipientEmail == null || recipientEmail.trim().isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Recipient email is required"));

        try {
            Message message = messagerieService.sendMessage(senderEmail, recipientEmail, content, role);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENVOYER UN MESSAGE AVEC PIÈCES JOINTES
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping(value = "/send-with-attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> sendMessageWithAttachments(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("recipientEmail") String recipientEmail,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "attachments", required = false) MultipartFile[] attachments) {

        String senderEmail = jwt.getClaimAsString("email");
        String role = getRole(jwt);

        if (recipientEmail == null || recipientEmail.trim().isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Recipient email is required"));

        if ((content == null || content.trim().isEmpty()) &&
                (attachments == null || attachments.length == 0))
            return ResponseEntity.badRequest().body(Map.of("error", "Message or attachments required"));

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
    // RÉPONDRE
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/reply")
    public ResponseEntity<?> replyMessage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> request) {

        String senderEmail = jwt.getClaimAsString("email");
        String role = getRole(jwt);

        String recipientEmail = request.get("recipientEmail");
        if (recipientEmail == null) recipientEmail = request.get("senderEmail");
        String content = request.get("content");

        if (content == null || content.trim().isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));

        try {
            Message message = messagerieService.sendMessage(senderEmail, recipientEmail, content, role);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/local-partner/reply")
    public ResponseEntity<?> replyMessageLegacy(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> request) {
        return replyMessage(jwt, request);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONVERSATIONS
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/my-conversations")
    public ResponseEntity<?> getMyConversations(@AuthenticationPrincipal Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        return ResponseEntity.ok(messagerieService.getAllConversations(email));
    }

    @GetMapping("/local-partner/my-conversations")
    public ResponseEntity<?> getPartnerConversations(@AuthenticationPrincipal Jwt jwt) {
        return getMyConversations(jwt);
    }

    @GetMapping("/conversation/{otherEmail}")
    public ResponseEntity<?> getConversation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String otherEmail) {
        String myEmail = jwt.getClaimAsString("email");
        try {
            List<Message> messages = messagerieService.getConversation(myEmail, otherEmail);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

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
    // MESSAGES & PIÈCES JOINTES
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/message/{messageId}")
    public ResponseEntity<?> getMessage(@PathVariable Long messageId) {
        try {
            return ResponseEntity.ok(messagerieService.getMessageWithAttachments(messageId));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/attachment/{attachmentId}")
    public ResponseEntity<?> downloadAttachment(@PathVariable Long attachmentId) {
        try {
            MessageAttachment attachment = messagerieService.getAttachment(attachmentId);
            Path filePath = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() && !resource.isReadable())
                return ResponseEntity.notFound().build();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(attachment.getFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + attachment.getFileName() + "\"")
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(attachment.getFileSize()))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors du téléchargement: " + e.getMessage()));
        }
    }

    @GetMapping("/attachment/{attachmentId}/preview")
    public ResponseEntity<?> previewAttachment(@PathVariable Long attachmentId) {
        try {
            MessageAttachment attachment = messagerieService.getAttachment(attachmentId);
            if (!attachment.getFileType().startsWith("image/"))
                return ResponseEntity.badRequest().body(Map.of("error", "Preview only available for images"));

            Path filePath = Paths.get(attachment.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() && !resource.isReadable())
                return ResponseEntity.notFound().build();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(attachment.getFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + attachment.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors de l'affichage: " + e.getMessage()));
        }
    }

    @DeleteMapping("/attachment/{attachmentId}")
    public ResponseEntity<?> deleteAttachment(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long attachmentId) {
        try {
            MessageAttachment attachment = messagerieService.getAttachment(attachmentId);
            Message message = messagerieService.getMessageWithAttachments(attachment.getMessage().getId());
            String currentUserEmail = jwt.getClaimAsString("email");

            if (!message.getSenderEmail().equals(currentUserEmail))
                return ResponseEntity.status(403)
                        .body(Map.of("error", "You can only delete attachments from your own messages"));

            messagerieService.deleteAttachment(attachmentId);
            return ResponseEntity.ok(Map.of("message", "Attachment deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/message/{messageId}/attachments")
    public ResponseEntity<?> deleteAllAttachments(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long messageId) {
        try {
            Message message = messagerieService.getMessageWithAttachments(messageId);
            if (!message.getSenderEmail().equals(jwt.getClaimAsString("email")))
                return ResponseEntity.status(403)
                        .body(Map.of("error", "You can only delete attachments from your own messages"));

            messagerieService.deleteAllAttachments(messageId);
            return ResponseEntity.ok(Map.of("message", "All attachments deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/message/{messageId}/has-attachments")
    public ResponseEntity<?> hasAttachments(@PathVariable Long messageId) {
        try {
            return ResponseEntity.ok(Map.of("hasAttachments", messagerieService.hasAttachments(messageId)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MESSAGES NON LUS
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadMessages(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        String email = jwt.getClaimAsString("email");
        return ResponseEntity.ok(Map.of(
                "unreadCount", messagerieService.countUnreadMessages(email),
                "messages", messagerieService.getUnreadMessages(email)
        ));
    }

    @GetMapping("/unread/count")
    public ResponseEntity<?> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        return ResponseEntity.ok(Map.of("unreadCount",
                messagerieService.countUnreadMessages(jwt.getClaimAsString("email"))));
    }

    @GetMapping("/exists/{recipientEmail}")
    public ResponseEntity<?> conversationExists(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String recipientEmail) {
        String senderEmail = jwt.getClaimAsString("email");
        return ResponseEntity.ok(Map.of("exists",
                messagerieService.conversationExists(senderEmail, recipientEmail)));
    }

    @GetMapping("/user-role/{email}")
    public ResponseEntity<Map<String, String>> getUserRole(@PathVariable String email) {
        return ResponseEntity.ok(Map.of("email", email, "role", messagerieService.getUserRole(email)));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ✅ ABONNEMENT MENSUEL - KONNECT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * 1. Vérifier l'abonnement actif
     */
    @GetMapping("/subscription/check")
    public ResponseEntity<?> checkSubscription(@AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        if ("LOCAL_PARTNER".equals(getRole(jwt)))
            return ResponseEntity.badRequest().body(Map.of("error", "Action non autorisée"));
        return ResponseEntity.ok(messagerieService.checkSubscription(userEmail));
    }

    /**
     * 2. Initier le paiement Konnect → retourne payUrl vers laquelle Angular redirige
     */
    @PostMapping("/subscription/subscribe")
    public ResponseEntity<?> initiateSubscription(@AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        if ("LOCAL_PARTNER".equals(getRole(jwt)))
            return ResponseEntity.badRequest().body(Map.of("error", "Action non autorisée"));

        try {
            //  Une seule méthode fait tout : crée l'abonnement + appelle Konnect
            Map<String, Object> response = messagerieService.initiateSubscriptionPayment(userEmail);

            return ResponseEntity.ok(Map.of(
                    "paymentId",  response.get("paymentId"),   // notre ID interne
                    "payUrl",     response.get("payUrl"),       //  URL Konnect → Angular redirige ici
                    "paymentRef", response.getOrDefault("paymentRef", ""),
                    "amount",     40,
                    "description","Abonnement mensuel — accès illimité aux Local Partners"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 3. Callback après paiement Konnect réussi
     *    Konnect redirige vers : /subscription/payment-success?paymentId=...&paymentRef=...
     */
    @GetMapping("/subscription/payment-success")
    public ResponseEntity<?> subscriptionPaymentSuccess(
            @RequestParam String paymentId,
            @RequestParam(required = false) String paymentRef,
            @RequestParam(required = false) String transaction_id) {

        try {
            log.info("🔔 Callback paiement reçu - paymentId: {}, paymentRef: {}", paymentId, paymentRef);

            String refToVerify = (paymentRef != null) ? paymentRef : paymentId;
            Map<String, Object> verification = konnectSubscriptionService
                    .verifySubscriptionPayment(refToVerify);

            log.info("📋 Résultat vérification: {}", verification);

            boolean paymentSuccess = false;

            if (Boolean.TRUE.equals(verification.get("simulated"))) {
                // ✅ Mode simulation ou fallback test
                log.info("🧪 Mode simulé → succès automatique");
                paymentSuccess = true;

            } else if (verification.containsKey("result")) {
                Map<String, Object> result = (Map<String, Object>) verification.get("result");
                String status = (String) result.get("status");
                log.info("📌 Statut reçu: '{}'", status);
                paymentSuccess = "SUCCESS".equals(status);
            }

            if (paymentSuccess) {
                Map<String, Object> info = messagerieService.confirmSubscriptionPayment(
                        paymentId, transaction_id, refToVerify);
                log.info("✅ Abonnement confirmé: {}", info);
                return ResponseEntity.ok(info);
            } else {
                log.warn("❌ Paiement non confirmé pour paymentId: {}", paymentId);
                messagerieService.markSubscriptionFailed(paymentId);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "error", "Paiement Konnect non confirmé"));
            }

        } catch (Exception e) {
            log.error("❌ Erreur callback: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 4. Callback si paiement échoué
     */
    @GetMapping("/subscription/payment-failed")
    public ResponseEntity<?> subscriptionPaymentFailed(
            @RequestParam(required = false) String paymentId) {
        if (paymentId != null) {
            messagerieService.markSubscriptionFailed(paymentId);
        }
        return ResponseEntity.ok(Map.of("success", false, "message", "Paiement annulé ou échoué"));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITAIRES
    // ─────────────────────────────────────────────────────────────────────────

    private String getRole(Jwt jwt) {
        if (jwt == null) return "UNKNOWN";
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles != null) {
                for (String r : List.of("INVESTOR", "PARTNER", "LOCAL_PARTNER", "TOURIST", "ADMIN")) {
                    if (roles.contains(r)) return r;
                }
            }
        }
        return "UNKNOWN";
    }

}