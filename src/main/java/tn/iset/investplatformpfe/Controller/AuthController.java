package tn.iset.investplatformpfe.Controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Repository.InvestorRepository;
import tn.iset.investplatformpfe.Service.AuthService;
import tn.iset.investplatformpfe.Service.UserSessionService;

import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final InvestorRepository investorRepository;
    private final RestTemplate restTemplate;
    private final UserSessionService sessionService;

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin-username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin-password:admin}")
    private String adminPassword;


    public AuthController(AuthService authService, InvestorRepository investorRepository, UserSessionService sessionService) {
        this.authService = authService;
        this.investorRepository = investorRepository;
        this.sessionService = sessionService;
        this.restTemplate = new RestTemplate();
    }

    // ========================================
    // INSCRIPTION
    // ========================================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> userData) {

        String[] requiredFields = {"email", "password", "firstName", "lastName"};
        for (String field : requiredFields) {
            if (!userData.containsKey(field) || userData.get(field) == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le champ '" + field + "' est requis"));
            }
            if (userData.get(field) instanceof String && ((String) userData.get(field)).trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Le champ '" + field + "' ne peut pas être vide"));
            }
        }

        String email = (String) userData.get("email");
        String password = (String) userData.get("password");

        // Validation de l'email
        if (!isValidEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Format d'email invalide. Utilisez un email valide (ex: nom@domaine.com)"));
        }

        // Validation du mot de passe
        if (password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le mot de passe doit contenir au moins 6 caractères"));
        }

        // Validation du rôle si fourni
        if (userData.containsKey("role") && userData.get("role") != null) {
            String roleStr = (String) userData.get("role");
            try {
                Role.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error",
                        "Rôle invalide. Rôles acceptés: TOURIST, INVESTOR, PARTNER, LOCAL_PARTNER, INTERNATIONAL_COMPANY, ADMIN"));
            }
        }

        try {
            Map<String, Object> response = authService.register(userData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Erreur lors de l'inscription: " + e.getMessage()));
        }
    }

    // ========================================
    // CONNEXION
    // ========================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email et mot de passe requis"));
        }

        try {
            Map<String, Object> response = authService.login(email, password);

            // ✅ RÉCUPÉRER LE JWT ET EXTRAIRE LE RÔLE
            String accessToken = (String) response.get("access_token");
            if (accessToken != null) {
                String role = extractRoleFromJwt(accessToken);
                authService.startSessionAfterLogin(email, role);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentification échouée: " + e.getMessage()));
        }
    }

    // ✅ MÉTHODE CORRIGÉE SANS JACKSON
    private String extractRoleFromJwt(String token) {
        try {
            // Séparer les parties du JWT
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                return "USER";
            }

            // Décoder le payload (partie 2)
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

            // Chercher "realm_access" dans le JSON
            int realmAccessIndex = payload.indexOf("\"realm_access\"");
            if (realmAccessIndex == -1) return "USER";

            // Chercher "roles"
            int rolesIndex = payload.indexOf("\"roles\"", realmAccessIndex);
            if (rolesIndex == -1) return "USER";

            // Chercher le tableau des rôles
            int startBracket = payload.indexOf("[", rolesIndex);
            int endBracket = payload.indexOf("]", startBracket);

            if (startBracket == -1 || endBracket == -1) return "USER";

            String rolesSection = payload.substring(startBracket + 1, endBracket);

            // Extraire les rôles
            String[] roleMatches = rolesSection.split(",");

            for (String roleMatch : roleMatches) {
                String cleanRole = roleMatch.trim().replaceAll("\"", "");

                // Filtrer les rôles système Keycloak
                if (!cleanRole.startsWith("default-roles-") &&
                        !cleanRole.equals("offline_access") &&
                        !cleanRole.equals("uma_authorization")) {
                    return cleanRole;
                }
            }

            return "USER";

        } catch (Exception e) {
            System.err.println("Erreur extraction rôle: " + e.getMessage());
            return "USER";
        }
    }
    // ========================================
    // PROFIL UTILISATEUR CONNECTÉ
    // ========================================
    // ========================================
// PROFIL UTILISATEUR CONNECTÉ (COMPLET)
// ========================================
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        String email = jwt.getClaimAsString("email");

        try {
            // ✅ Utiliser la nouvelle méthode getProfile du service
            Map<String, Object> profile = authService.getProfile(email);

            // Ajouter les rôles du token JWT
            @SuppressWarnings("unchecked")
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            profile.put("roles", realmAccess != null ? realmAccess.get("roles") : List.of());

            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
    // ========================================
    // METTRE À JOUR LE PROFIL (UTILISATEUR CONNECTÉ)
    // ========================================
    @PutMapping("/update")
    public ResponseEntity<?> updateCurrentUser(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, Object> userData) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        String email = jwt.getClaimAsString("email");

        try {
            Map<String, Object> response = authService.updateProfile(email, userData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // RAFRAÎCHIR LE TOKEN
    // ========================================
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Refresh token requis"));
        }

        try {
            Map<String, Object> response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Rafraîchissement échoué: " + e.getMessage()));
        }
    }

    // ========================================
    // DÉCONNEXION
    // ========================================
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal Jwt jwt,
                                    @RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Refresh token requis"));
        }

        // 1. Email depuis le JWT header (Authorization: Bearer ...)
        String email = null;
        if (jwt != null) {
            email = jwt.getClaimAsString("email");
            System.out.println("📧 Email depuis JWT: " + email);
        }

        // 2. Fallback : email depuis le body
        if (email == null) {
            email = request.get("email");
            if (email != null) System.out.println("📧 Email depuis body: " + email);
        }

        // 3. ✅ NOUVEAU : extraire le sub du refreshToken → appel Keycloak → email
        if (email == null) {
            String sub = extractSubFromToken(refreshToken);
            System.out.println("🔑 sub extrait du refreshToken: " + sub);
            if (sub != null) {
                email = authService.findEmailByKeycloakSub(sub);
                System.out.println("📧 Email trouvé via Keycloak: " + email);
            }
        }

        try {
            // Déconnexion Keycloak
            authService.logout(refreshToken);

            // Terminer la session locale
            if (email != null) {
                authService.endSession(email);
                System.out.println("✅ Session terminée pour " + email);
            } else {
                System.out.println("⚠️ Email introuvable, session non terminée");
            }

            return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Déconnexion échouée: " + e.getMessage()));
        }
    }
    private String extractSubFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;

            String payload = parts[1];
            int mod = payload.length() % 4;
            if (mod != 0) payload += "=".repeat(4 - mod);

            String decoded = new String(
                    Base64.getUrlDecoder().decode(payload),
                    StandardCharsets.UTF_8
            );
            System.out.println("🔍 Payload refreshToken décodé: " + decoded);

            // Extraire "sub"
            if (decoded.contains("\"sub\"")) {
                int idx    = decoded.indexOf("\"sub\"");
                int colon  = decoded.indexOf(":", idx);
                int startQ = decoded.indexOf("\"", colon);
                int endQ   = decoded.indexOf("\"", startQ + 1);
                if (startQ != -1 && endQ != -1) {
                    return decoded.substring(startQ + 1, endQ);
                }
            }
            return null;
        } catch (Exception e) {
            System.err.println("⚠️ Erreur extraction sub: " + e.getMessage());
            return null;
        }
    }
    // MOT DE PASSE OUBLIÉ
    // ========================================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "L'email est requis"));
        }

        if (!isValidEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Format d'email invalide"));
        }

        try {
            Map<String, Object> response = authService.forgotPassword(email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // RÉINITIALISER LE MOT DE PASSE
    // ========================================
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String newPassword = request.get("newPassword");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "L'email est requis"));
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le nouveau mot de passe est requis"));
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le mot de passe doit contenir au moins 6 caractères"));
        }

        try {
            Map<String, Object> response = authService.resetPassword(email, newPassword);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // MÉTHODES DE VALIDATION
    // ========================================
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
        return email.matches(emailRegex);
    }

    private boolean isAllowedDomain(String email) {
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        List<String> allowedDomains = Arrays.asList(
                "gmail.com", "outlook.com", "hotmail.com", "yahoo.com",
                "live.com", "icloud.com", "tn", "com", "fr"
        );
        for (String allowedDomain : allowedDomains) {
            if (domain.endsWith(allowedDomain)) {
                return true;
            }
        }
        return false;
    }
    // SUPPRESSION DE COMPTE PAR L'UTILISATEUR (AVEC MOT DE PASSE)
    // ========================================
    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> request) {

        // Vérifier que l'utilisateur est authentifié
        if (jwt == null) {
            return ResponseEntity.status(401).body(
                    Map.of("error", "Non authentifié")
            );
        }

        // Récupérer l'email depuis le token JWT
        String email = jwt.getClaimAsString("email");

        // Récupérer le mot de passe depuis le corps de la requête
        String password = request.get("password");

        // Vérifier que le mot de passe est fourni
        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Le mot de passe est requis pour confirmer la suppression")
            );
        }

        try {
            // Appeler le service pour supprimer le compte
            Map<String, Object> response = authService.deleteAccount(email, password);

            // Retourner la réponse de succès
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // En cas d'erreur, retourner le message d'erreur
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    // ========================================
    // SUPPRESSION DE COMPTE PAR L'ADMIN (SANS MOT DE PASSE)
    // ========================================
    @DeleteMapping("/admin/delete-account/{email}")
    public ResponseEntity<?> deleteAccountByAdmin(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String email) {

        // Vérifier que l'utilisateur est authentifié
        if (jwt == null) {
            return ResponseEntity.status(401).body(
                    Map.of("error", "Non authentifié")
            );
        }

        // Vérifier que l'utilisateur connecté a le rôle ADMIN
        @SuppressWarnings("unchecked")
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");
            if (roles == null || !roles.contains("ADMIN")) {
                return ResponseEntity.status(403).body(
                        Map.of("error", "Accès non autorisé. Seul un administrateur peut supprimer un compte sans mot de passe.")
                );
            }
        } else {
            return ResponseEntity.status(403).body(
                    Map.of("error", "Accès non autorisé")
            );
        }

        try {
            // Appeler le service pour supprimer le compte (version admin)
            Map<String, Object> response = authService.deleteAccountByAdmin(email);

            // Retourner la réponse de succès
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // En cas d'erreur, retourner le message d'erreur
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }
    // CHANGER LE MOT DE PASSE (INVESTOR CONNECTÉ)
// ========================================
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> request) {

        // Vérifier que l'utilisateur est authentifié
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        String email = jwt.getClaimAsString("email");
        String oldPassword = request.get("oldPassword");
        String newPassword = request.get("newPassword");

        // Vérifier que les mots de passe sont fournis
        if (oldPassword == null || oldPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "L'ancien mot de passe est requis"));
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le nouveau mot de passe est requis"));
        }

        // Valider la longueur du nouveau mot de passe
        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le nouveau mot de passe doit contenir au moins 6 caractères"));
        }

        try {
            Map<String, Object> response = authService.changePassword(email, oldPassword, newPassword);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}