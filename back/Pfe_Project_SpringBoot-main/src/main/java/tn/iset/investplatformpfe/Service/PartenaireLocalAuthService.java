package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tn.iset.investplatformpfe.Entity.Role;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PartenaireLocalAuthService {
    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    private final PartenaireLocalRepository partenaireRepository;
    private final RestTemplate restTemplate;

    public PartenaireLocalAuthService(PartenaireLocalRepository partenaireRepository) {
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

        // 1. Vérifier si l'email existe déjà
        if (partenaireRepository.existsByEmail(email)) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        try {
            // 2. Créer l'utilisateur dans Keycloak
            String userId = createUserInKeycloak(email, password, nom, prenom);

            // 3. Assigner le rôle LOCAL_PARTNER dans Keycloak
            assignRoleToUser(userId, "LOCAL_PARTNER");

            // 4. Créer dans MySQL
            PartenaireLocal newPartenaire = new PartenaireLocal();
            newPartenaire.setEmail(email);
            newPartenaire.setMotDePasse(password);
            newPartenaire.setNom(nom);
            newPartenaire.setPrenom(prenom);
            newPartenaire.setActif(true);
            newPartenaire.setRole(Role.LOCAL_PARTNER);
            newPartenaire.setDateInscription(LocalDateTime.now());
            newPartenaire.setStatut("actif");

            // Champs optionnels
            if (userData.containsKey("telephone")) {
                newPartenaire.setTelephone((String) userData.get("telephone"));
            }
            if (userData.containsKey("siteWeb")) {
                newPartenaire.setSiteWeb((String) userData.get("siteWeb"));
            }
            if (userData.containsKey("description")) {
                newPartenaire.setDescription((String) userData.get("description"));
            }
            if (userData.containsKey("domaineActivite")) {
                newPartenaire.setDomaineActivite((String) userData.get("domaineActivite"));
            }
            if (userData.containsKey("numeroRegistreCommerce")) {
                newPartenaire.setNumeroRegistreCommerce((String) userData.get("numeroRegistreCommerce"));
            }
            if (userData.containsKey("taxeProfessionnelle")) {
                newPartenaire.setTaxeProfessionnelle((String) userData.get("taxeProfessionnelle"));
            }

            PartenaireLocal savedPartenaire = partenaireRepository.save(newPartenaire);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Inscription réussie");
            response.put("id", savedPartenaire.getId());
            response.put("email", savedPartenaire.getEmail());
            response.put("nom", savedPartenaire.getNom());
            response.put("prenom", savedPartenaire.getPrenom());
            response.put("role", savedPartenaire.getRole());

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
    // METTRE À JOUR LE PROFIL
    // ========================================
    @Transactional
    public Map<String, Object> updateProfile(String email, Map<String, Object> userData) {

        PartenaireLocal existingPartenaire = partenaireRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Partenaire non trouvé"));

        if (userData.containsKey("nom")) {
            existingPartenaire.setNom((String) userData.get("nom"));
        }
        if (userData.containsKey("prenom")) {
            existingPartenaire.setPrenom((String) userData.get("prenom"));
        }
        if (userData.containsKey("telephone")) {
            existingPartenaire.setTelephone((String) userData.get("telephone"));
        }
        if (userData.containsKey("siteWeb")) {
            existingPartenaire.setSiteWeb((String) userData.get("siteWeb"));
        }
        if (userData.containsKey("description")) {
            existingPartenaire.setDescription((String) userData.get("description"));
        }
        if (userData.containsKey("domaineActivite")) {
            existingPartenaire.setDomaineActivite((String) userData.get("domaineActivite"));
        }
        if (userData.containsKey("numeroRegistreCommerce")) {
            existingPartenaire.setNumeroRegistreCommerce((String) userData.get("numeroRegistreCommerce"));
        }
        if (userData.containsKey("taxeProfessionnelle")) {
            existingPartenaire.setTaxeProfessionnelle((String) userData.get("taxeProfessionnelle"));
        }

        PartenaireLocal updated = partenaireRepository.save(existingPartenaire);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Profil mis à jour avec succès");
        response.put("email", updated.getEmail());
        response.put("nom", updated.getNom());
        response.put("prenom", updated.getPrenom());

        return response;
    }

    // ========================================
    // RÉCUPÉRER LE PROFIL
    // ========================================
    public Map<String, Object> getProfile(String email) {

        PartenaireLocal partenaire = partenaireRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Partenaire non trouvé"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", partenaire.getId());
        profile.put("email", partenaire.getEmail());
        profile.put("nom", partenaire.getNom());
        profile.put("prenom", partenaire.getPrenom());
        profile.put("telephone", partenaire.getTelephone());
        profile.put("siteWeb", partenaire.getSiteWeb());
        profile.put("description", partenaire.getDescription());
        profile.put("domaineActivite", partenaire.getDomaineActivite());
        profile.put("statut", partenaire.getStatut());
        profile.put("role", partenaire.getRole());

        return profile;
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

    // ========================================
// MOT DE PASSE OUBLIÉ - ENVOI D'EMAIL
// ========================================
    public Map<String, Object> forgotPassword(String email) {

        // 1. Vérifier si l'email existe dans la base de données
        PartenaireLocal partenaire = partenaireRepository.findByEmail(email)
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
// RÉINITIALISER LE MOT DE PASSE DIRECTEMENT
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
            PartenaireLocal partenaire = partenaireRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Partenaire non trouvé dans la base de données"));
            partenaire.setMotDePasse(newPassword);
            partenaireRepository.save(partenaire);

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
// ENVOYER L'EMAIL DE RÉINITIALISATION
// ========================================
    private void sendResetPasswordEmail(String userId, String adminToken) {
        String emailUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/execute-actions-email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        // Liste des actions requises (UPDATE_PASSWORD = réinitialiser le mot de passe)
        List<String> requiredActions = List.of("UPDATE_PASSWORD");

        // URL de redirection après réinitialisation (pour le frontend)
        String redirectUri = "http://localhost:4200/partenaires-locaux/reset-password-complete";

        // Construction de l'URL avec les paramètres
        String urlWithParams = emailUrl + "?client_id=" + clientId + "&redirect_uri=" + redirectUri;

        HttpEntity<List<String>> entity = new HttpEntity<>(requiredActions, headers);

        try {
            restTemplate.exchange(
                    urlWithParams,
                    HttpMethod.PUT,
                    entity,
                    String.class
            );

            System.out.println("📧 Email de réinitialisation envoyé au partenaire: " + userId);

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'envoi de l'email: " + e.getMessage());
        }
    }

    // ========================================
// RÉCUPÉRER L'ID UTILISATEUR PAR EMAIL
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

    // ========================================
// METTRE À JOUR LE MOT DE PASSE DANS KEYCLOAK
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
            System.out.println("🔑 Mot de passe mis à jour pour le partenaire: " + userId);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour du mot de passe: " + e.getMessage());
        }
    }
}
