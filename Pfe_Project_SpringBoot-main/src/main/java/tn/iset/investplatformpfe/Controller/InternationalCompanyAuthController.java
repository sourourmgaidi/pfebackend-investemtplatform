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
    // INSCRIPTION
    // ========================================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> userData) {

        // Vérifier les champs obligatoires
        String[] requiredFields = {
                "email", "password", "companyName",
                "contactLastName", "contactFirstName", "phone",
                "originCountry", "activitySector", "siret", "interetPrincipal"
        };

        for (String field : requiredFields) {
            if (!userData.containsKey(field) || userData.get(field) == null) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Le champ '" + field + "' est requis")
                );
            }
        }

        try {
            Map<String, Object> response = authService.register(userData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Erreur lors de l'inscription: " + e.getMessage())
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
                    Map.of("error", "Email et mot de passe requis")
            );
        }

        try {
            Map<String, Object> response = authService.login(email, password);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(
                    Map.of("error", "Authentification échouée: " + e.getMessage())
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
                    Map.of("error", "Refresh token requis")
            );
        }

        try {
            Map<String, Object> response = authService.refreshToken(refreshToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body(
                    Map.of("error", "Rafraîchissement échoué: " + e.getMessage())
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
                    Map.of("error", "Refresh token requis")
            );
        }

        try {
            authService.logout(refreshToken);
            return ResponseEntity.ok(Map.of("message", "Déconnexion réussie"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Déconnexion échouée: " + e.getMessage())
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
                    Map.of("error", "Non authentifié")
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
                    Map.of("error", "Non authentifié")
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
                    Map.of("error", "L'email est requis")
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
                    Map.of("error", "L'email est requis")
            );
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Le nouveau mot de passe est requis")
            );
        }

        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Le mot de passe doit contenir au moins 6 caractères")
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
        // Cette méthode est optionnelle, à implémenter si nécessaire
        return ResponseEntity.ok(Map.of("message", "Fonctionnalité à implémenter"));
    }
}
