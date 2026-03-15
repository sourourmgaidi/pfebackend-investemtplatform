package tn.iset.investplatformpfe.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Service.InternationalCompanyAuthService;

import java.util.Map;

@RestController
@RequestMapping("/api/international-companies")
public class InternationalCompanyAuthController {

    private final InternationalCompanyAuthService authService;

    public InternationalCompanyAuthController(InternationalCompanyAuthService authService) {
        this.authService = authService;
    }

    // ========================================
    // INSCRIPTION - CORRIGÉE (sans interetPrincipal)
    // ========================================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> userData) {

        // ✅ Vérifier les champs obligatoires (SANS interetPrincipal)
        String[] requiredFields = {
                "email", "password", "companyName",
                "contactLastName", "contactFirstName", "phone",
                "originCountry", "activitySector", "siret"
        };

        for (String field : requiredFields) {
            if (!userData.containsKey(field) || userData.get(field) == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Field '" + field + "' is required")
                );
            }
        }

        try {
            Map<String, Object> response = authService.register(userData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Registration error: " + e.getMessage())
            );
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
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Email and password are required")
            );
        }

        try {
            Map<String, Object> response = authService.login(email, password);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(
                    Map.of("error", "Authentication failed: " + e.getMessage())
            );
        }
    }

    // ========================================
    // RAFRAÎCHIR LE TOKEN
    // ========================================
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> request) {

        String refreshToken = request.get("refreshToken");

        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Refresh token is required")
            );
        }

        try {
            Map<String, Object> response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(
                    Map.of("error", "Refresh failed: " + e.getMessage())
            );
        }
    }

    // ========================================
    // DÉCONNEXION
    // ========================================
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {

        String refreshToken = request.get("refreshToken");

        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Refresh token is required")
            );
        }

        try {
            authService.logout(refreshToken);
            return ResponseEntity.ok(Map.of("message", "Logout successful"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Logout failed: " + e.getMessage())
            );
        }
    }

    // ========================================
    // RÉCUPÉRER LE PROFIL
    // ========================================
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@AuthenticationPrincipal Jwt jwt) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(
                    Map.of("error", "Not authenticated")
            );
        }

        String email = jwt.getClaimAsString("email");

        try {
            Map<String, Object> profile = authService.getProfile(email);
            return ResponseEntity.ok(profile);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    // ========================================
    // METTRE À JOUR LE PROFIL
    // ========================================
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, Object> userData) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(
                    Map.of("error", "Not authenticated")
            );
        }

        String email = jwt.getClaimAsString("email");

        try {
            Map<String, Object> response = authService.updateProfile(email, userData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    // ========================================
    // MOT DE PASSE OUBLIÉ
    // ========================================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {

        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Email is required")
            );
        }

        try {
            Map<String, Object> response = authService.forgotPassword(email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
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
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Email is required")
            );
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "New password is required")
            );
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Password must be at least 6 characters")
            );
        }

        try {
            Map<String, Object> response = authService.resetPassword(email, newPassword);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    // ========================================
    // RÉCUPÉRER UNE ENTREPRISE PAR ID (optionnel)
    // ========================================
    @GetMapping("/{id}")
    public ResponseEntity<?> getCompanyById(@PathVariable Long id) {
        return ResponseEntity.ok(Map.of("message", "Functionality to be implemented"));
    }
    @DeleteMapping("/delete-account")
    public ResponseEntity<?> deleteAccount(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, String> request) {

        // Vérifier que l'utilisateur est authentifié
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        // Récupérer l'email depuis le token JWT
        String email = jwt.getClaimAsString("email");

        // Récupérer le mot de passe depuis le corps de la requête
        String password = request.get("password");

        // Vérifier que le mot de passe est fourni
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

        // Vérifier que l'utilisateur est authentifié
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
    // CHANGER LE MOT DE PASSE (SOCIÉTÉ INTERNATIONALE CONNECTÉE)
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