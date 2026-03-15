package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tn.iset.investplatformpfe.Entity.Investor;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Repository.InvestorRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    @Value("${keycloak.admin-username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin-password:admin}")
    private String adminPassword;

    private final InvestorRepository investorRepository;
    private final RestTemplate restTemplate;

    public AuthService(InvestorRepository investorRepository) {
        this.investorRepository = investorRepository;
        this.restTemplate = new RestTemplate();
    }

    // ========================================
    // INSCRIPTION
    // ========================================
    public Map<String, Object> register(Map<String, Object> userData) {

        String email = (String) userData.get("email");
        String password = (String) userData.get("password");
        String firstName = (String) userData.get("firstName");
        String lastName = (String) userData.get("lastName");

        // Récupérer le rôle depuis la requête (avec valeur par défaut INVESTOR)
        String roleStr = (String) userData.get("role");
        Role role = Role.INVESTOR; // Valeur par défaut

        if (roleStr != null && !roleStr.isEmpty()) {
            try {
                role = Role.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Rôle invalide: " + roleStr);
            }
        }

        // 1. Vérifier si l'email existe déjà dans MySQL
        if (investorRepository.existsByEmail(email)) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        try {
            // 2. Créer l'utilisateur dans Keycloak
            String userId = createUserInKeycloak(email, password, firstName, lastName);

            // 3. Assigner le rôle dans Keycloak
            assignRoleToUser(userId, role.name());

            // 4. Créer dans MySQL avec le rôle spécifié
            Investor newInvestor = new Investor();
            newInvestor.setEmail(email);
            newInvestor.setPassword(password);
            newInvestor.setFirstName(firstName);
            newInvestor.setLastName(lastName);
            newInvestor.setActive(true);
            newInvestor.setRole(role);
            newInvestor.setRegistrationDate(LocalDateTime.now());

            // Champs optionnels
            if (userData.containsKey("phone")) newInvestor.setPhone((String) userData.get("phone"));
            if (userData.containsKey("company")) newInvestor.setCompany((String) userData.get("company"));
            if (userData.containsKey("originCountry")) newInvestor.setOriginCountry((String) userData.get("originCountry"));
            if (userData.containsKey("activitySector")) newInvestor.setActivitySector((String) userData.get("activitySector"));
            if (userData.containsKey("website")) newInvestor.setWebsite((String) userData.get("website"));
            if (userData.containsKey("linkedinProfile")) newInvestor.setLinkedinProfile((String) userData.get("linkedinProfile"));

            Investor savedInvestor = investorRepository.save(newInvestor);

            // 5. Réponse
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Inscription réussie");
            response.put("id", savedInvestor.getId());
            response.put("email", savedInvestor.getEmail());
            response.put("firstName", savedInvestor.getFirstName());
            response.put("lastName", savedInvestor.getLastName());
            response.put("role", savedInvestor.getRole());

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'inscription: " + e.getMessage());
        }
    }

    // ========================================
    // CONNEXION
    // ========================================
    public Map<String, Object> login(String email, String password) {
        String tokenUrl = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("grant_type", "password");
        map.add("username", email);
        map.add("password", password);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Erreur d'authentification: " + e.getMessage());
        }
    }

    // ========================================
    // RAFRAÎCHIR LE TOKEN
    // ========================================
    public Map<String, Object> refreshToken(String refreshToken) {
        String tokenUrl = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("grant_type", "refresh_token");
        map.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Erreur de rafraîchissement du token: " + e.getMessage());
        }
    }

    // ========================================
    // DÉCONNEXION
    // ========================================
    public void logout(String refreshToken) {
        String logoutUrl = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("refresh_token", refreshToken);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        try {
            restTemplate.postForEntity(logoutUrl, entity, String.class);
        } catch (Exception e) {
            throw new RuntimeException("Erreur de déconnexion: " + e.getMessage());
        }
    }

    // ========================================
    // MÉTHODES PRIVÉES POUR KEYCLOAK
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

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                request,
                Map.class
        );

        return (String) response.getBody().get("access_token");
    }

    private String createUserInKeycloak(String email, String password, String firstName, String lastName) {
        String createUserUrl = authServerUrl + "/admin/realms/" + realm + "/users";

        String adminToken = getAdminToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> user = new HashMap<>();
        user.put("username", email);
        user.put("email", email);
        user.put("firstName", firstName);
        user.put("lastName", lastName);
        user.put("enabled", true);
        user.put("emailVerified", true);

        Map<String, Object> credentials = new HashMap<>();
        credentials.put("type", "password");
        credentials.put("value", password);
        credentials.put("temporary", false);

        user.put("credentials", new Map[]{credentials});

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(user, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(createUserUrl, request, String.class);

        String location = response.getHeaders().getLocation().toString();
        return location.substring(location.lastIndexOf("/") + 1);
    }

    private void assignRoleToUser(String userId, String roleName) {
        String adminToken = getAdminToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 1. Récupérer l'ID du rôle
        String rolesUrl = authServerUrl + "/admin/realms/" + realm + "/roles";

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<Map[]> rolesResponse = restTemplate.exchange(
                rolesUrl,
                HttpMethod.GET,
                entity,
                Map[].class
        );

        // 2. Trouver le rôle par son nom
        String roleId = null;
        for (Map role : rolesResponse.getBody()) {
            if (roleName.equals(role.get("name"))) {
                roleId = (String) role.get("id");
                break;
            }
        }

        if (roleId == null) {
            throw new RuntimeException("Rôle " + roleName + " non trouvé dans Keycloak");
        }

        // 3. Assigner le rôle à l'utilisateur
        String assignUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";

        Map<String, Object> roleMapping = new HashMap<>();
        roleMapping.put("id", roleId);
        roleMapping.put("name", roleName);

        HttpEntity<Map[]> assignEntity = new HttpEntity<>(new Map[]{roleMapping}, headers);

        restTemplate.exchange(assignUrl, HttpMethod.POST, assignEntity, String.class);

        System.out.println("Rôle " + roleName + " assigné à l'utilisateur " + userId);
    }
    // ========================================
// MOT DE PASSE OUBLIÉ - ENVOI D'EMAIL
// ========================================
    public Map<String, Object> forgotPassword(String email) {

        // 1. Vérifier si l'email existe dans la base de données
        Investor investor = investorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Aucun compte trouvé avec cet email"));

        try {
            // 2. Obtenir un token admin
            String adminToken = getAdminToken();

            // 3. Récupérer l'ID de l'utilisateur dans Keycloak
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("Utilisateur non trouvé dans Keycloak");
            }

            // 4. Envoyer un email de réinitialisation via Keycloak
            sendResetPasswordEmail(userId, adminToken);

            // 5. Préparer la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Un email de réinitialisation a été envoyé à " + email);
            response.put("email", email);

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la demande: " + e.getMessage());
        }
    }

    // ========================================
// ENVOYER L'EMAIL DE RÉINITIALISATION
// ========================================
    // ========================================
// ENVOYER L'EMAIL DE RÉINITIALISATION (CORRIGÉ)
// ========================================
    private void sendResetPasswordEmail(String userId, String adminToken) {
        String emailUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/execute-actions-email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        // Liste des actions requises (UPDATE_PASSWORD = réinitialiser le mot de passe)
        List<String> requiredActions = List.of("UPDATE_PASSWORD");

        // URL de redirection après réinitialisation
        String redirectUri = "http://localhost:4200/reset-password-complete";

        // ✅ AJOUTER LE CLIENT_ID COMME PARAMÈTRE
        String urlWithParams = emailUrl + "?client_id=" + clientId + "&redirect_uri=" + redirectUri;

        HttpEntity<List<String>> entity = new HttpEntity<>(requiredActions, headers);

        try {
            restTemplate.exchange(
                    urlWithParams,
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            System.out.println("Email de réinitialisation envoyé à l'utilisateur: " + userId);

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
    }

    // ========================================
// RÉINITIALISER LE MOT DE PASSE DIRECTEMENT (OPTION ADMIN)
// ========================================
    public Map<String, Object> resetPassword(String email, String newPassword) {

        // Validation du mot de passe
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Le mot de passe doit contenir au moins 6 caractères");
        }

        try {
            // 1. Obtenir un token admin
            String adminToken = getAdminToken();

            // 2. Récupérer l'ID de l'utilisateur dans Keycloak
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("Utilisateur non trouvé dans Keycloak");
            }

            // 3. Réinitialiser le mot de passe dans Keycloak
            updatePasswordInKeycloak(userId, newPassword, adminToken);

            // 4. Mettre à jour le mot de passe dans MySQL
            Investor investor = investorRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé dans la base de données"));
            investor.setPassword(newPassword);
            investorRepository.save(investor);

            // 5. Préparer la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Mot de passe réinitialisé avec succès");
            response.put("email", email);

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la réinitialisation: " + e.getMessage());
        }
    }

    // ========================================
// MÉTHODE POUR METTRE À JOUR LE MOT DE PASSE DANS KEYCLOAK
// ========================================
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

        try {
            restTemplate.exchange(passwordUrl, HttpMethod.PUT, entity, String.class);
            System.out.println("Mot de passe mis à jour pour l'utilisateur: " + userId);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour du mot de passe: " + e.getMessage());
        }
    }

    // ========================================
// MÉTHODE POUR RÉCUPÉRER L'ID UTILISATEUR PAR EMAIL
// ========================================
    private String getUserIdByEmail(String email, String adminToken) {
        String usersUrl = authServerUrl + "/admin/realms/" + realm + "/users?email=" + email;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
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

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la recherche de l'utilisateur: " + e.getMessage());
        }
    }
}