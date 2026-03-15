package tn.iset.investplatformpfe.Service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tn.iset.investplatformpfe.Entity.ActivityDomain;
import tn.iset.investplatformpfe.Entity.Investor;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Repository.InvestorRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
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
    // VALIDATION GMAIL
    // ========================================
    private boolean isGmail(String email) {
        if (email == null) return false;

        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();

        // Liste des domaines Gmail acceptés
        List<String> gmailDomains = Arrays.asList(
                "gmail.com",
                "googlemail.com",
                "gmail.co.uk",
                "gmail.fr",
                "gmail.de",
                "gmail.it",
                "gmail.es",
                "gmail.ca",
                "gmail.com.au",
                "gmail.co.in"
        );

        return gmailDomains.contains(domain);
    }

    // ========================================
    // INSCRIPTION - AVEC VALIDATION GMAIL
    // ========================================
    // ========================================
// INSCRIPTION - AVEC VALIDATION GMAIL
// ========================================
    public Map<String, Object> register(Map<String, Object> userData) {

        String email = (String) userData.get("email");
        String password = (String) userData.get("password");
        String firstName = (String) userData.get("firstName");
        String lastName = (String) userData.get("lastName");

        // ✅ VALIDATION GMAIL
        if (!isGmail(email)) {
            throw new RuntimeException("Seules les adresses Gmail sont autorisées. Veuillez utiliser une adresse Gmail valide (ex: @gmail.com, @gmail.fr, etc.)");
        }

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

            // Champs optionnels avec conversion appropriée
            if (userData.containsKey("phone") && userData.get("phone") != null) {
                newInvestor.setPhone((String) userData.get("phone"));
            }

            if (userData.containsKey("company") && userData.get("company") != null) {
                newInvestor.setCompany((String) userData.get("company"));
            }

            if (userData.containsKey("originCountry") && userData.get("originCountry") != null) {
                newInvestor.setOriginCountry((String) userData.get("originCountry"));
            }

            // ✅ NOUVEAU: Nationality
            if (userData.containsKey("nationality") && userData.get("nationality") != null) {
                newInvestor.setNationality((String) userData.get("nationality"));
            }

            // ✅ CORRECTION : Convertir String en ActivityDomain
            if (userData.containsKey("activitySector") && userData.get("activitySector") != null) {
                String sectorStr = (String) userData.get("activitySector");
                try {
                    ActivityDomain activitySector = ActivityDomain.valueOf(sectorStr.toUpperCase());
                    newInvestor.setActivitySector(activitySector);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Secteur d'activité invalide: " + sectorStr);
                }
            }

            if (userData.containsKey("website") && userData.get("website") != null) {
                newInvestor.setWebsite((String) userData.get("website"));
            }

            if (userData.containsKey("linkedinProfile") && userData.get("linkedinProfile") != null) {
                newInvestor.setLinkedinProfile((String) userData.get("linkedinProfile"));
            }

            if (userData.containsKey("profilePicture") && userData.get("profilePicture") != null) {
                newInvestor.setProfilePicture((String) userData.get("profilePicture"));
            }

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
    // METTRE À JOUR LE PROFIL - AVEC VALIDATION GMAIL
    // ========================================
    // ========================================
// METTRE À JOUR LE PROFIL - AVEC VALIDATION GMAIL
// ========================================
    @Transactional
    public Map<String, Object> updateProfile(String email, Map<String, Object> userData) {

        Investor existing = investorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        try {
            String adminToken = getAdminToken();
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("Utilisateur non trouvé dans Keycloak");
            }

            Map<String, Object> keycloakUpdates = new HashMap<>();
            boolean emailChanged = false;
            String newEmail = null;

            // ✅ 1. COLLECTER LES MODIFICATIONS SANS EMAIL
            if (userData.containsKey("firstName")) {
                String newFirstName = (String) userData.get("firstName");
                existing.setFirstName(newFirstName);
                keycloakUpdates.put("firstName", newFirstName);
            }

            if (userData.containsKey("lastName")) {
                String newLastName = (String) userData.get("lastName");
                existing.setLastName(newLastName);
                keycloakUpdates.put("lastName", newLastName);
            }

            // ✅ 2. GÉRER L'EMAIL SÉPARÉMENT AVEC VALIDATION GMAIL
            if (userData.containsKey("email")) {
                newEmail = (String) userData.get("email");

                if (!newEmail.equals(existing.getEmail())) {
                    // ✅ VALIDATION GMAIL POUR LE NOUVEL EMAIL
                    if (!isGmail(newEmail)) {
                        throw new RuntimeException("Le nouvel email doit être une adresse Gmail valide");
                    }

                    // Vérifier que le nouvel email n'est pas déjà utilisé
                    if (investorRepository.existsByEmail(newEmail)) {
                        throw new RuntimeException("Cet email est déjà utilisé: " + newEmail);
                    }
                    emailChanged = true;
                }
            }

            // ✅ 3. AUTRES CHAMPS (PAS DANS KEYCLOAK)
            if (userData.containsKey("phone")) {
                existing.setPhone((String) userData.get("phone"));
            }

            if (userData.containsKey("company")) {
                existing.setCompany((String) userData.get("company"));
            }

            if (userData.containsKey("originCountry")) {
                existing.setOriginCountry((String) userData.get("originCountry"));
            }

            // ✅ NOUVEAU: Mise à jour de la nationalité
            if (userData.containsKey("nationality")) {
                existing.setNationality((String) userData.get("nationality"));
            }

            // ✅ CORRECTION : Convertir String en ActivityDomain pour la mise à jour
            if (userData.containsKey("activitySector") && userData.get("activitySector") != null) {
                String sectorStr = (String) userData.get("activitySector");
                try {
                    ActivityDomain activitySector = ActivityDomain.valueOf(sectorStr.toUpperCase());
                    existing.setActivitySector(activitySector);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Secteur d'activité invalide: " + sectorStr);
                }
            }

            if (userData.containsKey("website")) {
                existing.setWebsite((String) userData.get("website"));
            }

            if (userData.containsKey("linkedinProfile")) {
                existing.setLinkedinProfile((String) userData.get("linkedinProfile"));
            }

            if (userData.containsKey("profilePicture")) {
                existing.setProfilePicture((String) userData.get("profilePicture"));
            }

            if (userData.containsKey("password")) {
                String newPassword = (String) userData.get("password");
                if (newPassword != null && !newPassword.isEmpty() && newPassword.length() >= 6) {
                    existing.setPassword(newPassword);
                    updatePasswordInKeycloak(userId, newPassword, adminToken);
                }
            }

            // ✅ 4. METTRE À JOUR KEYCLOAK (UNIQUEMENT SI CHANGEMENT DE NOM/PRÉNOM)
            if (!keycloakUpdates.isEmpty()) {
                updateUserInKeycloak(userId, keycloakUpdates, adminToken);
            }

            // ✅ 5. METTRE À JOUR L'EMAIL DANS KEYCLOAK (SI NÉCESSAIRE)
            if (emailChanged) {
                updateEmailInKeycloak(userId, newEmail, adminToken);
                existing.setEmail(newEmail);
            }

            // ✅ 6. SAUVEGARDER DANS MYSQL
            Investor updated = investorRepository.save(existing);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profil mis à jour avec succès");
            response.put("email", updated.getEmail());
            response.put("firstName", updated.getFirstName());
            response.put("lastName", updated.getLastName());
            response.put("role", updated.getRole());

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la mise à jour: " + e.getMessage());
        }
    }
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

    // ========================================
    // MÉTHODE POUR METTRE À JOUR L'UTILISATEUR DANS KEYCLOAK (SANS EMAIL)
    // ========================================
    private void updateUserInKeycloak(String userId, Map<String, Object> updates, String adminToken) {
        String updateUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        // ⚠️ CRÉER UN NOUVEAU MAP POUR ÉVITER TOUT PROBLÈME
        Map<String, Object> safeUpdates = new HashMap<>();

        // ✅ N'AJOUTER QUE LES CHAMPS AUTHORISÉS
        if (updates.containsKey("firstName")) {
            safeUpdates.put("firstName", updates.get("firstName"));
        }
        if (updates.containsKey("lastName")) {
            safeUpdates.put("lastName", updates.get("lastName"));
        }
        // ❌ NE PAS AJOUTER "email" ou "username"

        if (!safeUpdates.isEmpty()) {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(safeUpdates, headers);
            try {
                restTemplate.exchange(updateUrl, HttpMethod.PUT, entity, String.class);
                System.out.println("✅ Utilisateur Keycloak mis à jour: " + userId);
            } catch (Exception e) {
                System.out.println("❌ Erreur updateUserInKeycloak: " + e.getMessage());
                throw new RuntimeException("Erreur lors de la mise à jour dans Keycloak: " + e.getMessage());
            }
        }
    }

    // ========================================
    // MÉTHODE POUR METTRE À JOUR L'EMAIL DANS KEYCLOAK
    // ========================================
    private void updateEmailInKeycloak(String userId, String newEmail, String adminToken) {
        String updateUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> emailUpdate = new HashMap<>();
        emailUpdate.put("email", newEmail);
        emailUpdate.put("emailVerified", true);
        // ⚠️ NE PAS AJOUTER "username" !

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(emailUpdate, headers);

        try {
            restTemplate.exchange(updateUrl, HttpMethod.PUT, entity, String.class);
            System.out.println("✅ Email mis à jour dans Keycloak: " + userId);
        } catch (Exception e) {
            System.out.println("❌ Erreur updateEmailInKeycloak: " + e.getMessage());
            throw new RuntimeException("Erreur lors de la mise à jour de l'email dans Keycloak: " + e.getMessage());
        }
    }

    // ========================================
    // ENVOYER L'EMAIL DE RÉINITIALISATION
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

            System.out.println("✅ Email de réinitialisation envoyé à l'utilisateur: " + userId);

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email: " + e.getMessage());
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
            System.out.println("✅ Mot de passe mis à jour pour l'utilisateur: " + userId);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour du mot de passe: " + e.getMessage());
        }
    }
    // ========================================
// RÉCUPÉRER LE PROFIL COMPLET
// ========================================
    // ========================================
// RÉCUPÉRER LE PROFIL COMPLET
// ========================================
    public Map<String, Object> getProfile(String email) {
        Investor investor = investorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Investisseur non trouvé"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", investor.getId());
        profile.put("email", investor.getEmail());
        profile.put("firstName", investor.getFirstName());
        profile.put("lastName", investor.getLastName());
        profile.put("phone", investor.getPhone());
        profile.put("company", investor.getCompany());
        profile.put("originCountry", investor.getOriginCountry());
        // ✅ NOUVEAU: Nationality
        profile.put("nationality", investor.getNationality());
        profile.put("activitySector", investor.getActivitySector() != null ? investor.getActivitySector().name() : null);
        profile.put("website", investor.getWebsite());
        profile.put("linkedinProfile", investor.getLinkedinProfile());
        profile.put("profilePicture", investor.getProfilePicture());
        profile.put("registrationDate", investor.getRegistrationDate());
        profile.put("active", investor.getActive());
        profile.put("role", investor.getRole());

        return profile;
    }
    // ========================================
// SUPPRESSION COMPLÈTE DU COMPTE (KEYCLOAK + BASE DE DONNÉES)
// ========================================
    @Transactional
    public Map<String, Object> deleteAccount(String email, String password) {

        // 1. Vérifier que l'utilisateur existe dans MySQL
        Investor investor = investorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé dans la base de données"));

        try {
            // 2. Obtenir un token admin pour Keycloak
            String adminToken = getAdminToken();

            // 3. Récupérer l'ID de l'utilisateur dans Keycloak
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                // Si l'utilisateur n'existe pas dans Keycloak, on supprime quand même de MySQL
                System.out.println("⚠️ Utilisateur non trouvé dans Keycloak, suppression uniquement de MySQL");
            } else {
                // 4. Valider le mot de passe (optionnel - pour plus de sécurité)
                try {
                    validatePasswordWithKeycloak(email, password);
                } catch (Exception e) {
                    throw new RuntimeException("Mot de passe incorrect. La suppression est annulée.");
                }

                // 5. Supprimer l'utilisateur de Keycloak
                deleteUserFromKeycloak(userId, adminToken);
                System.out.println("✅ Utilisateur supprimé de Keycloak: " + userId);
            }

            // 6. Supprimer l'utilisateur de MySQL
            investorRepository.delete(investor);
            System.out.println("✅ Utilisateur supprimé de MySQL: " + email);

            // 7. Préparer la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Compte supprimé avec succès");
            response.put("email", email);

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la suppression du compte: " + e.getMessage());
        }
    }

    // ========================================
// SUPPRESSION DE KEYCLOAK
// ========================================
    private void deleteUserFromKeycloak(String userId, String adminToken) {
        String deleteUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            restTemplate.exchange(
                    deleteUrl,
                    HttpMethod.DELETE,
                    entity,
                    String.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la suppression de Keycloak: " + e.getMessage());
        }
    }

    // ========================================
// VALIDER LE MOT DE PASSE AVEC KEYCLOAK
// ========================================
    private void validatePasswordWithKeycloak(String email, String password) {
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
            restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Mot de passe incorrect");
        }
    }

    // ========================================
// VERSION SANS VALIDATION DE MOT DE PASSE (POUR ADMIN)
// ========================================
    @Transactional
    public Map<String, Object> deleteAccountByAdmin(String email) {

        // 1. Vérifier que l'utilisateur existe dans MySQL
        Investor investor = investorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé dans la base de données"));

        try {
            // 2. Obtenir un token admin pour Keycloak
            String adminToken = getAdminToken();

            // 3. Récupérer l'ID de l'utilisateur dans Keycloak
            String userId = getUserIdByEmail(email, adminToken);

            if (userId != null) {
                // 4. Supprimer l'utilisateur de Keycloak
                deleteUserFromKeycloak(userId, adminToken);
                System.out.println("✅ Utilisateur supprimé de Keycloak: " + userId);
            }

            // 5. Supprimer l'utilisateur de MySQL
            investorRepository.delete(investor);
            System.out.println("✅ Utilisateur supprimé de MySQL: " + email);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Compte supprimé avec succès");
            response.put("email", email);

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la suppression du compte: " + e.getMessage());
        }
    }
    // CHANGER LE MOT DE PASSE (AVEC VALIDATION DE L'ANCIEN)
// ========================================
    @Transactional
    public Map<String, Object> changePassword(String email, String oldPassword, String newPassword) {

        // 1. Vérifier que l'investor existe
        Investor investor = investorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Investisseur non trouvé"));

        // 2. Validation du nouveau mot de passe
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Le nouveau mot de passe doit contenir au moins 6 caractères");
        }

        try {
            // 3. Vérifier l'ancien mot de passe avec Keycloak
            validatePasswordWithKeycloak(email, oldPassword);

            // 4. Obtenir un token admin
            String adminToken = getAdminToken();

            // 5. Récupérer l'ID de l'utilisateur dans Keycloak
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("Utilisateur non trouvé dans Keycloak");
            }

            // 6. Mettre à jour le mot de passe dans Keycloak
            updatePasswordInKeycloak(userId, newPassword, adminToken);
            System.out.println("✅ Mot de passe mis à jour dans Keycloak pour: " + email);

            // 7. Mettre à jour le mot de passe dans MySQL
            investor.setPassword(newPassword);
            investorRepository.save(investor);
            System.out.println("✅ Mot de passe mis à jour dans MySQL pour: " + email);

            // 8. Préparer la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Mot de passe changé avec succès");
            response.put("email", email);

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors du changement de mot de passe: " + e.getMessage());
        }
    }

}