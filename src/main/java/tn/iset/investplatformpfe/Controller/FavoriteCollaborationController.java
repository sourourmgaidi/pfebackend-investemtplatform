package tn.iset.investplatformpfe.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.CollaborationService;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Service.FavoriteCollaborationService;
import tn.iset.investplatformpfe.Service.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FavoriteCollaborationController {

    private final FavoriteCollaborationService favoriteService;
    private final UserService userService;

    public FavoriteCollaborationController(
            FavoriteCollaborationService favoriteService,
            UserService userService) {
        this.favoriteService = favoriteService;
        this.userService = userService;
    }

    // ========================================
    // MÉTHODE UTILITAIRE POUR VÉRIFIER LE RÔLE
    // ========================================
    private boolean hasRole(Jwt jwt, String role) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles != null && roles.contains(role);
        }
        return false;
    }

    // ========================================
    // POUR INTERNATIONAL COMPANY
    // ========================================

    /**
     * Ajouter un service de collaboration aux favoris (International Company)
     */
    @PostMapping("/international-companies/collaboration-favorites/{serviceId}")
    public ResponseEntity<?> addCompanyCollaborationFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serviceId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "INTERNATIONAL_COMPANY")) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Accès réservé aux sociétés internationales"
            ));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Long companyId = userService.getUserIdByEmailAndRole(email, Role.INTERNATIONAL_COMPANY);

            CollaborationService service = favoriteService.addCompanyFavorite(companyId, serviceId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Service de collaboration ajouté aux favoris",
                    "serviceId", serviceId,
                    "serviceName", service.getName()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Retirer un service de collaboration des favoris (International Company)
     */
    @DeleteMapping("/international-companies/collaboration-favorites/{serviceId}")
    public ResponseEntity<?> removeCompanyCollaborationFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serviceId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "INTERNATIONAL_COMPANY")) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Accès réservé aux sociétés internationales"
            ));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Long companyId = userService.getUserIdByEmailAndRole(email, Role.INTERNATIONAL_COMPANY);

            favoriteService.removeCompanyFavorite(companyId, serviceId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Service de collaboration retiré des favoris",
                    "serviceId", serviceId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Récupérer tous les favoris de collaboration (International Company)
     */
    @GetMapping("/international-companies/collaboration-favorites")
    public ResponseEntity<?> getCompanyCollaborationFavorites(@AuthenticationPrincipal Jwt jwt) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "INTERNATIONAL_COMPANY")) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Accès réservé aux sociétés internationales"
            ));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Long companyId = userService.getUserIdByEmailAndRole(email, Role.INTERNATIONAL_COMPANY);

            List<CollaborationService> favorites = favoriteService.getCompanyFavorites(companyId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "favorites", favorites,
                    "count", favorites.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Vérifier si un service est en favori (International Company)
     */
    @GetMapping("/international-companies/collaboration-favorites/check/{serviceId}")
    public ResponseEntity<?> checkCompanyCollaborationFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serviceId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "INTERNATIONAL_COMPANY")) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Accès réservé aux sociétés internationales"
            ));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Long companyId = userService.getUserIdByEmailAndRole(email, Role.INTERNATIONAL_COMPANY);

            boolean isFavorite = favoriteService.isCompanyFavorite(companyId, serviceId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "isFavorite", isFavorite,
                    "serviceId", serviceId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Compter le nombre de favoris (International Company)
     */
    @GetMapping("/international-companies/collaboration-favorites/count")
    public ResponseEntity<?> countCompanyCollaborationFavorites(@AuthenticationPrincipal Jwt jwt) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "INTERNATIONAL_COMPANY")) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Accès réservé aux sociétés internationales"
            ));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Long companyId = userService.getUserIdByEmailAndRole(email, Role.INTERNATIONAL_COMPANY);

            int count = favoriteService.countCompanyFavorites(companyId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", count
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    // ========================================
    // POUR ECONOMIC PARTNER
    // ========================================

    /**
     * Ajouter un service de collaboration aux favoris (Economic Partner)
     */
    @PostMapping("/economic-partners/collaboration-favorites/{serviceId}")
    public ResponseEntity<?> addPartnerCollaborationFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serviceId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "PARTNER")) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Accès réservé aux partenaires économiques"
            ));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Long partnerId = userService.getUserIdByEmailAndRole(email, Role.PARTNER);

            CollaborationService service = favoriteService.addPartnerFavorite(partnerId, serviceId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Service de collaboration ajouté aux favoris",
                    "serviceId", serviceId,
                    "serviceName", service.getName()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Retirer un service de collaboration des favoris (Economic Partner)
     */
    @DeleteMapping("/economic-partners/collaboration-favorites/{serviceId}")
    public ResponseEntity<?> removePartnerCollaborationFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serviceId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "PARTNER")) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Accès réservé aux partenaires économiques"
            ));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Long partnerId = userService.getUserIdByEmailAndRole(email, Role.PARTNER);

            favoriteService.removePartnerFavorite(partnerId, serviceId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Service de collaboration retiré des favoris",
                    "serviceId", serviceId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Récupérer tous les favoris de collaboration (Economic Partner)
     */
    @GetMapping("/economic-partners/collaboration-favorites")
    public ResponseEntity<?> getPartnerCollaborationFavorites(@AuthenticationPrincipal Jwt jwt) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "PARTNER")) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Accès réservé aux partenaires économiques"
            ));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Long partnerId = userService.getUserIdByEmailAndRole(email, Role.PARTNER);

            List<CollaborationService> favorites = favoriteService.getPartnerFavorites(partnerId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "favorites", favorites,
                    "count", favorites.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Vérifier si un service est en favori (Economic Partner)
     */
    @GetMapping("/economic-partners/collaboration-favorites/check/{serviceId}")
    public ResponseEntity<?> checkPartnerCollaborationFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serviceId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "PARTNER")) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Accès réservé aux partenaires économiques"
            ));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Long partnerId = userService.getUserIdByEmailAndRole(email, Role.PARTNER);

            boolean isFavorite = favoriteService.isPartnerFavorite(partnerId, serviceId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "isFavorite", isFavorite,
                    "serviceId", serviceId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Compter le nombre de favoris (Economic Partner)
     */
    @GetMapping("/economic-partners/collaboration-favorites/count")
    public ResponseEntity<?> countPartnerCollaborationFavorites(@AuthenticationPrincipal Jwt jwt) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "PARTNER")) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Accès réservé aux partenaires économiques"
            ));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Long partnerId = userService.getUserIdByEmailAndRole(email, Role.PARTNER);

            int count = favoriteService.countPartnerFavorites(partnerId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", count
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
