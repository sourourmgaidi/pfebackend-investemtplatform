package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tn.iset.investplatformpfe.Entity.ActivityDomain;
import tn.iset.investplatformpfe.Entity.LocalPartner;
import tn.iset.investplatformpfe.Entity.Region;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Repository.LocalPartnerRepository;
import tn.iset.investplatformpfe.Repository.RegionRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LocalPartnerAuthService {

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    private final LocalPartnerRepository partnerRepository;
    private final RegionRepository regionRepository;  // ✅ AJOUTÉ
    private final RestTemplate restTemplate;

    public LocalPartnerAuthService(
            LocalPartnerRepository partnerRepository,
            RegionRepository regionRepository) {  // ✅ AJOUTÉ
        this.partnerRepository = partnerRepository;
        this.regionRepository = regionRepository;  // ✅ AJOUTÉ
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
    // INSCRIPTION - AVEC RÉGION
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
            assignRoleToUser(userId, "LOCAL_PARTNER");

            LocalPartner newPartner = new LocalPartner();
            newPartner.setEmail(email);
            newPartner.setPassword(password);
            newPartner.setLastName(lastName);
            newPartner.setFirstName(firstName);
            newPartner.setActive(true);
            newPartner.setRole(Role.LOCAL_PARTNER);
            newPartner.setRegistrationDate(LocalDateTime.now());

            // Téléphone
            if (userData.containsKey("telephone") && userData.get("telephone") != null) {
                newPartner.setPhone((String) userData.get("telephone"));
            }

            // Site web
            if (userData.containsKey("siteWeb") && userData.get("siteWeb") != null) {
                newPartner.setWebsite((String) userData.get("siteWeb"));
            }

            // Description
            if (userData.containsKey("description") && userData.get("description") != null) {
                newPartner.setDescription((String) userData.get("description"));
            }

            // ✅ RÉGION - Traitement complet
            if (userData.containsKey("region") && userData.get("region") != null) {
                Object regionObj = userData.get("region");
                Region region = null;

                if (regionObj instanceof String) {
                    // Si c'est le nom de la région (ex: "Tunis")
                    String regionName = (String) regionObj;
                    region = regionRepository.findByName(regionName).orElse(null);
                } else if (regionObj instanceof Number) {
                    // Si c'est l'ID de la région
                    Long regionId = ((Number) regionObj).longValue();
                    region = regionRepository.findById(regionId).orElse(null);
                } else if (regionObj instanceof Map) {
                    // Si c'est un objet région complet
                    Map<String, Object> regionMap = (Map<String, Object>) regionObj;
                    if (regionMap.containsKey("id")) {
                        Long regionId = ((Number) regionMap.get("id")).longValue();
                        region = regionRepository.findById(regionId).orElse(null);
                    } else if (regionMap.containsKey("name")) {
                        String regionName = (String) regionMap.get("name");
                        region = regionRepository.findByName(regionName).orElse(null);
                    }
                }

                if (region != null) {
                    newPartner.setRegion(region);
                }
            }

            // Adresse
            if (userData.containsKey("adresse") && userData.get("adresse") != null) {
                newPartner.setAddress((String) userData.get("adresse"));
            }

            // Domaine d'activité
            if (userData.containsKey("domaineActivite") && userData.get("domaineActivite") != null) {
                String activityStr = (String) userData.get("domaineActivite");
                try {
                    ActivityDomain activityDomain = ActivityDomain.valueOf(activityStr.toUpperCase());
                    newPartner.setActivityDomain(activityDomain);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid activity domain: " + activityStr);
                }
            }

            // Numéro registre commerce
            if (userData.containsKey("numeroRegistreCommerce") && userData.get("numeroRegistreCommerce") != null) {
                newPartner.setBusinessRegistrationNumber((String) userData.get("numeroRegistreCommerce"));
            }

            // Taxe professionnelle
            if (userData.containsKey("taxeProfessionnelle") && userData.get("taxeProfessionnelle") != null) {
                newPartner.setProfessionalTaxNumber((String) userData.get("taxeProfessionnelle"));
            }

            // LinkedIn
            if (userData.containsKey("linkedinProfile") && userData.get("linkedinProfile") != null) {
                newPartner.setLinkedinProfile((String) userData.get("linkedinProfile"));
            }

            LocalPartner saved = partnerRepository.save(newPartner);

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
    // RÉCUPÉRER LE PROFIL - COMPLET
    // ========================================
    public Map<String, Object> getProfile(String email) {
        LocalPartner partner = partnerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Partner not found"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", partner.getId());
        profile.put("email", partner.getEmail());
        profile.put("lastName", partner.getLastName());
        profile.put("firstName", partner.getFirstName());
        profile.put("telephone", partner.getPhone());
        profile.put("siteWeb", partner.getWebsite());
        profile.put("description", partner.getDescription());
        profile.put("adresse", partner.getAddress());
        profile.put("domaineActivite", partner.getActivityDomain() != null ? partner.getActivityDomain().name() : null);
        profile.put("statut", partner.getStatus());
        profile.put("role", partner.getRole());
        profile.put("linkedinProfile", partner.getLinkedinProfile());

        // ✅ RÉGION - avec getName()
        profile.put("region", partner.getRegion() != null ? partner.getRegion().getName() : null);
        profile.put("regionId", partner.getRegion() != null ? partner.getRegion().getId() : null);

        profile.put("numeroRegistreCommerce", partner.getBusinessRegistrationNumber());
        profile.put("taxeProfessionnelle", partner.getProfessionalTaxNumber());
        profile.put("photoProfil", partner.getProfilePhoto());

        return profile;
    }

    // ========================================
    // METTRE À JOUR LE PROFIL - AVEC RÉGION
    // ========================================
    @Transactional
    public Map<String, Object> updateProfile(String email, Map<String, Object> userData) {
        LocalPartner existing = partnerRepository.findByEmail(email)
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

            // GESTION DE L'EMAIL
            if (userData.containsKey("email")) {
                newEmail = (String) userData.get("email");

                if (!newEmail.equals(existing.getEmail())) {
                    if (!isGmail(newEmail)) {
                        throw new RuntimeException("The new email must be a valid Gmail address");
                    }
                    if (partnerRepository.existsByEmail(newEmail)) {
                        throw new RuntimeException("Email already in use: " + newEmail);
                    }
                    emailChanged = true;
                }
            }

            // Téléphone
            if (userData.containsKey("telephone")) {
                existing.setPhone((String) userData.get("telephone"));
            }

            // Site web
            if (userData.containsKey("siteWeb")) {
                existing.setWebsite((String) userData.get("siteWeb"));
            }

            // Description
            if (userData.containsKey("description")) {
                existing.setDescription((String) userData.get("description"));
            }

            // ✅ RÉGION - Mise à jour
            if (userData.containsKey("region")) {
                Object regionObj = userData.get("region");
                if (regionObj == null) {
                    existing.setRegion(null);
                } else {
                    Region region = null;

                    if (regionObj instanceof String) {
                        String regionName = (String) regionObj;
                        region = regionRepository.findByName(regionName).orElse(null);
                    } else if (regionObj instanceof Number) {
                        Long regionId = ((Number) regionObj).longValue();
                        region = regionRepository.findById(regionId).orElse(null);
                    } else if (regionObj instanceof Map) {
                        Map<String, Object> regionMap = (Map<String, Object>) regionObj;
                        if (regionMap.containsKey("id")) {
                            Long regionId = ((Number) regionMap.get("id")).longValue();
                            region = regionRepository.findById(regionId).orElse(null);
                        } else if (regionMap.containsKey("name")) {
                            String regionName = (String) regionMap.get("name");
                            region = regionRepository.findByName(regionName).orElse(null);
                        }
                    }

                    existing.setRegion(region);
                }
            }

            // Adresse
            if (userData.containsKey("adresse")) {
                existing.setAddress((String) userData.get("adresse"));
            }

            // Domaine d'activité
            if (userData.containsKey("domaineActivite") && userData.get("domaineActivite") != null) {
                String activityStr = (String) userData.get("domaineActivite");
                try {
                    ActivityDomain activityDomain = ActivityDomain.valueOf(activityStr.toUpperCase());
                    existing.setActivityDomain(activityDomain);
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid activity domain: " + activityStr);
                }
            }

            // LinkedIn
            if (userData.containsKey("linkedinProfile")) {
                existing.setLinkedinProfile((String) userData.get("linkedinProfile"));
            }

            // Documents
            if (userData.containsKey("numeroRegistreCommerce")) {
                existing.setBusinessRegistrationNumber((String) userData.get("numeroRegistreCommerce"));
            }
            if (userData.containsKey("taxeProfessionnelle")) {
                existing.setProfessionalTaxNumber((String) userData.get("taxeProfessionnelle"));
            }

            // Mettre à jour Keycloak
            if (!keycloakUpdates.isEmpty()) {
                updateUserInKeycloak(userId, keycloakUpdates, adminToken);
            }

            if (emailChanged) {
                updateEmailInKeycloak(userId, newEmail, adminToken);
                existing.setEmail(newEmail);
            }

            // Sauvegarder
            LocalPartner updated = partnerRepository.save(existing);

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

            LocalPartner partner = partnerRepository.findByEmail(email)
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

    private void updateUserInKeycloak(String userId, Map<String, Object> updates, String adminToken) {
        String updateUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(updates, headers);

        restTemplate.exchange(updateUrl, HttpMethod.PUT, entity, String.class);
    }

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
        String redirectUri = "http://localhost:4200/partenaires-locaux/reset-password-complete";

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
    // SUPPRESSION DE COMPTE PAR L'UTILISATEUR
    // ========================================
    @Transactional
    public Map<String, Object> deleteAccount(String email, String password) {

        LocalPartner partner = partnerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Partner not found in database"));

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

            deleteUserFromKeycloak(userId, adminToken);
            System.out.println("✅ User deleted from Keycloak: " + userId);

            partnerRepository.delete(partner);
            System.out.println("✅ User deleted from MySQL: " + email);

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
    // SUPPRESSION DE COMPTE PAR L'ADMIN
    // ========================================
    @Transactional
    public Map<String, Object> deleteAccountByAdmin(String email) {

        LocalPartner partner = partnerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Partner not found in database"));

        try {
            String adminToken = getAdminToken();
            String userId = getUserIdByEmail(email, adminToken);

            if (userId != null) {
                deleteUserFromKeycloak(userId, adminToken);
                System.out.println("✅ User deleted from Keycloak: " + userId);
            } else {
                System.out.println("⚠️ User not found in Keycloak, deleting only from MySQL");
            }

            partnerRepository.delete(partner);
            System.out.println("✅ User deleted from MySQL: " + email);

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
    // CHANGER LE MOT DE PASSE (PARTENAIRE LOCAL CONNECTÉ)
// ========================================
    @Transactional
    public Map<String, Object> changePassword(String email, String oldPassword, String newPassword) {

        // 1. Vérifier que l'utilisateur existe dans MySQL
        LocalPartner partner = partnerRepository.findByEmail(email)
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