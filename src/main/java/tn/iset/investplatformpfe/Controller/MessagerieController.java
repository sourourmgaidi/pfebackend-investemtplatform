package tn.iset.investplatformpfe.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.Message;
import tn.iset.investplatformpfe.Entity.Conversation;
import tn.iset.investplatformpfe.Service.MessagerieService;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/messagerie")
public class MessagerieController {

    private final MessagerieService messagerieService;

    public MessagerieController(MessagerieService messagerieService) {
        this.messagerieService = messagerieService;
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
    // ENVOYER UN MESSAGE (universel - tous les rôles)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> request) {

        String senderEmail = jwt.getClaimAsString("email");
        String role = getRole(jwt);

        // ✅ Accepte "recipientEmail" ET "partnerEmail" pour compatibilité
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
    // RÉPONDRE (endpoint unifié - remplace /local-partner/reply)
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping("/reply")
    public ResponseEntity<?> replyMessage(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> request) {

        String senderEmail = jwt.getClaimAsString("email");
        String role = getRole(jwt);

        // ✅ Accepte "recipientEmail" ET "senderEmail" pour compatibilité
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
        // ✅ Retourne toutes les convs où l'utilisateur est sender OU recipient
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
}