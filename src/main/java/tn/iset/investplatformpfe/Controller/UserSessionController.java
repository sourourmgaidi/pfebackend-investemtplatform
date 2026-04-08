package tn.iset.investplatformpfe.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Dto.UserTimeStatsDTO;
import tn.iset.investplatformpfe.Service.UserSessionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
@CrossOrigin(origins = "*")
public class UserSessionController {

    // ✅ AJOUT: Logger pour remplacer les System.out.println
    private static final Logger log = LoggerFactory.getLogger(UserSessionController.class);

    private final UserSessionService sessionService;

    public UserSessionController(UserSessionService sessionService) {
        this.sessionService = sessionService;
    }

    // ========================================
    // DÉMARRER UNE SESSION
    // ========================================
    @PostMapping("/start")
    public ResponseEntity<?> startSession(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        String email = jwt.getClaimAsString("email");

        // ✅ CORRIGÉ: Le rôle dans le JWT Keycloak est dans "realm_access.roles"
        // et non dans un claim "role" direct — on récupère le rôle depuis la session
        // ou on passe une valeur par défaut si absent
        String role = jwt.getClaimAsString("role");
        if (role == null) {
            // Keycloak met les rôles dans realm_access, pas dans "role" directement
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null) {
                @SuppressWarnings("unchecked")
                java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");
                if (roles != null && !roles.isEmpty()) {
                    // Prendre le premier rôle métier (ignorer les rôles système Keycloak)
                    role = roles.stream()
                            .filter(r -> !r.startsWith("default-roles") &&
                                    !r.equals("offline_access") &&
                                    !r.equals("uma_authorization"))
                            .findFirst()
                            .orElse("UNKNOWN");
                }
            }
            if (role == null) role = "UNKNOWN";
        }

        try {
            sessionService.startSession(email, role);
            log.info("✅ Session démarrée pour {}", email);
            return ResponseEntity.ok(Map.of("success", true, "message", "Session démarrée"));
        } catch (Exception e) {
            log.error("Erreur démarrage session pour {}: {}", email, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors du démarrage de la session"));
        }
    }

    // ========================================
    // TERMINER UNE SESSION
    // ========================================
    @PostMapping("/end")
    public ResponseEntity<?> endSession(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        String email = jwt.getClaimAsString("email");

        // ✅ CORRIGÉ: Résultat ignoré silencieusement avant — maintenant on log
        try {
            sessionService.endSession(email);
            log.info("✅ Session terminée pour {}", email);
            return ResponseEntity.ok(Map.of("success", true, "message", "Session terminée"));
        } catch (Exception e) {
            log.error("Erreur fermeture session pour {}: {}", email, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors de la fermeture de la session"));
        }
    }

    // ========================================
    // OBTENIR SES PROPRES STATISTIQUES
    // ========================================
    @GetMapping("/my-stats")
    public ResponseEntity<?> getMyStats(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        String email = jwt.getClaimAsString("email");

        try {
            UserTimeStatsDTO stats = sessionService.getUserStats(email);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Erreur récupération stats pour {}: {}", email, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors de la récupération des statistiques"));
        }
    }

    // ========================================
    // OBTENIR LES STATISTIQUES D'UN UTILISATEUR (ADMIN)
    // ========================================
    @GetMapping("/user/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserStats(@PathVariable String email) {
        try {
            UserTimeStatsDTO stats = sessionService.getUserStats(email);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Erreur récupération stats admin pour {}: {}", email, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors de la récupération des statistiques"));
        }
    }

    // ========================================
    // OBTENIR TOUS LES UTILISATEURS (ADMIN)
    // ========================================
    @GetMapping("/all-users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsersStats() {
        try {
            List<UserTimeStatsDTO> stats = sessionService.getAllUsersStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Erreur récupération stats tous utilisateurs: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur lors de la récupération des statistiques"));
        }
    }
    // ========================================
// TERMINER UNE SESSION PAR EMAIL (sans token — pour expiration)
// ========================================
    @PostMapping("/end-by-email")
    public ResponseEntity<?> endSessionByEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email requis"));
        }

        try {
            sessionService.endSession(email);
            log.info("✅ Session fermée par expiration token pour {}", email);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            log.error("Erreur fermeture session expirée pour {}: {}", email, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Erreur fermeture session"));
        }
    }
}