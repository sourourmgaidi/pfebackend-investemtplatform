package tn.iset.investplatformpfe.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.EconomicSector;
import tn.iset.investplatformpfe.Service.EconomicSectorService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/economic-sectors")
public class EconomicSectorController {

    private final EconomicSectorService economicSectorService;

    public EconomicSectorController(EconomicSectorService economicSectorService) {
        this.economicSectorService = economicSectorService;
    }

    // ========================================
    // CREATE - Réservé aux ADMIN
    // ========================================
    @PostMapping
    public ResponseEntity<?> createEconomicSector(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody EconomicSector sector) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les administrateurs peuvent créer des secteurs économiques"));
        }

        try {
            EconomicSector created = economicSectorService.createEconomicSector(sector);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // READ (GET ALL) - Public
    // ========================================
    @GetMapping
    public ResponseEntity<List<EconomicSector>> getAllEconomicSectors() {
        return ResponseEntity.ok(economicSectorService.getAllEconomicSectors());
    }

    // ========================================
    // READ (GET BY ID) - Public
    // ========================================
    @GetMapping("/{id}")
    public ResponseEntity<?> getEconomicSectorById(@PathVariable Long id) {
        try {
            EconomicSector sector = economicSectorService.getEconomicSectorById(id);
            return ResponseEntity.ok(sector);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // READ (GET BY NAME) - Public
    // ========================================
    @GetMapping("/name/{name}")
    public ResponseEntity<?> getEconomicSectorByName(@PathVariable String name) {
        try {
            EconomicSector sector = economicSectorService.getEconomicSectorByName(name);
            return ResponseEntity.ok(sector);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // UPDATE - Réservé aux ADMIN
    // ========================================
    @PutMapping("/{id}")
    public ResponseEntity<?> updateEconomicSector(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody EconomicSector sector) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les administrateurs peuvent modifier des secteurs économiques"));
        }

        try {
            EconomicSector updated = economicSectorService.updateEconomicSector(id, sector);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // DELETE - Réservé aux ADMIN
    // ========================================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEconomicSector(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les administrateurs peuvent supprimer des secteurs économiques"));
        }

        try {
            economicSectorService.deleteEconomicSector(id);
            return ResponseEntity.ok(Map.of("message", "Secteur économique supprimé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // SEARCH - Public
    // ========================================
    @GetMapping("/search")
    public ResponseEntity<List<EconomicSector>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(economicSectorService.searchEconomicSectors(keyword));
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