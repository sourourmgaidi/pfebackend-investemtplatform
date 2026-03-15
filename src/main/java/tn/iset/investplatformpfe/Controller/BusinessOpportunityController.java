package tn.iset.investplatformpfe.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.BusinessOpportunity;
import tn.iset.investplatformpfe.Service.BusinessOpportunityService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/business-opportunities")
public class BusinessOpportunityController {

    private final BusinessOpportunityService businessOpportunityService;

    public BusinessOpportunityController(BusinessOpportunityService businessOpportunityService) {
        this.businessOpportunityService = businessOpportunityService;
    }

    // ========================================
    // CREATE - Réservé aux ADMIN
    // ========================================
    @PostMapping
    public ResponseEntity<?> createBusinessOpportunity(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody BusinessOpportunity opportunity) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les administrateurs peuvent créer des opportunités d'affaires"));
        }

        try {
            BusinessOpportunity created = businessOpportunityService.createBusinessOpportunity(opportunity);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // READ (GET ALL) - Public
    // ========================================
    @GetMapping
    public ResponseEntity<List<BusinessOpportunity>> getAllBusinessOpportunities() {
        return ResponseEntity.ok(businessOpportunityService.getAllBusinessOpportunities());
    }

    // ========================================
    // READ (GET BY ID) - Public
    // ========================================
    @GetMapping("/{id}")
    public ResponseEntity<?> getBusinessOpportunityById(@PathVariable Long id) {
        try {
            BusinessOpportunity opportunity = businessOpportunityService.getBusinessOpportunityById(id);
            return ResponseEntity.ok(opportunity);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // READ (GET BY TYPE) - Public
    // ========================================
    @GetMapping("/type/{type}")
    public ResponseEntity<List<BusinessOpportunity>> getByType(@PathVariable String type) {
        return ResponseEntity.ok(businessOpportunityService.getBusinessOpportunitiesByType(type));
    }

    // ========================================
    // READ (GET RECENT) - Public
    // ========================================
    @GetMapping("/recent")
    public ResponseEntity<List<BusinessOpportunity>> getRecent(
            @RequestParam(defaultValue = "7") int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return ResponseEntity.ok(businessOpportunityService.getRecentBusinessOpportunities(since));
    }

    // ========================================
    // UPDATE - Réservé aux ADMIN
    // ========================================
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBusinessOpportunity(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody BusinessOpportunity opportunity) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les administrateurs peuvent modifier des opportunités d'affaires"));
        }

        try {
            BusinessOpportunity updated = businessOpportunityService.updateBusinessOpportunity(id, opportunity);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // DELETE - Réservé aux ADMIN
    // ========================================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBusinessOpportunity(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les administrateurs peuvent supprimer des opportunités d'affaires"));
        }

        try {
            businessOpportunityService.deleteBusinessOpportunity(id);
            return ResponseEntity.ok(Map.of("message", "Opportunité supprimée avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // SEARCH - Public
    // ========================================
    @GetMapping("/search")
    public ResponseEntity<List<BusinessOpportunity>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(businessOpportunityService.searchBusinessOpportunities(keyword));
    }

    // ========================================
    // SEARCH BY TYPE - Public
    // ========================================
    @GetMapping("/search/type")
    public ResponseEntity<List<BusinessOpportunity>> searchByType(
            @RequestParam String type,
            @RequestParam String keyword) {
        return ResponseEntity.ok(businessOpportunityService.searchBusinessOpportunitiesByType(type, keyword));
    }

    // ========================================
    // STATISTICS - Public
    // ========================================
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        List<BusinessOpportunity> all = businessOpportunityService.getAllBusinessOpportunities();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", all.size());

        // Compter par type
        Map<String, Long> typeCount = new HashMap<>();
        for (BusinessOpportunity opp : all) {
            typeCount.put(opp.getType(), typeCount.getOrDefault(opp.getType(), 0L) + 1);
        }
        stats.put("byType", typeCount);

        return ResponseEntity.ok(stats);
    }

    // ========================================
    // Méthode utilitaire pour vérifier les rôles
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