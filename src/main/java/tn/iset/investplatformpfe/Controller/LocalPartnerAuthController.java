package tn.iset.investplatformpfe.Controller;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Service.LocalPartnerAuthService;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/partenaires-locaux")  // Keep the original mapping
public class LocalPartnerAuthController {
    private final LocalPartnerAuthService authService;

    public LocalPartnerAuthController(LocalPartnerAuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> userData) {

        // ✅ CORRIGÉ : utiliser les noms anglais
        String[] requiredFields = {"email", "password", "lastName", "firstName"};

        for (String field : requiredFields) {
            if (!userData.containsKey(field) || userData.get(field) == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Field '" + field + "' is required"));
            }
        }

        try {
            Map<String, Object> response = authService.register(userData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        if (email == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required"));
        }

        try {
            Map<String, Object> response = authService.login(email, password);

            // ✅ DÉMARRER LA SESSION APRÈS LOGIN
            String accessToken = (String) response.get("access_token");
            if (accessToken != null) {
                String role = extractRoleFromJwt(accessToken);
                authService.startSessionAfterLogin(email, role);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }
    private String extractRoleFromJwt(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) return "USER";

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

            int realmAccessIndex = payload.indexOf("\"realm_access\"");
            if (realmAccessIndex == -1) return "USER";

            int rolesIndex = payload.indexOf("\"roles\"", realmAccessIndex);
            if (rolesIndex == -1) return "USER";

            int startBracket = payload.indexOf("[", rolesIndex);
            int endBracket = payload.indexOf("]", startBracket);

            if (startBracket == -1 || endBracket == -1) return "USER";

            String rolesSection = payload.substring(startBracket + 1, endBracket);
            String[] roleMatches = rolesSection.split(",");

            for (String roleMatch : roleMatches) {
                String cleanRole = roleMatch.trim().replaceAll("\"", "");
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
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Refresh token is required"));
        }

        try {
            Map<String, Object> response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Refresh failed: " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal Jwt jwt,
                                    @RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Refresh token is required"));
        }

        // 1. Email depuis le JWT
        String email = null;
        if (jwt != null) {
            email = jwt.getClaimAsString("email");
        }

        // 2. Fallback : email depuis le body
        if (email == null) {
            email = request.get("email");
        }

        try {
            authService.logout(refreshToken);

            // ✅ TERMINER LA SESSION
            if (email != null) {
                authService.endSession(email);
                System.out.println("✅ Session terminée pour " + email);
            } else {
                System.out.println("⚠️ Email introuvable, session non terminée");
            }

            return ResponseEntity.ok(Map.of("message", "Logout successful"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Logout failed: " + e.getMessage()));
        }
    }
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String email = jwt.getClaimAsString("email");

        try {
            Map<String, Object> profile = authService.getProfile(email);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, Object> userData) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
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
    // ENDPOINT: FORGOT PASSWORD
    // ========================================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {

        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        // Simple email validation
        if (!email.contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid email format"));
        }

        try {
            Map<String, Object> response = authService.forgotPassword(email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // ENDPOINT: RESET PASSWORD
    // ========================================
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {

        String email = request.get("email");
        String newPassword = request.get("newPassword");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required"));
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "New password is required"));
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password must contain at least 6 characters"));
        }

        try {
            Map<String, Object> response = authService.resetPassword(email, newPassword);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> request) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        String email = jwt.getClaimAsString("email");
        String password = request.get("password");

        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password is required to confirm deletion"));
        }

        try {
            Map<String, Object> response = authService.deleteAccount(email, password);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // ✅ NOUVEAU: SUPPRESSION DE COMPTE PAR L'ADMIN (SANS MOT DE PASSE)
    // ========================================
    @DeleteMapping("/admin/delete-account/{email}")
    public ResponseEntity<?> deleteAccountByAdmin(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String email) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        // Vérifier que l'utilisateur connecté a le rôle ADMIN
        @SuppressWarnings("unchecked")
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            java.util.List<String> roles = (java.util.List<String>) realmAccess.get("roles");
            if (roles == null || !roles.contains("ADMIN")) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied. Only ADMIN can delete accounts without password."));
            }
        } else {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        try {
            Map<String, Object> response = authService.deleteAccountByAdmin(email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    // CHANGER LE MOT DE PASSE (PARTENAIRE LOCAL CONNECTÉ)
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
