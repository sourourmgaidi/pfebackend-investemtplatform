package tn.iset.investplatformpfe.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import tn.iset.investplatformpfe.Entity.ChatMessageJur;
import tn.iset.investplatformpfe.Repository.ChatMessageJurRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chatbot")
@CrossOrigin(origins = "*")
public class AiChatbotController {

    private static final String FLASK_URL = "http://localhost:5000/api/ask";

    private static final List<String> ALLOWED_ROLES = List.of(
            "INVESTOR", "PARTNER", "INTERNATIONAL_COMPANY"
    );

    @Autowired
    private ChatMessageJurRepository chatMessageJurRepository;

    @Autowired
    private RestTemplate restTemplate;

    // ============================
    // ✅ TIMEOUT — uniquement avec SimpleClientHttpRequestFactory
    // Aucune dépendance externe requise
    // ============================
    @Configuration
    static class RestTemplateConfig {

        @Bean
        public RestTemplate restTemplate() {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(5000);   // 5 secondes pour se connecter
            factory.setReadTimeout(30000);      // 30 secondes pour attendre la réponse Flask+Groq
            return new RestTemplate(factory);
        }
    }

    // ============================
    // POST /api/chatbot/ask
    // ============================
    @PostMapping("/ask")
    public ResponseEntity<?> askQuestion(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> body) {

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
            String userId = jwt.getSubject();

            // 1. Sauvegarder message USER
            ChatMessageJur userMsg = new ChatMessageJur();
            userMsg.setSender("USER");
            userMsg.setMessage(question);
            userMsg.setTimestamp(LocalDateTime.now());
            userMsg.setUserId(userId);
            chatMessageJurRepository.save(userMsg);

            // 2. Appel Flask avec timeout
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request =
                    new HttpEntity<>(Map.of("question", question), headers);

            ResponseEntity<Map> response =
                    restTemplate.postForEntity(FLASK_URL, request, Map.class);

            // 3. Sauvegarder réponse BOT
            String answer = (String) response.getBody().get("answer");

            ChatMessageJur botMsg = new ChatMessageJur();
            botMsg.setSender("BOT");
            botMsg.setMessage(answer);
            botMsg.setTimestamp(LocalDateTime.now());
            botMsg.setUserId(userId);
            chatMessageJurRepository.save(botMsg);

            return ResponseEntity.ok(response.getBody());

        } catch (ResourceAccessException e) {
            return ResponseEntity.status(503)
                    .body(Map.of(
                            "error", "Le service IA ne répond pas. Veuillez réessayer dans quelques secondes.",
                            "details", e.getMessage()
                    ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(503)
                    .body(Map.of(
                            "error", "AI service temporarily unavailable. Please try again later.",
                            "details", e.getMessage()
                    ));
        }
    }

    // ============================
    // GET /api/chatbot/history
    // ============================
    @GetMapping("/history")
    public List<ChatMessageJur> getHistory(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return chatMessageJurRepository.findByUserIdOrderByTimestampAsc(userId);
    }

    // ============================
    // VÉRIFICATION RÔLE
    // ============================
    private boolean hasAllowedRole(Jwt jwt) {
        if (jwt == null) return false;
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return false;
        List<String> roles = (List<String>) realmAccess.get("roles");
        if (roles == null) return false;
        return ALLOWED_ROLES.stream().anyMatch(roles::contains);
    }
}