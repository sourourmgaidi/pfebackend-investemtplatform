package tn.iset.investplatformpfe.Controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import tn.iset.investplatformpfe.Entity.Investor;
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

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", jwt.getClaimAsString("sub"));
        userInfo.put("email", jwt.getClaimAsString("email"));
        userInfo.put("username", jwt.getClaimAsString("preferred_username"));
        userInfo.put("firstName", jwt.getClaimAsString("given_name"));
        userInfo.put("lastName", jwt.getClaimAsString("family_name"));

        @SuppressWarnings("unchecked")
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        userInfo.put("roles", realmAccess != null ? realmAccess.get("roles") : java.util.Collections.emptyList());

        return ResponseEntity.ok(userInfo);
    }

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

        // ✅ VALIDATION AMÉLIORÉE DE L'EMAIL
        if (!isValidEmail(email)) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Format d'email invalide. Utilisez un email valide (ex: nom@domaine.com)"));
        }

        // ✅ VALIDATION DU DOMAINE (OPTIONNELLE)
        if (!isAllowedDomain(email)) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Seuls les emails Gmail, Outlook, Yahoo et entreprises sont autorisés"));
        }

        if (password.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le mot de passe doit contenir au moins 6 caractères"));
        }

        // ✅ VALIDATION DU RÔLE SI FOURNI
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

    // ✅ MÉTHODE DE VALIDATION COMPLÈTE
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }

        // Regex complète pour validation email
        String emailRegex = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$";
        return email.matches(emailRegex);
    }

    // ✅ MÉTHODE POUR RESTREINDRE CERTAINS DOMAINES (OPTIONNEL)
    private boolean isAllowedDomain(String email) {
        // Extraire le domaine
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();

        // Liste des domaines autorisés
        List<String> allowedDomains = Arrays.asList(
                "gmail.com",
                "outlook.com",
                "hotmail.com",
                "yahoo.com",
                "live.com",
                "icloud.com",
                "entreprise.com", // Ajoutez vos domaines d'entreprise
                "tn", // Domaines nationaux
                "com",
                "fr"
        );

        // Vérifier si le domaine se termine par un domaine autorisé
        for (String allowedDomain : allowedDomains) {
            if (domain.endsWith(allowedDomain)) {
                return true;
            }
        }

        return false;
    }

    // ========================================
    // ENDPOINT UPDATE POUR L'UTILISATEUR CONNECTÉ
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
            String adminToken = getAdminToken();
            Map<String, Object> response = updateUser(email, userData, adminToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // ENDPOINT UPDATE POUR ADMIN (PAR EMAIL)
    // ========================================
    @PutMapping("/update/{email}")
    public ResponseEntity<?> updateUserByAdmin(
            @PathVariable String email,
            @RequestBody Map<String, Object> userData) {

        try {
            String adminToken = getAdminToken();
            Map<String, Object> response = updateUser(email, userData, adminToken);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // MÉTHODE POUR OBTENIR LE TOKEN ADMIN
    // ========================================
    private String getAdminToken() {
        String tokenUrl = authServerUrl + "/realms/master/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", "admin-cli");
        map.add("username", adminUsername);
        map.add("password", adminPassword);
        map.add("grant_type", "password");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("access_token")) {
                return (String) response.getBody().get("access_token");
            } else {
                throw new RuntimeException("Réponse invalide de Keycloak");
            }
        } catch (Exception e) {
            throw new RuntimeException("Impossible d'obtenir le token admin: " + e.getMessage());
        }
    }

    // ========================================
    // MÉTHODE PRIVÉE DE MISE À JOUR
    // ========================================
    private Map<String, Object> updateUser(String email, Map<String, Object> userData, String adminToken) {

        try {
            // 1. Récupérer l'ID de l'utilisateur dans Keycloak
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("Utilisateur non trouvé dans Keycloak");
            }

            // 2. Mettre à jour l'utilisateur dans Keycloak
            updateUserInKeycloak(userId, userData, adminToken);

            // 3. Mettre à jour l'utilisateur dans MySQL
            Investor existingInvestor = investorRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé dans la base de données"));

            // Mettre à jour les champs
            if (userData.containsKey("firstName")) {
                existingInvestor.setFirstName((String) userData.get("firstName"));
            }
            if (userData.containsKey("lastName")) {
                existingInvestor.setLastName((String) userData.get("lastName"));
            }
            if (userData.containsKey("phone")) {
                existingInvestor.setPhone((String) userData.get("phone"));
            }
            if (userData.containsKey("company")) {
                existingInvestor.setCompany((String) userData.get("company"));
            }
            if (userData.containsKey("originCountry")) {
                existingInvestor.setOriginCountry((String) userData.get("originCountry"));
            }
            if (userData.containsKey("activitySector")) {
                existingInvestor.setActivitySector((String) userData.get("activitySector"));
            }
            if (userData.containsKey("website")) {
                existingInvestor.setWebsite((String) userData.get("website"));
            }
            if (userData.containsKey("linkedinProfile")) {
                existingInvestor.setLinkedinProfile((String) userData.get("linkedinProfile"));
            }
            if (userData.containsKey("password")) {
                String newPassword = (String) userData.get("password");
                if (newPassword != null && !newPassword.isEmpty()) {
                    existingInvestor.setPassword(newPassword);
                    // Aussi mettre à jour le mot de passe dans Keycloak
                    updatePasswordInKeycloak(userId, newPassword, adminToken);
                }
            }

            // Sauvegarder dans MySQL
            Investor updatedInvestor = investorRepository.save(existingInvestor);

            // 4. Préparer la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Utilisateur mis à jour avec succès");
            response.put("email", updatedInvestor.getEmail());
            response.put("firstName", updatedInvestor.getFirstName());
            response.put("lastName", updatedInvestor.getLastName());
            response.put("role", updatedInvestor.getRole());

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour: " + e.getMessage());
        }
    }

    // ========================================
    // MÉTHODES PRIVÉES POUR KEYCLOAK
    // ========================================

    private String getUserIdByEmail(String email, String adminToken) {
        String usersUrl = authServerUrl + "/admin/realms/" + realm + "/users?email=" + email;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<Map[]> response = restTemplate.exchange(
                usersUrl,
                HttpMethod.GET,
                entity,
                Map[].class
        );

        Map[] users = response.getBody();
        if (users != null && users.length > 0) {
            return (String) users[0].get("id");
        }

        return null;
    }

    private void updateUserInKeycloak(String userId, Map<String, Object> userData, String adminToken) {
        String updateUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> updates = new HashMap<>();

        if (userData.containsKey("firstName")) {
            updates.put("firstName", userData.get("firstName"));
        }
        if (userData.containsKey("lastName")) {
            updates.put("lastName", userData.get("lastName"));
        }
        if (userData.containsKey("email")) {
            updates.put("email", userData.get("email"));
            updates.put("username", userData.get("email"));
        }

        if (!updates.isEmpty()) {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(updates, headers);
            restTemplate.exchange(updateUrl, HttpMethod.PUT, entity, String.class);
        }
    }

    private void updatePasswordInKeycloak(String userId, String newPassword, String adminToken) {
        String passwordUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> passwordData = new HashMap<>();
        passwordData.put("type", "password");
        passwordData.put("value", newPassword);
        passwordData.put("temporary", false);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(passwordData, headers);

        restTemplate.exchange(passwordUrl, HttpMethod.PUT, entity, String.class);
    }

    // ========================================
    // ENDPOINT: MOT DE PASSE OUBLIÉ
    // ========================================
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {

        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "L'email est requis"));
        }

        // Validation email
        if (!email.contains("@") || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
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
    // ENDPOINT: RÉINITIALISER LE MOT DE PASSE (OPTIONNEL)
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
}