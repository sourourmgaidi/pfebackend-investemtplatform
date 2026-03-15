package tn.iset.investplatformpfe.Controller;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.LocalProduct;
import tn.iset.investplatformpfe.Service.LocalProductService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/local-products")
public class LocalProductController {

    private final LocalProductService localProductService;

    public LocalProductController(LocalProductService localProductService) {
        this.localProductService = localProductService;
    }

    // ========================================
    // CREATE - Réservé aux ADMIN
    // ========================================
    @PostMapping
    public ResponseEntity<?> createLocalProduct(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody LocalProduct product) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les administrateurs peuvent créer des produits locaux"));
        }

        try {
            LocalProduct created = localProductService.createLocalProduct(product);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // READ (GET ALL) - Public
    // ========================================
    @GetMapping
    public ResponseEntity<List<LocalProduct>> getAllLocalProducts() {
        return ResponseEntity.ok(localProductService.getAllLocalProducts());
    }

    // ========================================
    // READ (GET BY ID) - Public
    // ========================================
    @GetMapping("/{id}")
    public ResponseEntity<?> getLocalProductById(@PathVariable Long id) {
        try {
            LocalProduct product = localProductService.getLocalProductById(id);
            return ResponseEntity.ok(product);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // READ (GET BY CATEGORY) - Public
    // ========================================
    @GetMapping("/category/{category}")
    public ResponseEntity<List<LocalProduct>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(localProductService.getLocalProductsByCategory(category));
    }

    // ========================================
    // UPDATE - Réservé aux ADMIN
    // ========================================
    @PutMapping("/{id}")
    public ResponseEntity<?> updateLocalProduct(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody LocalProduct product) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les administrateurs peuvent modifier des produits locaux"));
        }

        try {
            LocalProduct updated = localProductService.updateLocalProduct(id, product);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // DELETE - Réservé aux ADMIN
    // ========================================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteLocalProduct(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les administrateurs peuvent supprimer des produits locaux"));
        }

        try {
            localProductService.deleteLocalProduct(id);
            return ResponseEntity.ok(Map.of("message", "Produit local supprimé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // SEARCH - Public
    // ========================================
    @GetMapping("/search")
    public ResponseEntity<List<LocalProduct>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(localProductService.searchLocalProducts(keyword));
    }

    // ========================================
    // SEARCH BY CATEGORY - Public
    // ========================================
    @GetMapping("/search/category")
    public ResponseEntity<List<LocalProduct>> searchByCategory(
            @RequestParam String category,
            @RequestParam String keyword) {
        return ResponseEntity.ok(localProductService.searchLocalProductsByCategory(category, keyword));
    }

    // ========================================
    // STATISTICS - Public
    // ========================================
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        List<LocalProduct> all = localProductService.getAllLocalProducts();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", all.size());

        // Compter par catégorie
        Map<String, Long> categoryCount = new HashMap<>();
        for (LocalProduct product : all) {
            categoryCount.put(product.getCategory(), categoryCount.getOrDefault(product.getCategory(), 0L) + 1);
        }
        stats.put("byCategory", categoryCount);

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
