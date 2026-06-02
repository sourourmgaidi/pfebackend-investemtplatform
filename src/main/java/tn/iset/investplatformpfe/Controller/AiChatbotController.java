package tn.iset.investplatformpfe.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import tn.iset.investplatformpfe.Entity.ChatMessageJur;
import tn.iset.investplatformpfe.Repository.ChatMessageJurRepository;
import java.time.LocalDateTime;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@SpringBootApplication(scanBasePackages = "tn.iset.investplatformpfe")
@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "*")
public class AiChatbotController {

    // URL of the Flask RAG server
    private static final String FLASK_URL = "http://localhost:5000/api/ask";

    // Allowed roles that can use the chatbot
    private static final List<String> ALLOWED_ROLES = List.of(
            "INVESTOR", "PARTNER", "INTERNATIONAL_COMPANY"
    );

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private ChatMessageJurRepository chatMessageJurRepository; // Injection du repository

    @PostMapping("/ask")
    public ResponseEntity<?> askQuestion(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> body) {

        // Role check
        if (!hasAllowedRole(jwt)) {
            return ResponseEntity.status(403)
                    .body(Map.of("error", "Access reserved for investors, economic partners and international companies"));
        }

        String question = body.getOrDefault("question", "").trim();
        if (question.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Question is required"));
        }

        try {
            //  Récupérer user ID depuis token
            String userId = jwt.getSubject();

            //  1. Sauvegarder message USER
            ChatMessageJur userMsg = new ChatMessageJur();
            userMsg.setSender("USER");
            userMsg.setMessage(question);
            userMsg.setTimestamp(LocalDateTime.now());
            userMsg.setUserId(userId);

            chatMessageJurRepository.save(userMsg);

            //  Appel Flask
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request =
                    new HttpEntity<>(Map.of("question", question), headers);

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(FLASK_URL, request, Map.class);

            //  2. Récupérer réponse BOT
            String answer = (String) response.getBody().get("answer");

            //  3. Sauvegarder message BOT
            ChatMessageJur botMsg = new ChatMessageJur();
            botMsg.setSender("BOT");
            botMsg.setMessage(answer);
            botMsg.setTimestamp(LocalDateTime.now());
            botMsg.setUserId(userId);

            chatMessageJurRepository.save(botMsg);

            return ResponseEntity.ok(response.getBody());

        } catch (Exception e) {
            e.printStackTrace(); //  affiche l'erreur dans la console Spring

            return ResponseEntity.status(503)
                    .body(Map.of(
                            "error", "AI service temporarily unavailable. Please try again later.",
                            "details", e.getMessage() // 🔥 pour voir l'erreur dans Postman
                    ));
        }
    }
    @GetMapping("/history")
    public List<ChatMessageJur> getHistory(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return chatMessageJurRepository.findByUserIdOrderByTimestampAsc(userId);
    }
    private boolean hasAllowedRole(Jwt jwt) {
        if (jwt == null) return false;
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return false;
        List<String> roles = (List<String>) realmAccess.get("roles");
        if (roles == null) return false;
        return ALLOWED_ROLES.stream().anyMatch(roles::contains);
    }
}