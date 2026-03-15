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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final InvestorRepository investorRepository;
    private final RestTemplate restTemplate;

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin-username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin-password:admin}")
    private String adminPassword;

    public AuthController(AuthService authService, InvestorRepository investorRepository) {
        this.authService = authService;
        this.investorRepository = investorRepository;
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
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentification échouée: " + e.getMessage()));
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
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Refresh token requis"));
        }

        try {
            authService.logout(refreshToken);
            return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Déconnexion échouée: " + e.getMessage()));
        }
    }

    // ========================================
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