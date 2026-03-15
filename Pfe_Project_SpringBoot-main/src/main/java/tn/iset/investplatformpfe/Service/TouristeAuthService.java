package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Entity.Touriste;
import tn.iset.investplatformpfe.Repository.TouristeRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TouristeAuthService {

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.resource}")
    private String clientId;

    private final TouristeRepository touristeRepository;
    private final RestTemplate restTemplate;

    public TouristeAuthService(TouristeRepository touristeRepository) {
        this.touristeRepository = touristeRepository;
        this.restTemplate = new RestTemplate();
    }

    // ========================================
    // INSCRIPTION SIMPLIFIÉE
    // ========================================
    @Transactional
    public Map<String, Object> register(Map<String, Object> userData) {

        String email = (String) userData.get("email");
        String password = (String) userData.get("password");
        String nom = (String) userData.get("nom");
        String prenom = (String) userData.get("prenom");

        // 1. Vérifier les champs obligatoires
        if (email == null || password == null || nom == null || prenom == null) {
            throw new RuntimeException("Tous les champs obligatoires doivent être remplis");
        }

        // 2. Vérifier si l'email existe déjà
        if (touristeRepository.existsByEmail(email)) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        try {
            // 3. Créer l'utilisateur dans Keycloak
            String userId = createUserInKeycloak(email, password, nom, prenom);

            // 4. Assigner le rôle TOURIST dans Keycloak
            assignRoleToUser(userId, "TOURIST");

            // 5. Créer dans MySQL (seulement les attributs demandés)
            Touriste nouveauTouriste = new Touriste();
            nouveauTouriste.setEmail(email);
            nouveauTouriste.setMotDePasse(password);
            nouveauTouriste.setNom(nom);
            nouveauTouriste.setPrenom(prenom);
            nouveauTouriste.setActif(true);
            nouveauTouriste.setRole(Role.TOURIST);
            nouveauTouriste.setDateInscription(LocalDateTime.now());

            // ✅ Champs optionnels (seulement ceux que vous voulez)
            if (userData.containsKey("telephone") && userData.get("telephone") != null) {
                nouveauTouriste.setTelephone((String) userData.get("telephone"));
            }

            if (userData.containsKey("nationalite") && userData.get("nationalite") != null) {
                nouveauTouriste.setNationalite((String) userData.get("nationalite"));
            }

            Touriste savedTouriste = touristeRepository.save(nouveauTouriste);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Inscription réussie");
            response.put("id", savedTouriste.getId());
            response.put("email", savedTouriste.getEmail());
            response.put("nom", savedTouriste.getNom());
            response.put("prenom", savedTouriste.getPrenom());
            response.put("role", savedTouriste.getRole());

            // Ajouter les champs optionnels dans la réponse s'ils existent
            if (savedTouriste.getTelephone() != null) {
                response.put("telephone", savedTouriste.getTelephone());
            }
            if (savedTouriste.getNationalite() != null) {
                response.put("nationalite", savedTouriste.getNationalite());
            }

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

        Touriste touriste = touristeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Touriste non trouvé"));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", touriste.getId());
        profile.put("email", touriste.getEmail());
        profile.put("nom", touriste.getNom());
        profile.put("prenom", touriste.getPrenom());
        profile.put("telephone", touriste.getTelephone());
        profile.put("nationalite", touriste.getNationalite());
        profile.put("role", touriste.getRole());

        return profile;
    }

    // ========================================
    // METTRE À JOUR LE PROFIL
    // ========================================
    // ========================================
// METTRE À JOUR LE PROFIL (MySQL + Keycloak)
// ========================================
    @Transactional
    public Map<String, Object> updateProfile(String email, Map<String, Object> userData) {

        // 1. Récupérer le touriste dans MySQL
        Touriste existingTouriste = touristeRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Touriste non trouvé"));

        try {
            // 2. Obtenir un token admin pour Keycloak
            String adminToken = getAdminToken();

            // 3. Récupérer l'ID de l'utilisateur dans Keycloak
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("Utilisateur non trouvé dans Keycloak");
            }

            // 4. Préparer les mises à jour pour Keycloak
            Map<String, Object> keycloakUpdates = new HashMap<>();

            // 5. Mettre à jour les champs dans MySQL et préparer Keycloak
            if (userData.containsKey("nom")) {
                existingTouriste.setNom((String) userData.get("nom"));
                keycloakUpdates.put("lastName", userData.get("nom"));
            }

            if (userData.containsKey("prenom")) {
                existingTouriste.setPrenom((String) userData.get("prenom"));
                keycloakUpdates.put("firstName", userData.get("prenom"));
            }

            if (userData.containsKey("telephone")) {
                existingTouriste.setTelephone((String) userData.get("telephone"));
                // Note: Le téléphone n'est pas stocké dans Keycloak par défaut
            }

            if (userData.containsKey("nationalite")) {
                existingTouriste.setNationalite((String) userData.get("nationalite"));
                // Note: La nationalité n'est pas stockée dans Keycloak par défaut
            }

            // 6. Mettre à jour Keycloak si des champs modifiables dans Keycloak sont présents
            if (!keycloakUpdates.isEmpty()) {
                updateUserInKeycloak(userId, keycloakUpdates, adminToken);
            }

            // 7. Sauvegarder dans MySQL
            Touriste updated = touristeRepository.save(existingTouriste);

            // 8. Préparer la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profil mis à jour avec succès");
            response.put("email", updated.getEmail());
            response.put("nom", updated.getNom());
            response.put("prenom", updated.getPrenom());

            return response;

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour du profil: " + e.getMessage());
        }
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

        try {
            restTemplate.exchange(updateUrl, HttpMethod.PUT, entity, String.class);
            System.out.println("✅ Utilisateur Keycloak mis à jour: " + userId);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour dans Keycloak: " + e.getMessage());
        }
    }
    // MOT DE PASSE OUBLIÉ
    // ========================================
    public Map<String, Object> forgotPassword(String email) {

        Touriste touriste = touristeRepository.findByEmail(email)
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

            Touriste touriste = touristeRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Touriste non trouvé dans la base de données"));
            touriste.setMotDePasse(newPassword);
            touristeRepository.save(touriste);

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

    private void sendResetPasswordEmail(String userId, String adminToken) {
        String emailUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/execute-actions-email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        List<String> requiredActions = List.of("UPDATE_PASSWORD");
        String redirectUri = "http://localhost:4200/touristes/reset-password-complete";

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