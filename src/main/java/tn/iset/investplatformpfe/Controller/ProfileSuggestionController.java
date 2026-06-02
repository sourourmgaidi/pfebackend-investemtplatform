package tn.iset.investplatformpfe.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Dto.ProfileSuggestionRequest;
import tn.iset.investplatformpfe.Dto.ProfileSuggestionResponse;
import tn.iset.investplatformpfe.Service.ProfileSuggestionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai/suggestions")
@CrossOrigin(origins = "http://localhost:4200")
public class ProfileSuggestionController {

    private final ProfileSuggestionService suggestionService;

    public ProfileSuggestionController(ProfileSuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    /**
     * POST /api/ai/suggestions/profile
     * Body: { "question": "..." }
     * Retourne: { answer, role, userIdentifier, recommendedServices[] }
     */
    @PostMapping("/profile")
    public ResponseEntity<?> getProfileSuggestion(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ProfileSuggestionRequest request) {

        if (jwt == null)
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));

        String email = jwt.getClaimAsString("email");
        String role  = extractRole(jwt);

        if (email == null || email.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Email introuvable dans le token"));

        if (request.getQuestion() == null || request.getQuestion().isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "La question est requise"));

        try {
            ProfileSuggestionResponse response =
                    suggestionService.generateSuggestion(email, role, request.getQuestion());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/ai/suggestions/health
     * Vérifier que l'agent est actif
     */
    @GetMapping("/health")
    public ResponseEntity<?> health(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null)
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        return ResponseEntity.ok(Map.of(
                "status", "Agent IA actif",
                "role",   extractRole(jwt),
                "email",  jwt.getClaimAsString("email")
        ));
    }

    // ── helper ──────────────────────────────────────────────
    private String extractRole(Jwt jwt) {
        try {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null) return "USER";
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles == null) return "USER";
            List<String> system = List.of(
                    "default-roles-invest-platform", "offline_access", "uma_authorization");
            return roles.stream().filter(r -> !system.contains(r))
                    .findFirst().orElse("USER");
        } catch (Exception e) {
            return "USER";
        }
    }
}
