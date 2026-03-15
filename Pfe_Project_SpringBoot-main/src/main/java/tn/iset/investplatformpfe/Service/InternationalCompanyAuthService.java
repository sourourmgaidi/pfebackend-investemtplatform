package tn.iset.investplatformpfe.Service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Entity.internationalcompany;

import tn.iset.investplatformpfe.Repository.InternationalCompanyRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InternationalCompanyAuthService {

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

    private final InternationalCompanyRepository companyRepository;  // ✅ Nom du repository
    private final RestTemplate restTemplate;

    public InternationalCompanyAuthService(InternationalCompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
        this.restTemplate = new RestTemplate();
    }

    // ========================================
    // INSCRIPTION
    // ========================================
    @Transactional
    public Map<String, Object> register(Map<String, Object> userData) {

        String email = (String) userData.get("email");
        String password = (String) userData.get("password");
        String companyName = (String) userData.get("companyName");
        String contactLastName = (String) userData.get("contactLastName");
        String contactFirstName = (String) userData.get("contactFirstName");
        String phone = (String) userData.get("phone");
        String originCountry = (String) userData.get("originCountry");
        String activitySector = (String) userData.get("activitySector");
        String siret = (String) userData.get("siret");
        String interetPrincipal = (String) userData.get("interetPrincipal");

        // Vérifier les champs obligatoires
        if (email == null || password == null || companyName == null ||
                contactLastName == null || contactFirstName == null || phone == null ||
                originCountry == null || activitySector == null || siret == null ||
                interetPrincipal == null) {
            throw new RuntimeException("Tous les champs obligatoires doivent être remplis");
        }

        // Vérifier si l'email existe déjà
        if (companyRepository.existsByEmail(email)) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        // Vérifier si le SIRET existe déjà
        if (companyRepository.existsBySiret(siret)) {
            throw new RuntimeException("Ce numéro SIRET est déjà utilisé");
        }

        try {
            // Créer l'utilisateur dans Keycloak
            String userId = createUserInKeycloak(email, password, contactFirstName, contactLastName);

            // Assigner le rôle INTERNATIONAL_COMPANY dans Keycloak
            assignRoleToUser(userId, "INTERNATIONAL_COMPANY");

            // Créer dans MySQL
            internationalcompany newCompany = new internationalcompany();  // ✅ Nom de classe
            newCompany.setEmail(email);
            newCompany.setPassword(password);
            newCompany.setCompanyName(companyName);
            newCompany.setContactLastName(contactLastName);
            newCompany.setContactFirstName(contactFirstName);
            newCompany.setPhone(phone);
            newCompany.setOriginCountry(originCountry);
            newCompany.setActivitySector(activitySector);
            newCompany.setSiret(siret);
            newCompany.setInteretPrincipal(interetPrincipal);
            newCompany.setActive(true);
            newCompany.setRole(Role.INTERNATIONAL_COMPANY);
            newCompany.setRegistrationDate(LocalDateTime.now());

            // Champs optionnels
            if (userData.containsKey("website")) {
                newCompany.setWebsite((String) userData.get("website"));
            }
            if (userData.containsKey("linkedinProfile")) {
                newCompany.setLinkedinProfile((String) userData.get("linkedinProfile"));
            }
            if (userData.containsKey("profilePicture")) {
                newCompany.setProfilePicture((String) userData.get("profilePicture"));
            }

            internationalcompany saved = companyRepository.save(newCompany);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Inscription réussie");
            response.put("id", saved.getId());
            response.put("email", saved.getEmail());
            response.put("companyName", saved.getCompanyName());
            response.put("contactLastName", saved.getContactLastName());
            response.put("contactFirstName", saved.getContactFirstName());
            response.put("role", saved.getRole());

            return response;

        } catch (Exception e) {
            e.printStackTrace();
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
            throw new RuntimeException("Erreur de rafraîchissement: " + e.getMessage());
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
    // RÉCUPÉRER LE PROFIL
    // ========================================
    public Map<String, Object> getProfile(String email) {

        internationalcompany company = companyRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Entreprise non trouvée"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", company.getId());
        profile.put("email", company.getEmail());
        profile.put("companyName", company.getCompanyName());
        profile.put("contactLastName", company.getContactLastName());
        profile.put("contactFirstName", company.getContactFirstName());
        profile.put("phone", company.getPhone());
        profile.put("originCountry", company.getOriginCountry());
        profile.put("activitySector", company.getActivitySector());
        profile.put("siret", company.getSiret());
        profile.put("interetPrincipal", company.getInteretPrincipal());
        profile.put("website", company.getWebsite());
        profile.put("linkedinProfile", company.getLinkedinProfile());
        profile.put("profilePicture", company.getProfilePicture());
        profile.put("role", company.getRole());

        return profile;
    }

    // ========================================
    // METTRE À JOUR LE PROFIL
    // ========================================
    @Transactional
    public Map<String, Object> updateProfile(String email, Map<String, Object> userData) {

        internationalcompany existing = companyRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Entreprise non trouvée"));

        try {
            String adminToken = getAdminToken();
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("Utilisateur non trouvé dans Keycloak");
            }

            Map<String, Object> keycloakUpdates = new HashMap<>();

            // Mise à jour MySQL et préparation Keycloak
            if (userData.containsKey("companyName")) {
                existing.setCompanyName((String) userData.get("companyName"));
            }
            if (userData.containsKey("contactLastName")) {
                String newLastName = (String) userData.get("contactLastName");
                existing.setContactLastName(newLastName);
                keycloakUpdates.put("lastName", newLastName);
            }
            if (userData.containsKey("contactFirstName")) {
                String newFirstName = (String) userData.get("contactFirstName");
                existing.setContactFirstName(newFirstName);
                keycloakUpdates.put("firstName", newFirstName);
            }
            if (userData.containsKey("phone")) {
                existing.setPhone((String) userData.get("phone"));
            }
            if (userData.containsKey("originCountry")) {
                existing.setOriginCountry((String) userData.get("originCountry"));
            }
            if (userData.containsKey("activitySector")) {
                existing.setActivitySector((String) userData.get("activitySector"));
            }
            if (userData.containsKey("interetPrincipal")) {
                existing.setInteretPrincipal((String) userData.get("interetPrincipal"));
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

            // Mise à jour du mot de passe si fourni
            if (userData.containsKey("password")) {
                String newPassword = (String) userData.get("password");
                if (newPassword != null && !newPassword.isEmpty() && newPassword.length() >= 6) {
                    existing.setPassword(newPassword);
                    updatePasswordInKeycloak(userId, newPassword, adminToken);
                }
            }

            // Mettre à jour Keycloak si nécessaire
            if (!keycloakUpdates.isEmpty()) {
                updateUserInKeycloak(userId, keycloakUpdates, adminToken);
            }

            internationalcompany updated = companyRepository.save(existing);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profil mis à jour avec succès");
            response.put("email", updated.getEmail());
            response.put("companyName", updated.getCompanyName());
            response.put("contactLastName", updated.getContactLastName());
            response.put("contactFirstName", updated.getContactFirstName());

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la mise à jour: " + e.getMessage());
        }
    }

    // ========================================
    // MOT DE PASSE OUBLIÉ
    // ========================================
    public Map<String, Object> forgotPassword(String email) {

        companyRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Aucun compte trouvé avec cet email"));

        try {
            String adminToken = getAdminToken();
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("Utilisateur non trouvé dans Keycloak");
            }

            sendResetPasswordEmail(userId, adminToken);

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
    // RÉINITIALISER LE MOT DE PASSE
    // ========================================
    public Map<String, Object> resetPassword(String email, String newPassword) {

        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Le mot de passe doit contenir au moins 6 caractères");
        }

        try {
            String adminToken = getAdminToken();
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("Utilisateur non trouvé dans Keycloak");
            }

            updatePasswordInKeycloak(userId, newPassword, adminToken);

            internationalcompany company = companyRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Entreprise non trouvée"));
            company.setPassword(newPassword);
            companyRepository.save(company);

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
            throw new RuntimeException("Rôle " + roleName + " non trouvé dans Keycloak");
        }

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

    private void updateUserInKeycloak(String userId, Map<String, Object> updates, String adminToken) {
        String updateUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(updates, headers);

        restTemplate.exchange(updateUrl, HttpMethod.PUT, entity, String.class);
    }

    private void sendResetPasswordEmail(String userId, String adminToken) {
        String emailUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/execute-actions-email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        List<String> requiredActions = List.of("UPDATE_PASSWORD");
        String redirectUri = "http://localhost:4200/international-companies/reset-password-complete";

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
}
