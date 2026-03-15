package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Entity.Tourist;
import tn.iset.investplatformpfe.Repository.TouristRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TouristAuthService {

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    private final TouristRepository touristRepository;
    private final RestTemplate restTemplate;

    public TouristAuthService(TouristRepository touristRepository) {
        this.touristRepository = touristRepository;
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

        // 1. Vérifier les champs obligatoires
        if (email == null || password == null || lastName == null || firstName == null) {
            throw new RuntimeException("All required fields must be filled");
        }

        // ✅ VALIDATION GMAIL
        if (!isGmail(email)) {
            throw new RuntimeException("Only Gmail addresses are allowed. Please use a valid Gmail address (e.g., @gmail.com, @gmail.fr, etc.)");
        }

        // 2. Vérifier si l'email existe déjà
        if (touristRepository.existsByEmail(email)) {
            throw new RuntimeException("This email is already in use");
        }

        try {
            // 3. Créer l'utilisateur dans Keycloak
            String userId = createUserInKeycloak(email, password, firstName, lastName);

            // 4. Assigner le rôle TOURIST dans Keycloak
            assignRoleToUser(userId, "TOURIST");

            // 5. Créer dans MySQL
            Tourist newTourist = new Tourist();
            newTourist.setEmail(email);
            newTourist.setPassword(password);
            newTourist.setLastName(lastName);
            newTourist.setFirstName(firstName);
            newTourist.setActive(true);
            newTourist.setRole(Role.TOURIST);
            newTourist.setRegistrationDate(LocalDateTime.now());

            // ✅ Champs optionnels
            if (userData.containsKey("phone") && userData.get("phone") != null) {
                newTourist.setPhone((String) userData.get("phone"));
            }

            if (userData.containsKey("nationality") && userData.get("nationality") != null) {
                newTourist.setNationality((String) userData.get("nationality"));
            }

            if (userData.containsKey("profilePhoto") && userData.get("profilePhoto") != null) {
                newTourist.setProfilePhoto((String) userData.get("profilePhoto"));
            }

            Tourist savedTourist = touristRepository.save(newTourist);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Registration successful");
            response.put("id", savedTourist.getId());
            response.put("email", savedTourist.getEmail());
            response.put("lastName", savedTourist.getLastName());
            response.put("firstName", savedTourist.getFirstName());
            response.put("role", savedTourist.getRole());

            // Ajouter les champs optionnels dans la réponse s'ils existent
            if (savedTourist.getPhone() != null) {
                response.put("phone", savedTourist.getPhone());
            }
            if (savedTourist.getNationality() != null) {
                response.put("nationality", savedTourist.getNationality());
            }

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
    // ========================================
// RÉCUPÉRER LE PROFIL - AVEC PHOTO
// ========================================
    public Map<String, Object> getProfile(String email) {

        Tourist tourist = touristRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tourist not found"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", tourist.getId());
        profile.put("email", tourist.getEmail());
        profile.put("lastName", tourist.getLastName());
        profile.put("firstName", tourist.getFirstName());
        profile.put("phone", tourist.getPhone());
        profile.put("nationality", tourist.getNationality());
        profile.put("role", tourist.getRole());

        // ✅ AJOUT DE LA PHOTO
        profile.put("profilePhoto", tourist.getProfilePhoto());
        profile.put("photo", tourist.getProfilePhoto()); // Pour compatibilité

        return profile;
    }

    // ========================================
    // METTRE À JOUR LE PROFIL - AVEC VALIDATION GMAIL
    // ========================================
    @Transactional
    public Map<String, Object> updateProfile(String email, Map<String, Object> userData) {

        // 1. Récupérer le touriste dans MySQL
        Tourist existingTourist = touristRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tourist not found"));

        try {
            // 2. Obtenir un token admin pour Keycloak
            String adminToken = getAdminToken();

            // 3. Récupérer l'ID de l'utilisateur dans Keycloak
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("User not found in Keycloak");
            }

            // 4. Préparer les mises à jour pour Keycloak
            Map<String, Object> keycloakUpdates = new HashMap<>();
            boolean emailChanged = false;
            String newEmail = null;

            // 5. GESTION DE L'EMAIL AVEC VALIDATION GMAIL
            if (userData.containsKey("email")) {
                newEmail = (String) userData.get("email");

                if (!newEmail.equals(existingTourist.getEmail())) {
                    // ✅ VALIDATION GMAIL POUR LE NOUVEL EMAIL
                    if (!isGmail(newEmail)) {
                        throw new RuntimeException("The new email must be a valid Gmail address");
                    }

                    // Vérifier que le nouvel email n'est pas déjà utilisé
                    if (touristRepository.existsByEmail(newEmail)) {
                        throw new RuntimeException("Email already in use: " + newEmail);
                    }
                    emailChanged = true;
                }
            }

            // 6. Mise à jour des champs dans MySQL et préparation Keycloak
            if (userData.containsKey("lastName")) {
                String newLastName = (String) userData.get("lastName");
                existingTourist.setLastName(newLastName);
                keycloakUpdates.put("lastName", newLastName);
            }

            if (userData.containsKey("firstName")) {
                String newFirstName = (String) userData.get("firstName");
                existingTourist.setFirstName(newFirstName);
                keycloakUpdates.put("firstName", newFirstName);
            }

            if (userData.containsKey("phone")) {
                existingTourist.setPhone((String) userData.get("phone"));
            }

            if (userData.containsKey("nationality")) {
                existingTourist.setNationality((String) userData.get("nationality"));
            }

            if (userData.containsKey("profilePhoto")) {
                existingTourist.setProfilePhoto((String) userData.get("profilePhoto"));
            }

            // 7. Mise à jour du mot de passe si fourni
            if (userData.containsKey("password")) {
                String newPassword = (String) userData.get("password");
                if (newPassword != null && !newPassword.isEmpty() && newPassword.length() >= 6) {
                    existingTourist.setPassword(newPassword);
                    updatePasswordInKeycloak(userId, newPassword, adminToken);
                }
            }

            // 8. Mettre à jour Keycloak avec les modifications standards (nom, prénom)
            if (!keycloakUpdates.isEmpty()) {
                updateUserInKeycloak(userId, keycloakUpdates, adminToken);
            }

            // 9. Gérer le changement d'email dans Keycloak SI NÉCESSAIRE
            if (emailChanged) {
                updateEmailInKeycloak(userId, newEmail, adminToken);
                existingTourist.setEmail(newEmail);
            }

            // 10. Sauvegarder dans MySQL
            Tourist updated = touristRepository.save(existingTourist);

            // 11. Préparer la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            response.put("email", updated.getEmail());
            response.put("lastName", updated.getLastName());
            response.put("firstName", updated.getFirstName());

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Error during profile update: " + e.getMessage());
        }
    }

    // ========================================
    // MOT DE PASSE OUBLIÉ
    // ========================================
    public Map<String, Object> forgotPassword(String email) {

        touristRepository.findByEmail(email)
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

            Tourist tourist = touristRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Tourist not found in database"));
            tourist.setPassword(newPassword);
            touristRepository.save(tourist);

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

        // ✅ Créer un safeUpdates sans username
        Map<String, Object> safeUpdates = new HashMap<>();
        if (updates.containsKey("firstName")) {
            safeUpdates.put("firstName", updates.get("firstName"));
        }
        if (updates.containsKey("lastName")) {
            safeUpdates.put("lastName", updates.get("lastName"));
        }

        if (!safeUpdates.isEmpty()) {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(safeUpdates, headers);
            try {
                restTemplate.exchange(updateUrl, HttpMethod.PUT, entity, String.class);
                System.out.println("✅ Utilisateur Keycloak mis à jour: " + userId);
            } catch (Exception e) {
                throw new RuntimeException("Error updating user in Keycloak: " + e.getMessage());
            }
        }
    }

    // ========================================
    // ✅ MÉTHODE POUR METTRE À JOUR L'EMAIL DANS KEYCLOAK
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
            System.out.println("✅ Email updated in Keycloak: " + userId);
        } catch (Exception e) {
            throw new RuntimeException("Error updating email in Keycloak: " + e.getMessage());
        }
    }

    private void sendResetPasswordEmail(String userId, String adminToken) {
        String emailUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/execute-actions-email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        List<String> requiredActions = List.of("UPDATE_PASSWORD");
        String redirectUri = "http://localhost:4200/tourists/reset-password-complete";

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
        Tourist tourist = touristRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tourist not found in database"));

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
            touristRepository.delete(tourist);
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
        Tourist tourist = touristRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tourist not found in database"));

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
            touristRepository.delete(tourist);
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
            System.out.println("✅ User deleted from Keycloak: " + userId);
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
            System.out.println("✅ Password validated for: " + email);
        } catch (Exception e) {
            throw new RuntimeException("Incorrect password");
        }
    }
    // CHANGER LE MOT DE PASSE (TOURISTE CONNECTÉ)
// ========================================
    @Transactional
    public Map<String, Object> changePassword(String email, String oldPassword, String newPassword) {

        // 1. Vérifier que l'utilisateur existe dans MySQL
        Tourist tourist = touristRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tourist not found in database"));

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
            tourist.setPassword(newPassword);
            touristRepository.save(tourist);
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