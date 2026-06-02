package tn.iset.investplatformpfe.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Dto.AiRecommendationDTO.AiRecommendationResponse;
import tn.iset.investplatformpfe.Service.AiRecommendationService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiRecommendationController {

    private static final Logger log = LoggerFactory.getLogger(AiRecommendationController.class);

    private final AiRecommendationService aiRecommendationService;

    public AiRecommendationController(AiRecommendationService aiRecommendationService) {
        this.aiRecommendationService = aiRecommendationService;
    }

    @PostMapping("/recommend")
    public ResponseEntity<?> getAiRecommendations(@AuthenticationPrincipal Jwt jwt) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié."));
        }

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email introuvable dans le token."));
        }

        String rawRole = extractRoleFromJwt(jwt);
        String userRole = normalizeRole(rawRole);

        log.info("🔑 Rôle JWT brut: '{}' → Rôle normalisé: '{}'", rawRole, userRole);

        if (userRole.equals("UNKNOWN")) {
            log.error("❌ Rôle non reconnu après normalisation: '{}'", rawRole);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Rôle utilisateur non reconnu: " + rawRole));
        }

        log.info("🤖 Recommandation IA demandée par {} (rôle: {})", email, userRole);

        try {
            // ✅ Timeout global 100s — évite le blocage infini
            AiRecommendationResponse response = CompletableFuture
                    .supplyAsync(() -> aiRecommendationService.recommendForUser(email, userRole))
                    .get(100, TimeUnit.SECONDS);

            log.info("✅ {} services scorés pour {}", response.getRankedServices().size(), email);
            return ResponseEntity.ok(response);

        } catch (TimeoutException e) {
            // ✅ Timeout dépassé → retourner liste vide (pas 500 !)
            log.warn("⏱️ Timeout global atteint pour {} — retour liste vide", email);
            AiRecommendationResponse fallback = new AiRecommendationResponse();
            fallback.setRankedServices(Collections.emptyList());
            fallback.setGlobalExplanation(
                    "AI scoring timed out — services are shown in default order.");
            return ResponseEntity.ok(fallback); // ✅ 200, jamais 500

        } catch (Exception e) {
            // ✅ Toute autre erreur → aussi 200 avec liste vide
            log.error("❌ Erreur recommandation IA pour {}: {}", email, e.getMessage(), e);
            AiRecommendationResponse fallback = new AiRecommendationResponse();
            fallback.setRankedServices(Collections.emptyList());
            fallback.setGlobalExplanation("");
            return ResponseEntity.ok(fallback); // ✅ jamais 500
        }
    }

    private String extractRoleFromJwt(Jwt jwt) {
        try {
            String directRole = jwt.getClaimAsString("role");
            if (directRole != null && !directRole.isBlank()) return directRole.toUpperCase();

            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null) {
                @SuppressWarnings("unchecked")
                java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");
                if (roles != null) {
                    return roles.stream()
                            .filter(r -> !r.startsWith("default-roles") &&
                                    !r.equals("offline_access") &&
                                    !r.equals("uma_authorization"))
                            .findFirst()
                            .map(String::toUpperCase)
                            .orElse("UNKNOWN");
                }
            }
            return "UNKNOWN";
        } catch (Exception e) {
            log.warn("⚠️ Erreur extraction rôle JWT: {}", e.getMessage());
            return "UNKNOWN";
        }
    }

    private String normalizeRole(String rawRole) {
        if (rawRole == null) return "UNKNOWN";
        String r = rawRole.toUpperCase().trim();
        return switch (r) {
            case "INVESTOR"                                       -> "INVESTOR";
            case "ECONOMIC_PARTNER", "PARTNER",
                 "ECONOMICPARTNER", "ECONOMIC"                   -> "ECONOMIC_PARTNER";
            case "INTERNATIONAL_COMPANY", "COMPANY",
                 "INTERNATIONAL", "INTERNATIONALCOMPANY"         -> "INTERNATIONAL_COMPANY";
            case "TOURIST"                                        -> "TOURIST";
            default -> "UNKNOWN";
        };
    }
}