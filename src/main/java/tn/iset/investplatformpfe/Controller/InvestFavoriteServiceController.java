package tn.iset.investplatformpfe.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.InvestmentService;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Service.FavoriteInvestService;
import tn.iset.investplatformpfe.Service.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class InvestFavoriteServiceController {

    private final FavoriteInvestService favoriteService;
    private final UserService userService;

    public InvestFavoriteServiceController(
            FavoriteInvestService favoriteService,
            UserService userService) {
        this.favoriteService = favoriteService;
        this.userService = userService;
    }

    // ========================================
    // POUR INVESTOR
    // ========================================

    // ========================================
    // AJOUTER UN SERVICE AUX FAVORIS (INVESTOR)
    // ========================================
    @PostMapping("/investors/favorites/{serviceId}")
    public ResponseEntity<?> addInvestorFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serviceId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "INVESTOR")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux investisseurs"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Long investorId = userService.getUserIdByEmailAndRole(email, Role.INVESTOR);

            InvestmentService service = favoriteService.addInvestorFavorite(investorId, serviceId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Service ajouté aux favoris",
                    "serviceId", serviceId,
                    "serviceName", service.getName()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    // ========================================
    // RETIRER UN SERVICE DES FAVORIS (INVESTOR)
    // ========================================
    @DeleteMapping("/investors/favorites/{serviceId}")
    public ResponseEntity<?> removeInvestorFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serviceId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "INVESTOR")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux investisseurs"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Long investorId = userService.getUserIdByEmailAndRole(email, Role.INVESTOR);

            favoriteService.removeInvestorFavorite(investorId, serviceId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Service retiré des favoris",
                    "serviceId", serviceId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    // ========================================
    // LISTER TOUS LES FAVORIS (INVESTOR)
    // ========================================
    @GetMapping("/investors/favorites")
    public ResponseEntity<?> getInvestorFavorites(@AuthenticationPrincipal Jwt jwt) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "INVESTOR")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux investisseurs"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Long investorId = userService.getUserIdByEmailAndRole(email, Role.INVESTOR);

            List<InvestmentService> favorites = favoriteService.getInvestorFavorites(investorId);

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

    // ========================================
    // VÉRIFIER SI UN SERVICE EST EN FAVORI (INVESTOR)
    // ========================================
    @GetMapping("/investors/favorites/check/{serviceId}")
    public ResponseEntity<?> checkInvestorFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serviceId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "INVESTOR")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux investisseurs"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Long investorId = userService.getUserIdByEmailAndRole(email, Role.INVESTOR);

            boolean isFavorite = favoriteService.isInvestorFavorite(investorId, serviceId);

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

    // ========================================
    // COMPTER LE NOMBRE DE FAVORIS (INVESTOR)
    // ========================================
    @GetMapping("/investors/favorites/count")
    public ResponseEntity<?> countInvestorFavorites(@AuthenticationPrincipal Jwt jwt) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "INVESTOR")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux investisseurs"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Long investorId = userService.getUserIdByEmailAndRole(email, Role.INVESTOR);

            int count = favoriteService.countInvestorFavorites(investorId);

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
    // POUR INTERNATIONAL COMPANY
    // ========================================

    // ========================================
    // AJOUTER UN SERVICE AUX FAVORIS (COMPANY)
    // ========================================
    @PostMapping("/international-companies/favorites/{serviceId}")
    public ResponseEntity<?> addCompanyFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serviceId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "INTERNATIONAL_COMPANY")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux sociétés internationales"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Long companyId = userService.getUserIdByEmailAndRole(email, Role.INTERNATIONAL_COMPANY);

            InvestmentService service = favoriteService.addCompanyFavorite(companyId, serviceId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Service ajouté aux favoris",
                    "serviceId", serviceId,
                    "serviceName", service.getName()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    // ========================================
    // RETIRER UN SERVICE DES FAVORIS (COMPANY)
    // ========================================
    @DeleteMapping("/international-companies/favorites/{serviceId}")
    public ResponseEntity<?> removeCompanyFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serviceId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "INTERNATIONAL_COMPANY")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux sociétés internationales"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Long companyId = userService.getUserIdByEmailAndRole(email, Role.INTERNATIONAL_COMPANY);

            favoriteService.removeCompanyFavorite(companyId, serviceId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Service retiré des favoris",
                    "serviceId", serviceId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    // ========================================
    // LISTER TOUS LES FAVORIS (COMPANY)
    // ========================================
    @GetMapping("/international-companies/favorites")
    public ResponseEntity<?> getCompanyFavorites(@AuthenticationPrincipal Jwt jwt) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "INTERNATIONAL_COMPANY")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux sociétés internationales"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Long companyId = userService.getUserIdByEmailAndRole(email, Role.INTERNATIONAL_COMPANY);

            List<InvestmentService> favorites = favoriteService.getCompanyFavorites(companyId);

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

    // ========================================
    // VÉRIFIER SI UN SERVICE EST EN FAVORI (COMPANY)
    // ========================================
    @GetMapping("/international-companies/favorites/check/{serviceId}")
    public ResponseEntity<?> checkCompanyFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serviceId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "INTERNATIONAL_COMPANY")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux sociétés internationales"));
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

    // ========================================
    // COMPTER LE NOMBRE DE FAVORIS (COMPANY)
    // ========================================
    @GetMapping("/international-companies/favorites/count")
    public ResponseEntity<?> countCompanyFavorites(@AuthenticationPrincipal Jwt jwt) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "INTERNATIONAL_COMPANY")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux sociétés internationales"));
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
}