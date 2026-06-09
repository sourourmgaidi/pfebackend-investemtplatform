package tn.iset.investplatformpfe.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Entity.Tourist;
import tn.iset.investplatformpfe.Entity.UserSession;
import tn.iset.investplatformpfe.Repository.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TouristAuthService {

    //  AJOUT: Logger pour remplacer les System.out.println
    private static final Logger log = LoggerFactory.getLogger(TouristAuthService.class);

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    private final TouristRepository touristRepository;
    private final InvestorRepository investorRepository;
    private final EconomicPartnerRepository economicPartnerRepository;
    private final LocalPartnerRepository localPartnerRepository;
    private final InternationalCompanyRepository internationalCompanyRepository;
    private final UserSessionService sessionService;
    private final RestTemplate restTemplate;

    public TouristAuthService(
            TouristRepository touristRepository,
            InvestorRepository investorRepository,
            EconomicPartnerRepository economicPartnerRepository,
            LocalPartnerRepository localPartnerRepository,
            InternationalCompanyRepository internationalCompanyRepository,
            UserSessionService sessionService) {
        this.touristRepository = touristRepository;
        this.investorRepository = investorRepository;
        this.economicPartnerRepository = economicPartnerRepository;
        this.localPartnerRepository = localPartnerRepository;
        this.internationalCompanyRepository = internationalCompanyRepository;
        this.sessionService = sessionService;
        this.restTemplate = new RestTemplate();
    }

    // ========================================
    // VÉRIFIER L'EMAIL DANS TOUTES LES TABLES
    // ========================================
    private boolean isEmailAlreadyUsed(String email) {
        log.info("Vérification email dans toutes les tables: {}", email);

        if (touristRepository.existsByEmail(email)) {
            log.warn("Email trouvé dans Tourist table");
            return true;
        }
        if (investorRepository.existsByEmail(email)) {
            log.warn("Email trouvé dans Investor table");
            return true;
        }
        if (economicPartnerRepository.existsByEmail(email)) {
            log.warn("Email trouvé dans EconomicPartner table");
            return true;
        }
        if (localPartnerRepository.existsByEmail(email)) {
            log.warn("Email trouvé dans LocalPartner table");
            return true;
        }
        if (internationalCompanyRepository.existsByEmail(email)) {
            log.warn("Email trouvé dans InternationalCompany table");
            return true;
        }

        log.info("Email disponible pour inscription: {}", email);
        return false;
    }

    // ========================================
    // VALIDATION GMAIL
    // ========================================
    private boolean isGmail(String email) {
        if (email == null) return false;

        // ✅ CORRIGÉ: Protection contre email sans '@'
        int atIndex = email.indexOf("@");
        if (atIndex < 0) return false;

        String domain = email.substring(atIndex + 1).toLowerCase();

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
    // INSCRIPTION
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

        if (!isGmail(email)) {
            throw new RuntimeException("Only Gmail addresses are allowed. Please use a valid Gmail address (e.g., @gmail.com, @gmail.fr, etc.)");
        }

        if (isEmailAlreadyUsed(email)) {
            throw new RuntimeException("This email is already in use. Please use a different email address.");
        }

        try {
            String userId = createUserInKeycloak(email, password, firstName, lastName);
            assignRoleToUser(userId, "TOURIST");

            Tourist newTourist = new Tourist();
            newTourist.setEmail(email);
            newTourist.setPassword(password);
            newTourist.setLastName(lastName);
            newTourist.setFirstName(firstName);
            newTourist.setActive(true);
            newTourist.setRole(Role.TOURIST);
            newTourist.setRegistrationDate(LocalDateTime.now());

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

            if (savedTourist.getPhone() != null) response.put("phone", savedTourist.getPhone());
            if (savedTourist.getNationality() != null) response.put("nationality", savedTourist.getNationality());

            return response;

        } catch (Exception e) {
            log.error("Erreur lors de l'inscription pour {}: {}", email, e.getMessage());
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
                    tokenUrl, HttpMethod.POST, entity, Map.class);

            // ✅ PLUS DE SESSION ICI — gérée dans le controller
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
    // DÉCONNEXION SIMPLE (sans session)
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
    // ✅ DÉMARRER LA SESSION (appelé depuis le controller)
    public void startSessionAfterLogin(String email, String role) {
        try {
            sessionService.startSession(email, role);
            log.info("✅ Session démarrée pour {} avec rôle: {}", email, role);
        } catch (Exception e) {
            log.error("⚠️ Impossible de démarrer la session pour {}: {}", email, e.getMessage());
        }
    }
    public String findEmailByKeycloakSub(String sub) {
        try {
            String adminToken = getAdminToken();
            String userUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + sub;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            ResponseEntity<Map> response = restTemplate.exchange(
                    userUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

            if (response.getBody() != null) {
                String email = (String) response.getBody().get("email");
                log.info("📧 Email trouvé via Keycloak sub: {}", email);
                return email;
            }
            return null;
        } catch (Exception e) {
            log.error("⚠️ Erreur findEmailByKeycloakSub: {}", e.getMessage());
            return null;
        }
    }

    // ✅ TERMINER LA SESSION (appelé depuis le controller)
    public void endSession(String email) {
        try {
            sessionService.endSession(email);
            log.info("✅ Session terminée pour {}", email);
        } catch (Exception e) {
            log.error("⚠️ Erreur fermeture session pour {}: {}", email, e.getMessage());
        }
    }

    // ========================================
    // DÉCONNEXION AVEC EMAIL — FERME LA SESSION
    // ========================================
    public void logoutWithEmail(String refreshToken, String email) {
        log.info("Logout demandé pour: {}", email);

        // ✅ CORRIGÉ: Bloc Keycloak séparé du bloc session
        // Si Keycloak échoue, on ferme quand même la session locale

        // 1. Déconnexion Keycloak
        try {
            String logoutUrl = authServerUrl + "/realms/" + realm + "/protocol/openid-connect/logout";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            map.add("client_id", clientId);
            map.add("refresh_token", refreshToken);

            restTemplate.postForEntity(logoutUrl, new HttpEntity<>(map, headers), String.class);
            log.info("✅ Déconnexion Keycloak réussie pour {}", email);

        } catch (Exception e) {
            // ✅ CORRIGÉ: On log l'erreur mais on ne bloque pas — on ferme quand même la session locale
            log.error("⚠️ Erreur Keycloak logout pour {} (session locale fermée quand même): {}", email, e.getMessage());
        }

        // 2. Fermeture session locale — toujours exécutée même si Keycloak a échoué
        try {
            UserSession ended = sessionService.endSession(email);
            if (ended != null) {
                log.info("✅ Session terminée pour {} - Durée: {}s", email, ended.getDurationSeconds());
            } else {
                log.warn("⚠️ Aucune session active trouvée pour {}", email);
            }
        } catch (Exception e) {
            log.error("❌ Erreur fermeture session pour {}: {}", email, e.getMessage());
            throw new RuntimeException("Logout error: " + e.getMessage());
        }
    }

    // ========================================
    // RÉCUPÉRER LE PROFIL
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
        profile.put("profilePhoto", tourist.getProfilePhoto());
        profile.put("photo", tourist.getProfilePhoto());

        return profile;
    }

    // ========================================
    // METTRE À JOUR LE PROFIL
    // ========================================
    @Transactional
    public Map<String, Object> updateProfile(String email, Map<String, Object> userData) {

        Tourist existingTourist = touristRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tourist not found"));

        try {
            String adminToken = getAdminToken();
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("User not found in Keycloak");
            }

            Map<String, Object> keycloakUpdates = new HashMap<>();
            boolean emailChanged = false;
            String newEmail = null;

            if (userData.containsKey("email")) {
                newEmail = (String) userData.get("email");

                if (!newEmail.equals(existingTourist.getEmail())) {
                    if (!isGmail(newEmail)) {
                        throw new RuntimeException("The new email must be a valid Gmail address");
                    }
                    if (isEmailAlreadyUsed(newEmail)) {
                        throw new RuntimeException("Email already in use: " + newEmail);
                    }
                    emailChanged = true;
                }
            }

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
            if (userData.containsKey("password")) {
                String newPassword = (String) userData.get("password");
                if (newPassword != null && !newPassword.isEmpty() && newPassword.length() >= 6) {
                    existingTourist.setPassword(newPassword);
                    updatePasswordInKeycloak(userId, newPassword, adminToken);
                }
            }

            if (!keycloakUpdates.isEmpty()) {
                updateUserInKeycloak(userId, keycloakUpdates, adminToken);
            }
            if (emailChanged) {
                updateEmailInKeycloak(userId, newEmail, adminToken);
                existingTourist.setEmail(newEmail);
            }

            Tourist updated = touristRepository.save(existingTourist);

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
    // SUPPRIMER LE COMPTE (par l'utilisateur)
    // ========================================
    @Transactional
    public Map<String, Object> deleteAccount(String email, String password) {

        Tourist tourist = touristRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tourist not found in database"));

        try {
            String adminToken = getAdminToken();
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("User not found in Keycloak");
            }

            try {
                validatePasswordWithKeycloak(email, password);
            } catch (Exception e) {
                throw new RuntimeException("Incorrect password. Deletion cancelled.");
            }

            // ✅ CORRIGÉ: endSession isolé — ne bloque pas la suppression
            try {
                sessionService.endSession(email);
            } catch (Exception sessionEx) {
                log.warn("⚠️ Impossible de fermer la session pour {}: {}", email, sessionEx.getMessage());
            }

            deleteUserFromKeycloak(userId, adminToken);
            log.info("✅ User deleted from Keycloak: {}", userId);

            touristRepository.delete(tourist);
            log.info("✅ User deleted from MySQL: {}", email);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Account deleted successfully");
            response.put("email", email);

            return response;

        } catch (Exception e) {
            log.error("Erreur suppression compte {}: {}", email, e.getMessage());
            throw new RuntimeException("Error during account deletion: " + e.getMessage());
        }
    }

    // ========================================
    // SUPPRIMER LE COMPTE (par l'admin)
    // ========================================
    @Transactional
    public Map<String, Object> deleteAccountByAdmin(String email) {

        Tourist tourist = touristRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tourist not found in database"));

        try {
            String adminToken = getAdminToken();
            String userId = getUserIdByEmail(email, adminToken);

            if (userId != null) {
                deleteUserFromKeycloak(userId, adminToken);
                log.info("✅ User deleted from Keycloak: {}", userId);
            } else {
                log.warn("⚠️ User not found in Keycloak, deleting only from MySQL");
            }

            // ✅ AJOUT: Fermer la session si elle existe
            try {
                sessionService.endSession(email);
            } catch (Exception sessionEx) {
                log.warn("⚠️ Impossible de fermer la session pour {}: {}", email, sessionEx.getMessage());
            }

            touristRepository.delete(tourist);
            log.info("✅ User deleted from MySQL: {}", email);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Account deleted successfully by admin");
            response.put("email", email);

            return response;

        } catch (Exception e) {
            log.error("Erreur suppression admin compte {}: {}", email, e.getMessage());
            throw new RuntimeException("Error during account deletion: " + e.getMessage());
        }
    }

    // ========================================
    // CHANGER LE MOT DE PASSE
    // ========================================
    @Transactional
    public Map<String, Object> changePassword(String email, String oldPassword, String newPassword) {

        Tourist tourist = touristRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tourist not found in database"));

        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Le nouveau mot de passe doit contenir au moins 6 caractères");
        }

        try {
            String adminToken = getAdminToken();
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("Utilisateur non trouvé dans Keycloak");
            }

            try {
                validatePasswordWithKeycloak(email, oldPassword);
                log.info("✅ Ancien mot de passe validé pour: {}", email);
            } catch (Exception e) {
                throw new RuntimeException("Ancien mot de passe incorrect");
            }

            updatePasswordInKeycloak(userId, newPassword, adminToken);
            log.info("✅ Mot de passe mis à jour dans Keycloak pour: {}", email);

            tourist.setPassword(newPassword);
            touristRepository.save(tourist);
            log.info("✅ Mot de passe mis à jour dans MySQL pour: {}", email);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Mot de passe changé avec succès");
            response.put("email", email);

            return response;

        } catch (Exception e) {
            log.error("Erreur changement mot de passe pour {}: {}", email, e.getMessage());
            throw new RuntimeException("Erreur lors du changement de mot de passe: " + e.getMessage());
        }
    }

    // ========================================
    // MÉTHODES PRIVÉES KEYCLOAK
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

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl, HttpMethod.POST, new HttpEntity<>(map, headers), Map.class
        );

        return (String) response.getBody().get("access_token");
    }

    private String createUserInKeycloak(String email, String password, String firstName, String lastName) {
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

        ResponseEntity<String> response = restTemplate.postForEntity(
                authServerUrl + "/admin/realms/" + realm + "/users",
                new HttpEntity<>(user, headers),
                String.class
        );

        String location = response.getHeaders().getLocation().toString();
        return location.substring(location.lastIndexOf("/") + 1);
    }

    private void assignRoleToUser(String userId, String roleName) {
        String adminToken = getAdminToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map[]> rolesResponse = restTemplate.exchange(
                authServerUrl + "/admin/realms/" + realm + "/roles",
                HttpMethod.GET,
                new HttpEntity<>(headers),
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

        Map<String, Object> roleMapping = new HashMap<>();
        roleMapping.put("id", roleId);
        roleMapping.put("name", roleName);

        restTemplate.exchange(
                authServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm",
                HttpMethod.POST,
                new HttpEntity<>(new Map[]{roleMapping}, headers),
                String.class
        );
    }

    private String getUserIdByEmail(String email, String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        ResponseEntity<Map[]> response = restTemplate.exchange(
                authServerUrl + "/admin/realms/" + realm + "/users?email=" + email,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map[].class
        );

        Map[] users = response.getBody();
        if (users != null && users.length > 0) {
            return (String) users[0].get("id");
        }
        return null;
    }

    private void updateUserInKeycloak(String userId, Map<String, Object> updates, String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> safeUpdates = new HashMap<>();
        if (updates.containsKey("firstName")) safeUpdates.put("firstName", updates.get("firstName"));
        if (updates.containsKey("lastName")) safeUpdates.put("lastName", updates.get("lastName"));

        if (!safeUpdates.isEmpty()) {
            try {
                restTemplate.exchange(
                        authServerUrl + "/admin/realms/" + realm + "/users/" + userId,
                        HttpMethod.PUT,
                        new HttpEntity<>(safeUpdates, headers),
                        String.class
                );
                log.info("✅ Utilisateur Keycloak mis à jour: {}", userId);
            } catch (Exception e) {
                throw new RuntimeException("Error updating user in Keycloak: " + e.getMessage());
            }
        }
    }

    private void updateEmailInKeycloak(String userId, String newEmail, String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> emailUpdate = new HashMap<>();
        emailUpdate.put("email", newEmail);
        emailUpdate.put("emailVerified", true);

        try {
            restTemplate.exchange(
                    authServerUrl + "/admin/realms/" + realm + "/users/" + userId,
                    HttpMethod.PUT,
                    new HttpEntity<>(emailUpdate, headers),
                    String.class
            );
            log.info("✅ Email updated in Keycloak: {}", userId);
        } catch (Exception e) {
            throw new RuntimeException("Error updating email in Keycloak: " + e.getMessage());
        }
    }

    private void sendResetPasswordEmail(String userId, String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        String redirectUri = "http://localhost:4200/tourists/reset-password-complete";
        String urlWithParams = authServerUrl + "/admin/realms/" + realm +
                "/users/" + userId + "/execute-actions-email" +
                "?client_id=" + clientId + "&redirect_uri=" + redirectUri;

        restTemplate.exchange(urlWithParams, HttpMethod.PUT,
                new HttpEntity<>(List.of("UPDATE_PASSWORD"), headers), String.class);
    }

    private void updatePasswordInKeycloak(String userId, String newPassword, String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> passwordData = new HashMap<>();
        passwordData.put("type", "password");
        passwordData.put("value", newPassword);
        passwordData.put("temporary", false);

        restTemplate.exchange(
                authServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/reset-password",
                HttpMethod.PUT,
                new HttpEntity<>(passwordData, headers),
                String.class
        );
    }

    private void deleteUserFromKeycloak(String userId, String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);

        try {
            restTemplate.exchange(
                    authServerUrl + "/admin/realms/" + realm + "/users/" + userId,
                    HttpMethod.DELETE,
                    new HttpEntity<>(headers),
                    String.class
            );
            log.info("✅ User deleted from Keycloak: {}", userId);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting user from Keycloak: " + e.getMessage());
        }
    }

    private void validatePasswordWithKeycloak(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("client_id", clientId);
        map.add("grant_type", "password");
        map.add("username", email);
        map.add("password", password);

        try {
            restTemplate.exchange(
                    authServerUrl + "/realms/" + realm + "/protocol/openid-connect/token",
                    HttpMethod.POST,
                    new HttpEntity<>(map, headers),
                    Map.class
            );
            log.info("✅ Password validated for: {}", email);
        } catch (Exception e) {
            throw new RuntimeException("Incorrect password");
        }
    }
}