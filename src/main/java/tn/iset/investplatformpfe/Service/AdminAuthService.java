package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tn.iset.investplatformpfe.Dto.UserSearchDTO;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Repository.*;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminAuthService {
    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    private final AdminRepository adminRepository;
    private final RestTemplate restTemplate;
    private final InvestorRepository investorRepository;
    private final TouristRepository touristRepository;
    private final EconomicPartnerRepository economicPartnerRepository;
    private final LocalPartnerRepository localPartnerRepository;
    private final InternationalCompanyRepository internationalCompanyRepository;

    public AdminAuthService(AdminRepository adminRepository,InvestorRepository investorRepository,
                            TouristRepository touristRepository,
                            EconomicPartnerRepository economicPartnerRepository,
                            LocalPartnerRepository localPartnerRepository,
                            InternationalCompanyRepository internationalCompanyRepository) {
        this.adminRepository = adminRepository;
        this.restTemplate = new RestTemplate();
        this.investorRepository = investorRepository;
        this.touristRepository = touristRepository;
        this.economicPartnerRepository = economicPartnerRepository;
        this.localPartnerRepository = localPartnerRepository;
        this.internationalCompanyRepository = internationalCompanyRepository;
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
    // INSCRIPTION AVEC VALIDATION GMAIL
    // ========================================
    @Transactional
    public Map<String, Object> register(Map<String, Object> userData) {

        String email = (String) userData.get("email");
        String password = (String) userData.get("password");
        String lastName = (String) userData.get("lastName");
        String firstName = (String) userData.get("firstName");

        // Vérifier les champs obligatoires
        if (email == null || password == null || lastName == null || firstName == null) {
            throw new RuntimeException("Tous les champs obligatoires doivent être remplis");
        }

        // ✅ VALIDATION GMAIL
        if (!isGmail(email)) {
            throw new RuntimeException("Seules les adresses Gmail sont autorisées. Veuillez utiliser une adresse Gmail valide (ex: @gmail.com, @gmail.fr, etc.)");
        }

        // Vérifier si l'email existe déjà
        if (adminRepository.existsByEmail(email)) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        try {
            // Créer l'utilisateur dans Keycloak
            String userId = createUserInKeycloak(email, password, lastName, firstName);

            // Assigner le rôle ADMIN dans Keycloak
            assignRoleToUser(userId, "ADMIN");

            // Créer dans MySQL
            Admin newAdmin = new Admin();
            newAdmin.setEmail(email);
            newAdmin.setPassword(password);
            newAdmin.setLastName(lastName);
            newAdmin.setFirstName(firstName);
            newAdmin.setActive(true);
            newAdmin.setRole(Role.ADMIN);
            newAdmin.setRegistrationDate(LocalDateTime.now());

            // Champs optionnels
            if (userData.containsKey("phone")) {
                newAdmin.setPhone((String) userData.get("phone"));
            }
            if (userData.containsKey("profilePhoto")) {
                newAdmin.setProfilePhoto((String) userData.get("profilePhoto"));
            }

            Admin saved = adminRepository.save(newAdmin);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Inscription réussie");
            response.put("id", saved.getId());
            response.put("email", saved.getEmail());
            response.put("lastName", saved.getLastName());
            response.put("firstName", saved.getFirstName());
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

        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", admin.getId());
        profile.put("email", admin.getEmail());
        profile.put("lastName", admin.getLastName());
        profile.put("firstName", admin.getFirstName());
        profile.put("phone", admin.getPhone());
        profile.put("profilePhoto", admin.getProfilePhoto());
        profile.put("role", admin.getRole());

        return profile;
    }

    // ========================================
    // METTRE À JOUR LE PROFIL (MySQL + Keycloak) - VERSION CORRIGÉE
    // ========================================
    @Transactional
    public Map<String, Object> updateProfile(String email, Map<String, Object> userData) {

        Admin existing = adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        try {
            String adminToken = getAdminToken();
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("Utilisateur non trouvé dans Keycloak");
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

            // GESTION SPÉCIALE POUR L'EMAIL
            if (userData.containsKey("email")) {
                newEmail = (String) userData.get("email");

                // Vérifier si le nouvel email est différent de l'ancien
                if (!newEmail.equals(existing.getEmail())) {
                    // ✅ VALIDATION GMAIL POUR LE NOUVEL EMAIL
                    if (!isGmail(newEmail)) {
                        throw new RuntimeException("Le nouvel email doit être une adresse Gmail valide");
                    }

                    // Vérifier que le nouvel email n'est pas déjà utilisé
                    if (adminRepository.existsByEmail(newEmail)) {
                        throw new RuntimeException("Cet email est déjà utilisé: " + newEmail);
                    }
                    emailChanged = true;
                }
            }

            // Mise à jour du téléphone
            if (userData.containsKey("phone")) {
                existing.setPhone((String) userData.get("phone"));
            }

            // Mise à jour de la photo de profil
            if (userData.containsKey("profilePhoto")) {
                existing.setProfilePhoto((String) userData.get("profilePhoto"));
            }

            // 1. Mettre à jour Keycloak avec les modifications standards (nom, prénom)
            if (!keycloakUpdates.isEmpty()) {
                updateUserInKeycloak(userId, keycloakUpdates, adminToken);
            }

            // 2. Gérer le changement d'email dans Keycloak SI NÉCESSAIRE
            if (emailChanged) {
                // Mettre à jour l'email dans Keycloak (sans toucher au username)
                updateEmailInKeycloak(userId, newEmail, adminToken);
                existing.setEmail(newEmail); // Mettre à jour l'email APRÈS succès Keycloak
            }

            // 3. Sauvegarder dans MySQL
            Admin updated = adminRepository.save(existing);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profil mis à jour avec succès");
            response.put("email", updated.getEmail());
            response.put("lastName", updated.getLastName());
            response.put("firstName", updated.getFirstName());

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

        adminRepository.findByEmail(email)
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

            Admin admin = adminRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Admin non trouvé"));
            admin.setPassword(newPassword);
            adminRepository.save(admin);

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

    private String createUserInKeycloak(String email, String password, String lastName, String firstName) {
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

    // ========================================
    // NOUVELLE MÉTHODE: Mettre à jour l'email dans Keycloak
    // ========================================
    private void updateEmailInKeycloak(String userId, String newEmail, String adminToken) {
        String updateUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        // Ne modifier QUE l'email, pas le username (protégé)
        Map<String, Object> emailUpdate = new HashMap<>();
        emailUpdate.put("email", newEmail);
        emailUpdate.put("emailVerified", true);
        // ⚠️ NE PAS AJOUTER "username"

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(emailUpdate, headers);

        restTemplate.exchange(updateUrl, HttpMethod.PUT, entity, String.class);
    }

    private void sendResetPasswordEmail(String userId, String adminToken) {
        String emailUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/execute-actions-email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        List<String> requiredActions = List.of("UPDATE_PASSWORD");
        String redirectUri = "http://localhost:4200/admin/reset-password-complete";

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
// ✅ SUPPRESSION DU PROPRE COMPTE DE L'ADMIN (AVEC MOT DE PASSE)
// ========================================
    @Transactional
    public Map<String, Object> deleteOwnAccount(String email, String password) {

        // 1. Vérifier que l'admin existe dans MySQL
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé dans la base de données"));

        try {
            // 2. Obtenir un token admin pour Keycloak
            String adminToken = getAdminToken();

            // 3. Récupérer l'ID de l'utilisateur dans Keycloak
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("Utilisateur non trouvé dans Keycloak");
            }

            // 4. Valider le mot de passe (pour confirmer l'identité)
            try {
                validatePasswordWithKeycloak(email, password);
            } catch (Exception e) {
                throw new RuntimeException("Mot de passe incorrect. Suppression annulée.");
            }

            // 5. Supprimer l'utilisateur de Keycloak
            deleteUserFromKeycloak(userId, adminToken);
            System.out.println("✅ Admin supprimé de Keycloak: " + userId);

            // 6. Supprimer l'utilisateur de MySQL
            adminRepository.delete(admin);
            System.out.println("✅ Admin supprimé de MySQL: " + email);

            // 7. Préparer la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Votre compte admin a été supprimé avec succès");
            response.put("email", email);

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la suppression de votre compte: " + e.getMessage());
        }
    }

    // ========================================
// ✅ SUPPRESSION D'UN COMPTE UTILISATEUR (PAR L'ADMIN)
// ========================================
    @Transactional
    public Map<String, Object> deleteUserAccount(String userEmail, String userType, String adminEmail) {

        // Vérifier que l'admin qui fait la demande existe (optionnel)
        Admin admin = adminRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        try {
            // 1. Obtenir un token admin pour Keycloak
            String adminToken = getAdminToken();

            // 2. Récupérer l'ID de l'utilisateur dans Keycloak
            String userId = getUserIdByEmail(userEmail, adminToken);

            if (userId != null) {
                // 3. Supprimer l'utilisateur de Keycloak
                deleteUserFromKeycloak(userId, adminToken);
                System.out.println("✅ Utilisateur supprimé de Keycloak: " + userId);
            } else {
                System.out.println("⚠️ Utilisateur non trouvé dans Keycloak: " + userEmail);
            }

            // 4. NOTE: La suppression de la base de données MySQL sera gérée par les services spécifiques
            // Cette méthode ne fait que la partie Keycloak. Pour la suppression MySQL,
            // il faudrait appeler les repositories spécifiques (InvestorRepository, etc.)

            // 5. Préparer la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Compte utilisateur supprimé de Keycloak avec succès");
            response.put("email", userEmail);
            response.put("userType", userType);
            response.put("admin", adminEmail);

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la suppression du compte utilisateur: " + e.getMessage());
        }
    }

// ========================================
// ✅ MÉTHODES PRIVÉES UTILITAIRES
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
            System.out.println("✅ Utilisateur supprimé de Keycloak: " + userId);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la suppression de Keycloak: " + e.getMessage());
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
            System.out.println("✅ Mot de passe validé pour: " + email);
        } catch (Exception e) {
            throw new RuntimeException("Mot de passe incorrect");
        }
    }
    // ========================================
// RÉCUPÉRER TOUS LES UTILISATEURS (AVEC RECHERCHE OPTIONNELLE)
// ========================================
    public Map<String, Object> getAllUsers(String searchTerm) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> allUsers = new ArrayList<>();

        try {
            // ========================================
            // 1. Récupérer les investisseurs
            // ========================================
            List<Investor> investors;
            if (searchTerm != null && !searchTerm.isEmpty()) {
                investors = investorRepository.findByFirstNameContainingOrLastNameContainingOrEmailContaining(
                        searchTerm, searchTerm, searchTerm);
            } else {
                investors = investorRepository.findAll();
            }

            for (Investor investor : investors) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", investor.getId());
                userMap.put("email", investor.getEmail());
                userMap.put("firstName", investor.getFirstName());
                userMap.put("lastName", investor.getLastName());
                userMap.put("role", "INVESTOR");
                userMap.put("phone", investor.getPhone());
                userMap.put("company", investor.getCompany());
                userMap.put("originCountry", investor.getOriginCountry());

                // ✅ Gestion robuste : vérifier null ET valeur vide
                String activitySector = null;
                if (investor.getActivitySector() != null) {
                    try {
                        activitySector = investor.getActivitySector().name();
                    } catch (Exception e) {
                        System.err.println("⚠️ Erreur enum pour investisseur " + investor.getEmail() + ": " + e.getMessage());
                        activitySector = null;
                    }
                }
                userMap.put("activitySector", activitySector);

                userMap.put("profilePicture", investor.getProfilePicture());
                userMap.put("nationality", investor.getNationality());
                userMap.put("website", investor.getWebsite());
                userMap.put("linkedinProfile", investor.getLinkedinProfile());
                userMap.put("active", investor.getActive());
                userMap.put("registrationDate", investor.getRegistrationDate() != null ?
                        investor.getRegistrationDate().toString() : null);
                allUsers.add(userMap);
            }

            // ========================================
            // 2. Récupérer les sociétés internationales
            // ========================================
            List<internationalcompany> companies;
            if (searchTerm != null && !searchTerm.isEmpty()) {
                companies = internationalCompanyRepository.findByCompanyNameContainingOrEmailContainingOrContactFirstNameContaining(
                        searchTerm, searchTerm, searchTerm);
            } else {
                companies = internationalCompanyRepository.findAll();
            }

            for (internationalcompany company : companies) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", company.getId());
                userMap.put("email", company.getEmail());
                userMap.put("firstName", company.getContactFirstName());
                userMap.put("lastName", company.getContactLastName());
                userMap.put("role", "INTERNATIONAL_COMPANY");
                userMap.put("phone", company.getPhone());
                userMap.put("companyName", company.getCompanyName());
                userMap.put("originCountry", company.getOriginCountry());

                // ✅ Gestion robuste
                String activitySector = null;
                if (company.getActivitySector() != null) {
                    try {
                        activitySector = company.getActivitySector().name();
                    } catch (Exception e) {
                        System.err.println("⚠️ Erreur enum pour société " + company.getEmail() + ": " + e.getMessage());
                        activitySector = null;
                    }
                }
                userMap.put("activitySector", activitySector);

                userMap.put("siret", company.getSiret());
                userMap.put("website", company.getWebsite());
                userMap.put("linkedinProfile", company.getLinkedinProfile());
                userMap.put("profilePicture", company.getProfilePicture());
                userMap.put("active", company.getActive());
                userMap.put("registrationDate", company.getRegistrationDate() != null ?
                        company.getRegistrationDate().toString() : null);
                allUsers.add(userMap);
            }

            // ========================================
            // 3. Récupérer les partenaires économiques
            // ========================================
            List<EconomicPartner> partners;
            if (searchTerm != null && !searchTerm.isEmpty()) {
                partners = economicPartnerRepository.findByFirstNameContainingOrLastNameContainingOrEmailContaining(
                        searchTerm, searchTerm, searchTerm);
            } else {
                partners = economicPartnerRepository.findAll();
            }

            for (EconomicPartner partner : partners) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", partner.getId());
                userMap.put("email", partner.getEmail());
                userMap.put("firstName", partner.getFirstName());
                userMap.put("lastName", partner.getLastName());
                userMap.put("role", "PARTNER");
                userMap.put("phone", partner.getPhone());
                userMap.put("countryOfOrigin", partner.getCountryOfOrigin());

                // ✅ Gestion robuste
                String businessSector = null;
                if (partner.getBusinessSector() != null) {
                    try {
                        businessSector = partner.getBusinessSector().name();
                    } catch (Exception e) {
                        System.err.println("⚠️ Erreur enum pour partenaire " + partner.getEmail() + ": " + e.getMessage());
                        businessSector = null;
                    }
                }
                userMap.put("businessSector", businessSector);

                userMap.put("headquartersAddress", partner.getHeadquartersAddress());
                userMap.put("website", partner.getWebsite());
                userMap.put("linkedinProfile", partner.getLinkedinProfile());
                userMap.put("profilePhoto", partner.getProfilePhoto());
                userMap.put("active", partner.getActive());
                userMap.put("registrationDate", partner.getRegistrationDate() != null ?
                        partner.getRegistrationDate().toString() : null);
                allUsers.add(userMap);
            }

            // ========================================
            // 4. Récupérer les partenaires locaux
            // ========================================
            List<LocalPartner> localPartners;
            if (searchTerm != null && !searchTerm.isEmpty()) {
                localPartners = localPartnerRepository.findByFirstNameContainingOrLastNameContainingOrEmailContaining(
                        searchTerm, searchTerm, searchTerm);
            } else {
                localPartners = localPartnerRepository.findAll();
            }

            for (LocalPartner partner : localPartners) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", partner.getId());
                userMap.put("email", partner.getEmail());
                userMap.put("firstName", partner.getFirstName());
                userMap.put("lastName", partner.getLastName());
                userMap.put("role", "LOCAL_PARTNER");
                userMap.put("phone", partner.getPhone());
                userMap.put("website", partner.getWebsite());
                userMap.put("description", partner.getDescription());
                userMap.put("status", partner.getStatus());

                // ✅ Gestion robuste
                String activityDomain = null;
                if (partner.getActivityDomain() != null) {
                    try {
                        activityDomain = partner.getActivityDomain().name();
                    } catch (Exception e) {
                        System.err.println("⚠️ Erreur enum pour partenaire local " + partner.getEmail() + ": " + e.getMessage());
                        activityDomain = null;
                    }
                }
                userMap.put("activityDomain", activityDomain);

                userMap.put("businessRegistrationNumber", partner.getBusinessRegistrationNumber());
                userMap.put("professionalTaxNumber", partner.getProfessionalTaxNumber());
                userMap.put("linkedinProfile", partner.getLinkedinProfile());
                userMap.put("profilePhoto", partner.getProfilePhoto());
                userMap.put("address", partner.getAddress());
                userMap.put("region", partner.getRegion() != null ? partner.getRegion().getName() : null);
                userMap.put("active", partner.getActive());
                userMap.put("registrationDate", partner.getRegistrationDate() != null ?
                        partner.getRegistrationDate().toString() : null);
                allUsers.add(userMap);
            }

            // ========================================
            // 5. Récupérer les touristes
            // ========================================
            List<Tourist> tourists;
            if (searchTerm != null && !searchTerm.isEmpty()) {
                tourists = touristRepository.findByFirstNameContainingOrLastNameContainingOrEmailContaining(
                        searchTerm, searchTerm, searchTerm);
            } else {
                tourists = touristRepository.findAll();
            }

            for (Tourist tourist : tourists) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", tourist.getId());
                userMap.put("email", tourist.getEmail());
                userMap.put("firstName", tourist.getFirstName());
                userMap.put("lastName", tourist.getLastName());
                userMap.put("role", "TOURIST");
                userMap.put("phone", tourist.getPhone());
                userMap.put("nationality", tourist.getNationality());
                userMap.put("profilePhoto", tourist.getProfilePhoto());
                userMap.put("active", tourist.getActive());
                userMap.put("registrationDate", tourist.getRegistrationDate() != null ?
                        tourist.getRegistrationDate().toString() : null);
                allUsers.add(userMap);
            }

            // ========================================
            // 6. Récupérer les admins
            // ========================================
            List<Admin> admins = adminRepository.findAll();
            for (Admin admin : admins) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", admin.getId());
                userMap.put("email", admin.getEmail());
                userMap.put("firstName", admin.getFirstName());
                userMap.put("lastName", admin.getLastName());
                userMap.put("role", "ADMIN");
                userMap.put("phone", admin.getPhone());
                userMap.put("profilePhoto", admin.getProfilePhoto());
                userMap.put("active", admin.getActive());
                userMap.put("registrationDate", admin.getRegistrationDate() != null ?
                        admin.getRegistrationDate().toString() : null);
                allUsers.add(userMap);
            }

            response.put("success", true);
            response.put("users", allUsers);
            response.put("total", allUsers.size());
            response.put("message", "Utilisateurs récupérés avec succès");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Erreur dans getAllUsers: " + e.getMessage());
            System.err.println("❌ Cause: " + (e.getCause() != null ? e.getCause().getMessage() : "Inconnue"));

            throw new RuntimeException("Erreur lors de la récupération des utilisateurs: " + e.getMessage());
        }

        return response;
    }

    // ========================================
// SUPPRIMER UN UTILISATEUR COMPLÈTEMENT (MySQL + Keycloak)
// ========================================
    @Transactional
    public Map<String, Object> deleteUserCompletely(String userEmail, String adminEmail) {

        // Vérifier que l'admin existe
        Admin admin = adminRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

        // Empêcher la suppression d'un autre admin
        if (adminRepository.existsByEmail(userEmail)) {
            throw new RuntimeException("Impossible de supprimer un compte admin");
        }

        try {
            String adminToken = getAdminToken();
            String userId = getUserIdByEmail(userEmail, adminToken);
            boolean deleted = false;
            String userType = "";

            // 1. Chercher et supprimer dans Investor
            Optional<Investor> investor = investorRepository.findByEmail(userEmail);
            if (investor.isPresent()) {
                if (userId != null) deleteUserFromKeycloak(userId, adminToken);
                investorRepository.delete(investor.get());
                deleted = true;
                userType = "INVESTOR";
                System.out.println("✅ Investisseur supprimé: " + userEmail);
            }

            // 2. Chercher et supprimer dans Tourist
            if (!deleted) {
                Optional<Tourist> tourist = touristRepository.findByEmail(userEmail);
                if (tourist.isPresent()) {
                    if (userId != null) deleteUserFromKeycloak(userId, adminToken);
                    touristRepository.delete(tourist.get());
                    deleted = true;
                    userType = "TOURIST";
                    System.out.println("✅ Touriste supprimé: " + userEmail);
                }
            }

            // 3. Chercher et supprimer dans EconomicPartner
            if (!deleted) {
                Optional<EconomicPartner> partner = economicPartnerRepository.findByEmail(userEmail);
                if (partner.isPresent()) {
                    if (userId != null) deleteUserFromKeycloak(userId, adminToken);
                    economicPartnerRepository.delete(partner.get());
                    deleted = true;
                    userType = "PARTNER";
                    System.out.println("✅ Partenaire économique supprimé: " + userEmail);
                }
            }

            // 4. Chercher et supprimer dans LocalPartner
            if (!deleted) {
                Optional<LocalPartner> localPartner = localPartnerRepository.findByEmail(userEmail);
                if (localPartner.isPresent()) {
                    if (userId != null) deleteUserFromKeycloak(userId, adminToken);
                    localPartnerRepository.delete(localPartner.get());
                    deleted = true;
                    userType = "LOCAL_PARTNER";
                    System.out.println("✅ Partenaire local supprimé: " + userEmail);
                }
            }

            // 5. Chercher et supprimer dans InternationalCompany
            if (!deleted) {
                Optional<internationalcompany> company = internationalCompanyRepository.findByEmail(userEmail);
                if (company.isPresent()) {
                    if (userId != null) deleteUserFromKeycloak(userId, adminToken);
                    internationalCompanyRepository.delete(company.get());
                    deleted = true;
                    userType = "INTERNATIONAL_COMPANY";
                    System.out.println("✅ Société internationale supprimée: " + userEmail);
                }
            }

            if (!deleted) {
                throw new RuntimeException("Aucun utilisateur trouvé avec l'email: " + userEmail);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Utilisateur supprimé avec succès (MySQL + Keycloak)");
            response.put("email", userEmail);
            response.put("userType", userType);
            response.put("admin", adminEmail);

            return response;

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la suppression complète: " + e.getMessage());
        }
    }

    // ========================================
// RECHERCHER DES UTILISATEURS PAR CRITÈRES
// ========================================
    public Map<String, Object> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllUsers(null);
        }
        return getAllUsers(keyword);
    }

    // ========================================
// OBTENIR LES STATISTIQUES DES UTILISATEURS
// ========================================
    public Map<String, Object> getUserStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            stats.put("totalInvestors", investorRepository.count());
            stats.put("totalTourists", touristRepository.count());
            stats.put("totalPartners", economicPartnerRepository.count());
            stats.put("totalLocalPartners", localPartnerRepository.count());
            stats.put("totalInternationalCompanies", internationalCompanyRepository.count());
            stats.put("totalAdmins", adminRepository.count());

            long total = investorRepository.count() + touristRepository.count() +
                    economicPartnerRepository.count() + localPartnerRepository.count() +
                    internationalCompanyRepository.count() + adminRepository.count();

            stats.put("totalUsers", total);
            stats.put("success", true);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors du calcul des statistiques: " + e.getMessage());
        }

        return stats;
    }
    // CHANGER LE MOT DE PASSE (ADMIN CONNECTÉ)
// ========================================
    @Transactional
    public Map<String, Object> changePassword(String email, String oldPassword, String newPassword) {

        // 1. Vérifier que l'admin existe
        Admin admin = adminRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Admin non trouvé"));

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
            System.out.println("✅ Mot de passe mis à jour dans Keycloak pour l'admin: " + email);

            // 7. Mettre à jour le mot de passe dans MySQL
            admin.setPassword(newPassword);
            adminRepository.save(admin);
            System.out.println("✅ Mot de passe mis à jour dans MySQL pour l'admin: " + email);

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


    // ========================================
// RECHERCHER DES UTILISATEURS POUR LE CHAT
// ========================================
    public List<UserSearchDTO> searchUsersForChat(String query, String currentUserEmail) {
        List<UserSearchDTO> results = new ArrayList<>();

        try {
            // 1. Rechercher dans les investisseurs
            List<Investor> investors = investorRepository.findByFirstNameContainingOrLastNameContainingOrEmailContaining(
                    query, query, query);
            for (Investor inv : investors) {
                if (!inv.getEmail().equals(currentUserEmail)) {
                    results.add(new UserSearchDTO(
                            inv.getId(),
                            inv.getEmail(),
                            inv.getFirstName(),
                            inv.getLastName(),
                            "INVESTOR",
                            inv.getProfilePicture()
                    ));
                }
            }

            // 2. Rechercher dans les touristes
            List<Tourist> tourists = touristRepository.findByFirstNameContainingOrLastNameContainingOrEmailContaining(
                    query, query, query);
            for (Tourist tour : tourists) {
                if (!tour.getEmail().equals(currentUserEmail)) {
                    results.add(new UserSearchDTO(
                            tour.getId(),
                            tour.getEmail(),
                            tour.getFirstName(),
                            tour.getLastName(),
                            "TOURIST",
                            tour.getProfilePhoto()
                    ));
                }
            }

            // 3. Rechercher dans les partenaires économiques
            List<EconomicPartner> partners = economicPartnerRepository.findByFirstNameContainingOrLastNameContainingOrEmailContaining(
                    query, query, query);
            for (EconomicPartner partner : partners) {
                if (!partner.getEmail().equals(currentUserEmail)) {
                    results.add(new UserSearchDTO(
                            partner.getId(),
                            partner.getEmail(),
                            partner.getFirstName(),
                            partner.getLastName(),
                            "PARTNER",
                            partner.getProfilePhoto()
                    ));
                }
            }

            // 4. Rechercher dans les partenaires locaux
            List<LocalPartner> localPartners = localPartnerRepository.findByFirstNameContainingOrLastNameContainingOrEmailContaining(
                    query, query, query);
            for (LocalPartner lp : localPartners) {
                if (!lp.getEmail().equals(currentUserEmail)) {
                    results.add(new UserSearchDTO(
                            lp.getId(),
                            lp.getEmail(),
                            lp.getFirstName(),
                            lp.getLastName(),
                            "LOCAL_PARTNER",
                            lp.getProfilePhoto()
                    ));
                }
            }

            // 5. Rechercher dans les sociétés internationales
            List<internationalcompany> companies = internationalCompanyRepository.findByCompanyNameContainingOrEmailContainingOrContactFirstNameContaining(
                    query, query, query);
            for (internationalcompany comp : companies) {
                if (!comp.getEmail().equals(currentUserEmail)) {
                    results.add(new UserSearchDTO(
                            comp.getId(),
                            comp.getEmail(),
                            comp.getContactFirstName(),
                            comp.getContactLastName(),
                            "INTERNATIONAL_COMPANY",
                            comp.getProfilePicture()
                    ));
                }
            }

            // 6. Rechercher dans les admins (optionnel, selon votre besoin)
            List<Admin> admins = adminRepository.findByFirstNameContainingOrLastNameContainingOrEmailContaining(
                    query, query, query);
            for (Admin admin : admins) {
                if (!admin.getEmail().equals(currentUserEmail)) {
                    results.add(new UserSearchDTO(
                            admin.getId(),
                            admin.getEmail(),
                            admin.getFirstName(),
                            admin.getLastName(),
                            "ADMIN",
                            admin.getProfilePhoto()
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la recherche d'utilisateurs: " + e.getMessage());
        }

        return results;
    }

    // ========================================
// RÉCUPÉRER TOUS LES UTILISATEURS POUR LE CHAT
// ========================================
    public List<UserSearchDTO> getAllUsersForChat(String currentUserEmail) {
        List<UserSearchDTO> results = new ArrayList<>();

        try {
            // Récupérer tous les utilisateurs de chaque type
            for (Investor inv : investorRepository.findAll()) {
                if (!inv.getEmail().equals(currentUserEmail)) {
                    results.add(new UserSearchDTO(
                            inv.getId(), inv.getEmail(), inv.getFirstName(), inv.getLastName(),
                            "INVESTOR", inv.getProfilePicture()));
                }
            }

            for (Tourist tour : touristRepository.findAll()) {
                if (!tour.getEmail().equals(currentUserEmail)) {
                    results.add(new UserSearchDTO(
                            tour.getId(), tour.getEmail(), tour.getFirstName(), tour.getLastName(),
                            "TOURIST", tour.getProfilePhoto()));
                }
            }

            for (EconomicPartner partner : economicPartnerRepository.findAll()) {
                if (!partner.getEmail().equals(currentUserEmail)) {
                    results.add(new UserSearchDTO(
                            partner.getId(), partner.getEmail(), partner.getFirstName(), partner.getLastName(),
                            "PARTNER", partner.getProfilePhoto()));
                }
            }

            for (LocalPartner lp : localPartnerRepository.findAll()) {
                if (!lp.getEmail().equals(currentUserEmail)) {
                    results.add(new UserSearchDTO(
                            lp.getId(), lp.getEmail(), lp.getFirstName(), lp.getLastName(),
                            "LOCAL_PARTNER", lp.getProfilePhoto()));
                }
            }

            for (internationalcompany comp : internationalCompanyRepository.findAll()) {
                if (!comp.getEmail().equals(currentUserEmail)) {
                    results.add(new UserSearchDTO(
                            comp.getId(), comp.getEmail(), comp.getContactFirstName(), comp.getContactLastName(),
                            "INTERNATIONAL_COMPANY", comp.getProfilePicture()));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des utilisateurs: " + e.getMessage());
        }

        return results;
    }

    // ========================================
// RÉCUPÉRER LES ADMINS POUR LE CHAT
// ========================================
    public List<UserSearchDTO> getAdminsForChat(String currentUserEmail) {
        List<UserSearchDTO> results = new ArrayList<>();

        try {
            List<Admin> admins = adminRepository.findAll();
            for (Admin admin : admins) {
                if (!admin.getEmail().equals(currentUserEmail)) {
                    results.add(new UserSearchDTO(
                            admin.getId(),
                            admin.getEmail(),
                            admin.getFirstName(),
                            admin.getLastName(),
                            "ADMIN",
                            admin.getProfilePhoto()
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la récupération des admins: " + e.getMessage());
        }

        return results;
    }

}

