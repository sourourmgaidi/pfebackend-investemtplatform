package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tn.iset.investplatformpfe.Entity.ActivityDomain;
import tn.iset.investplatformpfe.Entity.EconomicPartner;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Repository.EconomicPartnerRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EconomicPartnerAuthService {

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    private final EconomicPartnerRepository partnerRepository;
    private final RestTemplate restTemplate;

    public EconomicPartnerAuthService(EconomicPartnerRepository partnerRepository) {
        this.partnerRepository = partnerRepository;
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
    @Transactional
    public Map<String, Object> register(Map<String, Object> userData) {
        String email = (String) userData.get("email");
        String password = (String) userData.get("password");
        String lastName = (String) userData.get("lastName");
        String firstName = (String) userData.get("firstName");

        if (email == null || password == null || lastName == null || firstName == null) {
            throw new RuntimeException("All required fields must be filled");
        }

        // ✅ VALIDATION GMAIL
        if (!isGmail(email)) {
            throw new RuntimeException("Only Gmail addresses are allowed. Please use a valid Gmail address (e.g., @gmail.com, @gmail.fr, etc.)");
        }

        if (partnerRepository.existsByEmail(email)) {
            throw new RuntimeException("This email is already in use");
        }

        try {
            String userId = createUserInKeycloak(email, password, firstName, lastName);
            assignRoleToUser(userId, "PARTNER");

            EconomicPartner newPartner = new EconomicPartner();
            newPartner.setEmail(email);
            newPartner.setPassword(password);
            newPartner.setLastName(lastName);
            newPartner.setFirstName(firstName);
            newPartner.setActive(true);
            newPartner.setRole(Role.PARTNER);
            newPartner.setRegistrationDate(LocalDateTime.now());

            // Champs optionnels
            if (userData.containsKey("phone") && userData.get("phone") != null) {
                newPartner.setPhone((String) userData.get("phone"));
            }

            if (userData.containsKey("countryOfOrigin") && userData.get("countryOfOrigin") != null) {
                newPartner.setCountryOfOrigin((String) userData.get("countryOfOrigin"));
            }

            // ✅ CORRECTION: Conversion String → ActivityDomain
            if (userData.containsKey("businessSector") && userData.get("businessSector") != null) {
                String sectorStr = (String) userData.get("businessSector");
                try {
                    ActivityDomain businessSector = ActivityDomain.valueOf(sectorStr.toUpperCase());
                    newPartner.setBusinessSector(businessSector);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid business sector: " + sectorStr);
                }
            }

            if (userData.containsKey("headquartersAddress") && userData.get("headquartersAddress") != null) {
                newPartner.setHeadquartersAddress((String) userData.get("headquartersAddress"));
            }

            if (userData.containsKey("website") && userData.get("website") != null) {
                newPartner.setWebsite((String) userData.get("website"));
            }

            if (userData.containsKey("profilePhoto") && userData.get("profilePhoto") != null) {
                newPartner.setProfilePhoto((String) userData.get("profilePhoto"));
            }

            if (userData.containsKey("linkedinProfile") && userData.get("linkedinProfile") != null) {
                newPartner.setLinkedinProfile((String) userData.get("linkedinProfile"));
            }

            EconomicPartner saved = partnerRepository.save(newPartner);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Registration successful");
            response.put("id", saved.getId());
            response.put("email", saved.getEmail());
            response.put("lastName", saved.getLastName());
            response.put("firstName", saved.getFirstName());
            response.put("role", saved.getRole());

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error during registration: " + e.getMessage());
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
            throw new RuntimeException("Authentication error: " + e.getMessage());
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
            throw new RuntimeException("Refresh error: " + e.getMessage());
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
            throw new RuntimeException("Logout error: " + e.getMessage());
        }
    }

    // ========================================
    // RÉCUPÉRER LE PROFIL
    // ========================================
    public Map<String, Object> getProfile(String email) {
        EconomicPartner partner = partnerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Partner not found"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", partner.getId());
        profile.put("email", partner.getEmail());
        profile.put("lastName", partner.getLastName());
        profile.put("firstName", partner.getFirstName());
        profile.put("phone", partner.getPhone());
        profile.put("countryOfOrigin", partner.getCountryOfOrigin());
        profile.put("businessSector", partner.getBusinessSector() != null ? partner.getBusinessSector().name() : null);
        profile.put("headquartersAddress", partner.getHeadquartersAddress());
        profile.put("website", partner.getWebsite());
        profile.put("profilePhoto", partner.getProfilePhoto());
        profile.put("linkedinProfile", partner.getLinkedinProfile());
        profile.put("role", partner.getRole());

        return profile;
    }

    // ========================================
    // METTRE À JOUR LE PROFIL - AVEC VALIDATION GMAIL
    // ========================================
    @Transactional
    public Map<String, Object> updateProfile(String email, Map<String, Object> userData) {
        EconomicPartner existing = partnerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Partner not found"));

        try {
            String adminToken = getAdminToken();
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("User not found in Keycloak");
            }

            Map<String, Object> keycloakUpdates = new HashMap<>();
            boolean emailChanged = false;
            String newEmail = null;

            // Mise à jour du lastName
            if (userData.containsKey("lastName")) {
                String newLastName = (String) userData.get("lastName");
                existing.setLastName(newLastName);
                keycloakUpdates.put("lastName", newLastName);
            }

            // Mise à jour du firstName
            if (userData.containsKey("firstName")) {
                String newFirstName = (String) userData.get("firstName");
                existing.setFirstName(newFirstName);
                keycloakUpdates.put("firstName", newFirstName);
            }

            // GESTION SPÉCIALE POUR L'EMAIL AVEC VALIDATION GMAIL
            if (userData.containsKey("email")) {
                newEmail = (String) userData.get("email");

                if (!newEmail.equals(existing.getEmail())) {
                    // ✅ VALIDATION GMAIL POUR LE NOUVEL EMAIL
                    if (!isGmail(newEmail)) {
                        throw new RuntimeException("The new email must be a valid Gmail address");
                    }

                    if (partnerRepository.existsByEmail(newEmail)) {
                        throw new RuntimeException("Email already in use: " + newEmail);
                    }
                    emailChanged = true;
                }
            }

            // Mise à jour du téléphone
            if (userData.containsKey("phone")) {
                existing.setPhone((String) userData.get("phone"));
            }

            // Mise à jour du pays d'origine
            if (userData.containsKey("countryOfOrigin")) {
                existing.setCountryOfOrigin((String) userData.get("countryOfOrigin"));
            }

            // ✅ CORRECTION: Conversion String → ActivityDomain pour la mise à jour
            if (userData.containsKey("businessSector") && userData.get("businessSector") != null) {
                String sectorStr = (String) userData.get("businessSector");
                try {
                    ActivityDomain businessSector = ActivityDomain.valueOf(sectorStr.toUpperCase());
                    existing.setBusinessSector(businessSector);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid business sector: " + sectorStr);
                }
            }

            // Mise à jour de l'adresse du siège
            if (userData.containsKey("headquartersAddress")) {
                existing.setHeadquartersAddress((String) userData.get("headquartersAddress"));
            }

            // Mise à jour du site web
            if (userData.containsKey("website")) {
                existing.setWebsite((String) userData.get("website"));
            }

            // Mise à jour de la photo de profil
            if (userData.containsKey("profilePhoto")) {
                existing.setProfilePhoto((String) userData.get("profilePhoto"));
            }

            // Mise à jour du profil LinkedIn
            if (userData.containsKey("linkedinProfile")) {
                existing.setLinkedinProfile((String) userData.get("linkedinProfile"));
            }

            // 1. Mettre à jour Keycloak avec les modifications standards (nom, prénom)
            if (!keycloakUpdates.isEmpty()) {
                updateUserInKeycloak(userId, keycloakUpdates, adminToken);
            }

            // 2. Gérer le changement d'email dans Keycloak SI NÉCESSAIRE
            if (emailChanged) {
                updateEmailInKeycloak(userId, newEmail, adminToken);
                existing.setEmail(newEmail);
            }

            // 3. Sauvegarder dans MySQL
            EconomicPartner updated = partnerRepository.save(existing);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            response.put("email", updated.getEmail());
            response.put("lastName", updated.getLastName());
            response.put("firstName", updated.getFirstName());

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error during profile update: " + e.getMessage());
        }
    }

    // ========================================
    // MOT DE PASSE OUBLIÉ
    // ========================================
    public Map<String, Object> forgotPassword(String email) {
        partnerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with this email"));

        try {
            String adminToken = getAdminToken();
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("User not found in Keycloak");
            }

            sendResetPasswordEmail(userId, adminToken);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "A reset email has been sent to " + email);
            response.put("email", email);

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Error during request: " + e.getMessage());
        }
    }

    // ========================================
    // RÉINITIALISER LE MOT DE PASSE
    // ========================================
    public Map<String, Object> resetPassword(String email, String newPassword) {
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters");
        }

        try {
            String adminToken = getAdminToken();
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("User not found in Keycloak");
            }

            updatePasswordInKeycloak(userId, newPassword, adminToken);

            EconomicPartner partner = partnerRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Partner not found"));
            partner.setPassword(newPassword);
            partnerRepository.save(partner);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password reset successfully");
            response.put("email", email);

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Error during reset: " + e.getMessage());
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
        map.add("username", "admin");
        map.add("password", "admin");
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

        String rolesUrl = authServerUrl + "/admin/realms/" + realm + "/roles";

        HttpEntity<?> entity = new HttpEntity<>(headers);

        ResponseEntity<Map[]> rolesResponse = restTemplate.exchange(
                rolesUrl,
                HttpMethod.GET,
                entity,
                Map[].class
        );

        String roleId = null;
        for (Map role : rolesResponse.getBody()) {
            if (roleName.equals(role.get("name"))) {
                roleId = (String) role.get("id");
                break;
            }
        }

        if (roleId == null) {
            throw new RuntimeException("Role " + roleName + " not found in Keycloak");
        }

        String assignUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm";

        Map<String, Object> roleMapping = new HashMap<>();
        roleMapping.put("id", roleId);
        roleMapping.put("name", roleName);

        HttpEntity<Map[]> assignEntity = new HttpEntity<>(new Map[]{roleMapping}, headers);

        restTemplate.exchange(assignUrl, HttpMethod.POST, assignEntity, String.class);
    }

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

    // ========================================
    // MÉTHODE POUR METTRE À JOUR L'UTILISATEUR DANS KEYCLOAK
    // ========================================
    private void updateUserInKeycloak(String userId, Map<String, Object> updates, String adminToken) {
        String updateUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(updates, headers);

        restTemplate.exchange(updateUrl, HttpMethod.PUT, entity, String.class);
    }

    // ========================================
    // Mettre à jour l'email dans Keycloak
    // ========================================
    private void updateEmailInKeycloak(String userId, String newEmail, String adminToken) {
        String updateUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> emailUpdate = new HashMap<>();
        emailUpdate.put("email", newEmail);
        emailUpdate.put("emailVerified", true);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(emailUpdate, headers);

        restTemplate.exchange(updateUrl, HttpMethod.PUT, entity, String.class);
    }

    private void sendResetPasswordEmail(String userId, String adminToken) {
        String emailUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/execute-actions-email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        List<String> requiredActions = List.of("UPDATE_PASSWORD");
        String redirectUri = "http://localhost:4200/economic-partners/reset-password-complete";

        String urlWithParams = emailUrl + "?client_id=" + clientId + "&redirect_uri=" + redirectUri;

        HttpEntity<List<String>> entity = new HttpEntity<>(requiredActions, headers);

        restTemplate.exchange(urlWithParams, HttpMethod.PUT, entity, String.class);
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
// SUPPRESSION DE COMPTE PAR L'UTILISATEUR (AVEC MOT DE PASSE)
// ========================================
    @Transactional
    public Map<String, Object> deleteAccount(String email, String password) {

        // 1. Vérifier que l'utilisateur existe dans MySQL
        EconomicPartner partner = partnerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Partner not found in database"));

        try {
            // 2. Obtenir un token admin pour Keycloak
            String adminToken = getAdminToken();

            // 3. Récupérer l'ID de l'utilisateur dans Keycloak
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("User not found in Keycloak");
            }

            // 4. Valider le mot de passe (pour confirmer l'identité)
            try {
                validatePasswordWithKeycloak(email, password);
            } catch (Exception e) {
                throw new RuntimeException("Incorrect password. Deletion cancelled.");
            }

            // 5. Supprimer l'utilisateur de Keycloak
            deleteUserFromKeycloak(userId, adminToken);
            System.out.println("✅ User deleted from Keycloak: " + userId);

            // 6. Supprimer l'utilisateur de MySQL
            partnerRepository.delete(partner);
            System.out.println("✅ User deleted from MySQL: " + email);

            // 7. Préparer la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Account deleted successfully");
            response.put("email", email);

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error during account deletion: " + e.getMessage());
        }
    }

    // ========================================
// SUPPRESSION DE COMPTE PAR L'ADMIN (SANS MOT DE PASSE)
// ========================================
    @Transactional
    public Map<String, Object> deleteAccountByAdmin(String email) {

        // 1. Vérifier que l'utilisateur existe dans MySQL
        EconomicPartner partner = partnerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Partner not found in database"));

        try {
            // 2. Obtenir un token admin pour Keycloak
            String adminToken = getAdminToken();

            // 3. Récupérer l'ID de l'utilisateur dans Keycloak
            String userId = getUserIdByEmail(email, adminToken);

            if (userId != null) {
                // 4. Supprimer l'utilisateur de Keycloak
                deleteUserFromKeycloak(userId, adminToken);
                System.out.println("✅ User deleted from Keycloak: " + userId);
            } else {
                System.out.println("⚠️ User not found in Keycloak, deleting only from MySQL");
            }

            // 5. Supprimer l'utilisateur de MySQL
            partnerRepository.delete(partner);
            System.out.println("✅ User deleted from MySQL: " + email);

            // 6. Préparer la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Account deleted successfully by admin");
            response.put("email", email);

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error during account deletion: " + e.getMessage());
        }
    }

// ========================================
// MÉTHODES PRIVÉES SUPPLÉMENTAIRES
// ========================================

    /**
     * Supprimer un utilisateur de Keycloak
     */
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
            throw new RuntimeException("Error deleting user from Keycloak: " + e.getMessage());
        }
    }

    /**
     * Valider le mot de passe avec Keycloak
     */
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
            throw new RuntimeException("Incorrect password");
        }
    }
    // CHANGER LE MOT DE PASSE (UTILISATEUR CONNECTÉ)
// ========================================
    @Transactional
    public Map<String, Object> changePassword(String email, String oldPassword, String newPassword) {

        // 1. Vérifier que l'utilisateur existe dans MySQL
        EconomicPartner partner = partnerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Partner not found in database"));

        // 2. Validation du nouveau mot de passe
        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Le nouveau mot de passe doit contenir au moins 6 caractères");
        }

        try {
            // 3. Obtenir un token admin pour Keycloak
            String adminToken = getAdminToken();

            // 4. Récupérer l'ID de l'utilisateur dans Keycloak
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("Utilisateur non trouvé dans Keycloak");
            }

            // 5. Valider l'ancien mot de passe avec Keycloak
            try {
                validatePasswordWithKeycloak(email, oldPassword);
                System.out.println("✅ Ancien mot de passe validé pour: " + email);
            } catch (Exception e) {
                throw new RuntimeException("Ancien mot de passe incorrect");
            }

            // 6. Mettre à jour le mot de passe dans Keycloak
            updatePasswordInKeycloak(userId, newPassword, adminToken);
            System.out.println("✅ Mot de passe mis à jour dans Keycloak pour: " + email);

            // 7. Mettre à jour le mot de passe dans MySQL
            partner.setPassword(newPassword);
            partnerRepository.save(partner);
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