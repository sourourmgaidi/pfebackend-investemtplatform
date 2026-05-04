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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
    private final UserSessionService sessionService;

    // ========================================
    // REGEX PATTERNS
    // ========================================
    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(https?://)([\\w\\-]+\\.)+[\\w]{2,}(/.*)?$"
    );
    private static final Pattern LINKEDIN_PATTERN = Pattern.compile(
            "^(https?://)?(www\\.)?linkedin\\.com/in/[\\w\\-]+/?$"
    );

    // ========================================
    // RÈGLES DE LONGUEUR PAR INDICATIF PAYS
    // ========================================
    private static final Map<String, Integer> PHONE_LENGTH_BY_DIAL_CODE = Map.ofEntries(
            Map.entry("+216", 8),
            Map.entry("+33",  9),
            Map.entry("+213", 9),
            Map.entry("+212", 9),
            Map.entry("+218", 9),
            Map.entry("+20",  10),
            Map.entry("+966", 9),
            Map.entry("+971", 9),
            Map.entry("+974", 8),
            Map.entry("+965", 8),
            Map.entry("+1",   10),
            Map.entry("+44",  10),
            Map.entry("+49",  10),
            Map.entry("+39",  10),
            Map.entry("+34",  9),
            Map.entry("+32",  9),
            Map.entry("+41",  9),
            Map.entry("+31",  9),
            Map.entry("+46",  9),
            Map.entry("+47",  8),
            Map.entry("+45",  8),
            Map.entry("+358", 9),
            Map.entry("+7",   10),
            Map.entry("+86",  11),
            Map.entry("+81",  10),
            Map.entry("+82",  10),
            Map.entry("+91",  10),
            Map.entry("+55",  11),
            Map.entry("+61",  9)
    );

    public AuthService(InvestorRepository investorRepository, UserSessionService sessionService) {
        this.investorRepository = investorRepository;
        this.sessionService = sessionService;
        this.restTemplate = new RestTemplate();
    }

    // ========================================
    // VALIDATION GMAIL (existante — inchangée)
    // ========================================
    private boolean isGmail(String email) {
        if (email == null) return false;

        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();

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
    // NOUVELLES MÉTHODES DE VALIDATION
    // ========================================

    private void validateEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("L'email est obligatoire");
        }
        if (!email.contains("@")) {
            throw new RuntimeException("Format d'email invalide");
        }
        if (!isGmail(email)) {
            throw new RuntimeException("Seules les adresses Gmail sont autorisées (ex: @gmail.com, @gmail.fr, etc.)");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("Le mot de passe est obligatoire");
        }
        if (password.length() < 6) {
            throw new RuntimeException("Le mot de passe doit contenir au moins 6 caractères");
        }
    }

    private void validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return;

        String p = phone.trim();

        if (!p.startsWith("+")) {
            throw new RuntimeException(
                    "Numéro de téléphone invalide. Il doit commencer par l'indicatif pays (ex: +216 pour la Tunisie)."
            );
        }

        // Trier par longueur décroissante pour éviter les conflits (+216 avant +21, etc.)
        String matchedDialCode = null;
        int matchedLength = -1;

        List<String> sortedDialCodes = PHONE_LENGTH_BY_DIAL_CODE.keySet()
                .stream()
                .sorted((a, b) -> Integer.compare(b.length(), a.length()))
                .collect(Collectors.toList());

        for (String dialCode : sortedDialCodes) {
            if (p.startsWith(dialCode)) {
                matchedDialCode = dialCode;
                matchedLength = PHONE_LENGTH_BY_DIAL_CODE.get(dialCode);
                break;
            }
        }

        if (matchedDialCode == null) {
            throw new RuntimeException(
                    "Indicatif pays non reconnu dans le numéro '" + p + "'. " +
                            "Veuillez utiliser un des indicatifs disponibles (ex: +216, +33, +1, +44...)."
            );
        }

        String localNumber = p.substring(matchedDialCode.length());

        if (!localNumber.matches("[0-9]+")) {
            throw new RuntimeException(
                    "Le numéro de téléphone ne doit contenir que des chiffres après l'indicatif pays."
            );
        }

        if (localNumber.length() != matchedLength) {
            throw new RuntimeException(
                    "Numéro de téléphone invalide pour l'indicatif " + matchedDialCode + ": " +
                            "le numéro local doit contenir exactement " + matchedLength + " chiffres " +
                            "(reçu: " + localNumber.length() + " chiffre(s))."
            );
        }
    }

    private void validateWebsite(String website) {
        if (website != null && !website.trim().isEmpty()) {
            if (!URL_PATTERN.matcher(website.trim()).matches()) {
                throw new RuntimeException("URL du site web invalide. Format attendu: https://www.example.com");
            }
        }
    }

    private void validateLinkedin(String linkedin) {
        if (linkedin != null && !linkedin.trim().isEmpty()) {
            if (!LINKEDIN_PATTERN.matcher(linkedin.trim()).matches()) {
                throw new RuntimeException("URL LinkedIn invalide. Format attendu: https://www.linkedin.com/in/votre-profil");
            }
        }
    }

    private void validateRequiredString(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new RuntimeException("Le champ '" + fieldName + "' est obligatoire");
        }
    }


    private void validateOriginCountry(String country) {
        if (country != null && !country.trim().isEmpty()) {
            if (country.trim().length() < 2 || country.trim().length() > 60) {
                throw new RuntimeException("Le pays d'origine doit contenir entre 2 et 60 caractères");
            }
            if (!country.matches("[\\p{L} \\-']+")) {
                throw new RuntimeException("Le pays d'origine ne doit contenir que des lettres, espaces, tirets ou apostrophes");
            }
        }
    }

    private void validateNationality(String nationality) {
        if (nationality != null && !nationality.trim().isEmpty()) {
            if (nationality.trim().length() < 2 || nationality.trim().length() > 60) {
                throw new RuntimeException("La nationalité doit contenir entre 2 et 60 caractères");
            }
            if (!nationality.matches("[\\p{L} \\-']+")) {
                throw new RuntimeException("La nationalité ne doit contenir que des lettres, espaces, tirets ou apostrophes");
            }
        }
    }

    private void validateCompany(String company) {
        if (company != null && !company.trim().isEmpty()) {
            if (company.trim().length() < 2 || company.trim().length() > 100) {
                throw new RuntimeException("Le nom de l'entreprise doit contenir entre 2 et 100 caractères");
            }
        }
    }

    // ========================================
    // INSCRIPTION - AVEC VALIDATION GMAIL
    // ========================================
    public Map<String, Object> register(Map<String, Object> userData) {

        String email = (String) userData.get("email");
        String password = (String) userData.get("password");
        String firstName = (String) userData.get("firstName");
        String lastName = (String) userData.get("lastName");

        // ✅ VALIDATIONS OBLIGATOIRES
        validateEmail(email);
        validateRequiredString(firstName, "firstName");
        validateRequiredString(lastName, "lastName");
        validatePassword(password);

        // ✅ VALIDATIONS OPTIONNELLES
        validatePhone((String) userData.get("phone"));
        validateWebsite((String) userData.get("website"));
        validateLinkedin((String) userData.get("linkedinProfile"));
        validateOriginCountry((String) userData.get("originCountry"));
        validateNationality((String) userData.get("nationality"));
        validateCompany((String) userData.get("company"));

        // Récupérer le rôle depuis la requête (avec valeur par défaut INVESTOR)
        String roleStr = (String) userData.get("role");
        Role role = Role.INVESTOR;

        if (roleStr != null && !roleStr.isEmpty()) {
            try {
                role = Role.valueOf(roleStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Rôle invalide: " + roleStr);
            }
        }

        // Vérifier si l'email existe déjà dans MySQL
        if (investorRepository.existsByEmail(email)) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        try {
            String userId = createUserInKeycloak(email, password, firstName, lastName);
            assignRoleToUser(userId, role.name());

            Investor newInvestor = new Investor();
            newInvestor.setEmail(email);
            newInvestor.setPassword(password);
            newInvestor.setFirstName(firstName);
            newInvestor.setLastName(lastName);
            newInvestor.setActive(true);
            newInvestor.setRole(role);
            newInvestor.setRegistrationDate(LocalDateTime.now());

            if (userData.containsKey("phone") && userData.get("phone") != null) {
                newInvestor.setPhone((String) userData.get("phone"));
            }

            if (userData.containsKey("company") && userData.get("company") != null) {
                newInvestor.setCompany((String) userData.get("company"));
            }

            if (userData.containsKey("originCountry") && userData.get("originCountry") != null) {
                newInvestor.setOriginCountry((String) userData.get("originCountry"));
            }

            if (userData.containsKey("nationality") && userData.get("nationality") != null) {
                newInvestor.setNationality((String) userData.get("nationality"));
            }

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

    public void startSessionAfterLogin(String email, String role) {
        try {
            sessionService.startSession(email, role);
            System.out.println("✅ Session démarrée pour " + email + " avec rôle: " + role);
        } catch (Exception e) {
            System.err.println("⚠️ Impossible de démarrer la session: " + e.getMessage());
        }
    }

    public void endSession(String email) {
        try {
            sessionService.endSession(email);
            System.out.println("✅ Session terminée pour " + email);
        } catch (Exception e) {
            System.err.println("⚠️ Erreur fermeture session: " + e.getMessage());
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
    @Transactional
    public Map<String, Object> updateProfile(String email, Map<String, Object> userData) {

        Investor existing = investorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // ✅ VALIDATIONS SUR LES CHAMPS ENVOYÉS
        if (userData.containsKey("phone"))
            validatePhone((String) userData.get("phone"));

        if (userData.containsKey("website"))
            validateWebsite((String) userData.get("website"));

        if (userData.containsKey("linkedinProfile"))
            validateLinkedin((String) userData.get("linkedinProfile"));


        if (userData.containsKey("originCountry"))
            validateOriginCountry((String) userData.get("originCountry"));

        if (userData.containsKey("nationality"))
            validateNationality((String) userData.get("nationality"));

        if (userData.containsKey("company"))
            validateCompany((String) userData.get("company"));

        if (userData.containsKey("firstName"))
            validateRequiredString((String) userData.get("firstName"), "firstName");

        if (userData.containsKey("lastName"))
            validateRequiredString((String) userData.get("lastName"), "lastName");

        try {
            String adminToken = getAdminToken();
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("Utilisateur non trouvé dans Keycloak");
            }

            Map<String, Object> keycloakUpdates = new HashMap<>();
            boolean emailChanged = false;
            String newEmail = null;

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

            if (userData.containsKey("email")) {
                newEmail = (String) userData.get("email");

                if (!newEmail.equals(existing.getEmail())) {
                    // ✅ VALIDATION GMAIL POUR LE NOUVEL EMAIL
                    if (!isGmail(newEmail)) {
                        throw new RuntimeException("Le nouvel email doit être une adresse Gmail valide");
                    }

                    if (investorRepository.existsByEmail(newEmail)) {
                        throw new RuntimeException("Cet email est déjà utilisé: " + newEmail);
                    }
                    emailChanged = true;
                }
            }

            if (userData.containsKey("phone")) {
                existing.setPhone((String) userData.get("phone"));
            }

            if (userData.containsKey("company")) {
                existing.setCompany((String) userData.get("company"));
            }

            if (userData.containsKey("originCountry")) {
                existing.setOriginCountry((String) userData.get("originCountry"));
            }

            if (userData.containsKey("nationality")) {
                existing.setNationality((String) userData.get("nationality"));
            }

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


            if (userData.containsKey("password")) {
                String newPassword = (String) userData.get("password");
                if (newPassword != null && !newPassword.isEmpty()) {
                    validatePassword(newPassword);
                    existing.setPassword(newPassword);
                    updatePasswordInKeycloak(userId, newPassword, adminToken);
                }
            }

            if (!keycloakUpdates.isEmpty()) {
                updateUserInKeycloak(userId, keycloakUpdates, adminToken);
            }

            if (emailChanged) {
                updateEmailInKeycloak(userId, newEmail, adminToken);
                existing.setEmail(newEmail);
            }

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

    // ========================================
    // MOT DE PASSE OUBLIÉ - ENVOI D'EMAIL
    // ========================================
    public Map<String, Object> forgotPassword(String email) {

        Investor investor = investorRepository.findByEmail(email)
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
    // RÉINITIALISER LE MOT DE PASSE DIRECTEMENT
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

            Investor investor = investorRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé dans la base de données"));
            investor.setPassword(newPassword);
            investorRepository.save(investor);

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

    private void updateUserInKeycloak(String userId, Map<String, Object> updates, String adminToken) {
        String updateUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

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
                System.out.println("❌ Erreur updateUserInKeycloak: " + e.getMessage());
                throw new RuntimeException("Erreur lors de la mise à jour dans Keycloak: " + e.getMessage());
            }
        }
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

        try {
            restTemplate.exchange(updateUrl, HttpMethod.PUT, entity, String.class);
            System.out.println("✅ Email mis à jour dans Keycloak: " + userId);
        } catch (Exception e) {
            System.out.println("❌ Erreur updateEmailInKeycloak: " + e.getMessage());
            throw new RuntimeException("Erreur lors de la mise à jour de l'email dans Keycloak: " + e.getMessage());
        }
    }

    private void sendResetPasswordEmail(String userId, String adminToken) {
        String emailUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + userId + "/execute-actions-email";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminToken);

        List<String> requiredActions = List.of("UPDATE_PASSWORD");

        String redirectUri = "http://localhost:4200/reset-password-complete";
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
    // SUPPRESSION COMPLÈTE DU COMPTE
    // ========================================
    @Transactional
    public Map<String, Object> deleteAccount(String email, String password) {

        Investor investor = investorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé dans la base de données"));

        try {
            String adminToken = getAdminToken();
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                System.out.println("⚠️ Utilisateur non trouvé dans Keycloak, suppression uniquement de MySQL");
            } else {
                try {
                    validatePasswordWithKeycloak(email, password);
                } catch (Exception e) {
                    throw new RuntimeException("Mot de passe incorrect. La suppression est annulée.");
                }

                deleteUserFromKeycloak(userId, adminToken);
                System.out.println("✅ Utilisateur supprimé de Keycloak: " + userId);
            }

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
    // SUPPRESSION PAR ADMIN (SANS MOT DE PASSE)
    // ========================================
    @Transactional
    public Map<String, Object> deleteAccountByAdmin(String email) {

        Investor investor = investorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé dans la base de données"));

        try {
            String adminToken = getAdminToken();
            String userId = getUserIdByEmail(email, adminToken);

            if (userId != null) {
                deleteUserFromKeycloak(userId, adminToken);
                System.out.println("✅ Utilisateur supprimé de Keycloak: " + userId);
            }

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

    // ========================================
    // CHANGER LE MOT DE PASSE
    // ========================================
    @Transactional
    public Map<String, Object> changePassword(String email, String oldPassword, String newPassword) {

        Investor investor = investorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Investisseur non trouvé"));

        if (newPassword == null || newPassword.length() < 6) {
            throw new RuntimeException("Le nouveau mot de passe doit contenir au moins 6 caractères");
        }

        try {
            validatePasswordWithKeycloak(email, oldPassword);

            String adminToken = getAdminToken();
            String userId = getUserIdByEmail(email, adminToken);

            if (userId == null) {
                throw new RuntimeException("Utilisateur non trouvé dans Keycloak");
            }

            updatePasswordInKeycloak(userId, newPassword, adminToken);
            System.out.println("✅ Mot de passe mis à jour dans Keycloak pour: " + email);

            investor.setPassword(newPassword);
            investorRepository.save(investor);
            System.out.println("✅ Mot de passe mis à jour dans MySQL pour: " + email);

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
    // RÉCUPÉRER L'EMAIL DEPUIS KEYCLOAK VIA LE SUB
    // ========================================
    public String findEmailByKeycloakSub(String sub) {
        try {
            String adminToken = getAdminToken();
            String userUrl = authServerUrl + "/admin/realms/" + realm + "/users/" + sub;

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(adminToken);

            HttpEntity<?> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    userUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            if (response.getBody() != null) {
                String emailFound = (String) response.getBody().get("email");
                System.out.println("📧 Email trouvé via Keycloak sub: " + emailFound);
                return emailFound;
            }

            return null;
        } catch (Exception e) {
            System.err.println("⚠️ Erreur récupération email Keycloak: " + e.getMessage());
            return null;
        }
    }
}