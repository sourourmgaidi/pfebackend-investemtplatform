package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tn.iset.investplatformpfe.Entity.PartenaireEconomique;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Repository.PartenaireEconomiqueRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class PartenaireEconomiqueAuthService {
    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    private final PartenaireEconomiqueRepository partenaireRepository;
    private final RestTemplate restTemplate;

    public PartenaireEconomiqueAuthService(PartenaireEconomiqueRepository partenaireRepository) {
        this.partenaireRepository = partenaireRepository;
        this.restTemplate = new RestTemplate();
    }

    // ========================================
    // INSCRIPTION
    // ========================================
    @Transactional
    public Map<String, Object> register(Map<String, Object> userData) {

        String email = (String) userData.get("email");
        String password = (String) userData.get("password");
        String nom = (String) userData.get("nom");
        String prenom = (String) userData.get("prenom");

        // Vérifier les champs obligatoires
        if (email == null || password == null || nom == null || prenom == null) {
            throw new RuntimeException("Tous les champs obligatoires doivent être remplis");
        }

        // Vérifier si l'email existe déjà
        if (partenaireRepository.existsByEmail(email)) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        try {
            // Créer l'utilisateur dans Keycloak
            String userId = createUserInKeycloak(email, password, nom, prenom);

            // Assigner le rôle PARTNER dans Keycloak
            assignRoleToUser(userId, "PARTNER");

            // Créer dans MySQL
            PartenaireEconomique nouveauPartenaire = new PartenaireEconomique();
            nouveauPartenaire.setEmail(email);
            nouveauPartenaire.setMotDePasse(password);
            nouveauPartenaire.setNom(nom);
            nouveauPartenaire.setPrenom(prenom);
            nouveauPartenaire.setActif(true);
            nouveauPartenaire.setRole(Role.PARTNER);
            nouveauPartenaire.setDateInscription(LocalDateTime.now());

            // Champs optionnels
            if (userData.containsKey("telephone")) {
                nouveauPartenaire.setTelephone((String) userData.get("telephone"));
            }
            if (userData.containsKey("paysOrigine")) {
                nouveauPartenaire.setPaysOrigine((String) userData.get("paysOrigine"));
            }
            if (userData.containsKey("secteurActivite")) {
                nouveauPartenaire.setSecteurActivite((String) userData.get("secteurActivite"));
            }
            if (userData.containsKey("adresseSiege")) {
                nouveauPartenaire.setAdresseSiege((String) userData.get("adresseSiege"));
            }
            if (userData.containsKey("siteWeb")) {
                nouveauPartenaire.setSiteWeb((String) userData.get("siteWeb"));
            }
            if (userData.containsKey("photoProfil")) {
                nouveauPartenaire.setPhotoProfil((String) userData.get("photoProfil"));
            }

            PartenaireEconomique saved = partenaireRepository.save(nouveauPartenaire);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Inscription réussie");
            response.put("id", saved.getId());
            response.put("email", saved.getEmail());
            response.put("nom", saved.getNom());
            response.put("prenom", saved.getPrenom());
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

        PartenaireEconomique partenaire = partenaireRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Partenaire non trouvé"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", partenaire.getId());
        profile.put("email", partenaire.getEmail());
        profile.put("nom", partenaire.getNom());
        profile.put("prenom", partenaire.getPrenom());
        profile.put("telephone", partenaire.getTelephone());
        profile.put("paysOrigine", partenaire.getPaysOrigine());
        profile.put("secteurActivite", partenaire.getSecteurActivite());
        profile.put("adresseSiege", partenaire.getAdresseSiege());
        profile.put("siteWeb", partenaire.getSiteWeb());
        profile.put("role", partenaire.getRole());

        return profile;
    }

    // ========================================
    // METTRE À JOUR LE PROFIL (MySQL + Keycloak)
    // ========================================
    @Transactional
    public Map<String, Object> updateProfile(String email, Map<String, Object> userData) {

        PartenaireEconomique existing = partenaireRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Partenaire non trouvé"));

        try {
            String adminToken = getAdminToken();
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("Utilisateur non trouvé dans Keycloak");
            }

            Map<String, Object> keycloakUpdates = new HashMap<>();

            // Mise à jour MySQL et préparation Keycloak
            if (userData.containsKey("nom")) {
                String nouveauNom = (String) userData.get("nom");
                existing.setNom(nouveauNom);
                keycloakUpdates.put("lastName", nouveauNom);
            }
            if (userData.containsKey("prenom")) {
                String nouveauPrenom = (String) userData.get("prenom");
                existing.setPrenom(nouveauPrenom);
                keycloakUpdates.put("firstName", nouveauPrenom);
            }
            if (userData.containsKey("telephone")) {
                existing.setTelephone((String) userData.get("telephone"));
            }
            if (userData.containsKey("paysOrigine")) {
                existing.setPaysOrigine((String) userData.get("paysOrigine"));
            }
            if (userData.containsKey("secteurActivite")) {
                existing.setSecteurActivite((String) userData.get("secteurActivite"));
            }
            if (userData.containsKey("adresseSiege")) {
                existing.setAdresseSiege((String) userData.get("adresseSiege"));
            }
            if (userData.containsKey("siteWeb")) {
                existing.setSiteWeb((String) userData.get("siteWeb"));
            }
            if (userData.containsKey("photoProfil")) {
                existing.setPhotoProfil((String) userData.get("photoProfil"));
            }

            // Mettre à jour Keycloak si nécessaire
            if (!keycloakUpdates.isEmpty()) {
                updateUserInKeycloak(userId, keycloakUpdates, adminToken);
            }

            PartenaireEconomique updated = partenaireRepository.save(existing);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profil mis à jour avec succès");
            response.put("email", updated.getEmail());
            response.put("nom", updated.getNom());
            response.put("prenom", updated.getPrenom());

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour: " + e.getMessage());
        }
    }

    // ========================================
    // MOT DE PASSE OUBLIÉ
    // ========================================
    public Map<String, Object> forgotPassword(String email) {

        partenaireRepository.findByEmail(email)
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

            PartenaireEconomique partenaire = partenaireRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Partenaire non trouvé"));
            partenaire.setMotDePasse(newPassword);
            partenaireRepository.save(partenaire);

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

    private String createUserInKeycloak(String email, String password, String nom, String prenom) {
        String createUserUrl = authServerUrl + "/admin/realms/" + realm + "/users";

        String adminToken = getAdminToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        Map<String, Object> user = new HashMap<>();
        user.put("username", email);
        user.put("email", email);
        user.put("firstName", prenom);
        user.put("lastName", nom);
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

    private void sendResetPasswordEmail(String userId, String adminToken) {
        String emailUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/execute-actions-email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        List<String> requiredActions = List.of("UPDATE_PASSWORD");
        String redirectUri = "http://localhost:4200/partenaires-economiques/reset-password-complete";

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
