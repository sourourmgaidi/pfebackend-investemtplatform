package tn.iset.investplatformpfe.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.CollaborationService;
import tn.iset.investplatformpfe.Entity.Region;
import tn.iset.investplatformpfe.Entity.Availability;
import tn.iset.investplatformpfe.Entity.ServiceStatus;
import tn.iset.investplatformpfe.Service.CollaborationServiceService;
import tn.iset.investplatformpfe.Service.NotificationService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collaboration-services")
public class CollaborationServiceController {

    private final CollaborationServiceService service;
    private final PartenaireLocalAuthService partenaireLocalAuthService;
    private final NotificationService notificationService;

    public CollaborationServiceController(CollaborationServiceService service,
                                          PartenaireLocalAuthService partenaireLocalAuthService,
                                          NotificationService notificationService) {
        this.service = service;
        this.partenaireLocalAuthService = partenaireLocalAuthService;
        this.notificationService = notificationService;
    }

    // ========================================
    // CREATE - Réservé aux LOCAL_PARTNER
    // ========================================
    @PostMapping("/with-provider/{providerId}")
    public ResponseEntity<?> createCollaborationServiceWithProvider(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long providerId,
            @RequestBody CollaborationService serviceData) {

        // Vérifier que l'utilisateur est authentifié
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        // Vérifier que l'utilisateur a le rôle LOCAL_PARTNER
        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Accès refusé. Seuls les partenaires locaux peuvent créer des services."));
        }

        try {
            // Vérifier que le providerId correspond à l'utilisateur connecté
            String email = jwt.getClaimAsString("email");
            Map<String, Object> partenaire = partenaireLocalAuthService.getProfile(email);
            Long partenaireId = (Long) partenaire.get("id");

            if (!partenaireId.equals(providerId)) {
                return ResponseEntity.status(403).body(Map.of("error",
                        "Vous ne pouvez créer des services que pour votre propre compte"));
            }

            CollaborationService created = service.createCollaborationServiceWithProviderId(serviceData, providerId);
            return new ResponseEntity<>(created, HttpStatus.CREATED);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // CREATE (sans providerId explicite)
    // ========================================
    @PostMapping
    public ResponseEntity<?> createCollaborationService(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody CollaborationService serviceData) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Accès refusé. Seuls les partenaires locaux peuvent créer des services."));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Map<String, Object> partenaire = partenaireLocalAuthService.getProfile(email);
            Long partenaireId = (Long) partenaire.get("id");

            CollaborationService created = service.createCollaborationServiceWithProviderId(serviceData, partenaireId);
            return new ResponseEntity<>(created, HttpStatus.CREATED);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // GET BY PROVIDER ID - Accessible à tous
    // ========================================
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<?> getByProviderId(@PathVariable Long providerId) {
        try {
            List<CollaborationService> services = service.getCollaborationServicesByProviderId(providerId);
            return ResponseEntity.ok(services);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // UPDATE - Seul le propriétaire peut modifier
    // ========================================
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCollaborationService(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody CollaborationService serviceData) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Accès refusé. Seuls les partenaires locaux peuvent modifier des services."));
        }

        try {
            // Récupérer le service existant
            CollaborationService existingService = service.getCollaborationServiceById(id);

            // Vérifier que l'utilisateur est le propriétaire
            String email = jwt.getClaimAsString("email");
            Map<String, Object> partenaire = partenaireLocalAuthService.getProfile(email);
            Long partenaireId = (Long) partenaire.get("id");

            if (!existingService.getProvider().getId().equals(partenaireId)) {
                return ResponseEntity.status(403).body(Map.of("error",
                        "Vous ne pouvez modifier que vos propres services"));
            }

            CollaborationService updated = service.updateCollaborationService(id, serviceData);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // DELETE - Seul le propriétaire ou admin peut supprimer
    // ========================================
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCollaborationService(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            CollaborationService existingService = service.getCollaborationServiceById(id);

            String email = jwt.getClaimAsString("email");
            Map<String, Object> partenaire = partenaireLocalAuthService.getProfile(email);
            Long partenaireId = (Long) partenaire.get("id");

            // Vérifier si c'est le propriétaire OU un ADMIN
            boolean isOwner = existingService.getProvider().getId().equals(partenaireId);
            boolean isAdmin = hasRole(jwt, "ADMIN");

            if (!isOwner && !isAdmin) {
                return ResponseEntity.status(403).body(Map.of("error",
                        "Vous n'avez pas les droits pour supprimer ce service"));
            }

            service.deleteCollaborationService(id);
            return ResponseEntity.ok(Map.of("message", "Service supprimé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // APPROVE - Réservé aux ADMIN
    // ========================================
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveService(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les administrateurs peuvent approuver des services"));
        }

        try {
            CollaborationService approved = service.approveService(id);

            // ✅ Les notifications sont déjà gérées dans le service
            // Pas besoin de les appeler ici

            return ResponseEntity.ok(approved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // REJECT - Réservé aux ADMIN
    // ========================================
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectService(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les administrateurs peuvent rejeter des services"));
        }

        try {
            CollaborationService rejected = service.rejectService(id);

            // ✅ Les notifications sont déjà gérées dans le service

            return ResponseEntity.ok(rejected);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // READ METHODS (PUBLIC)
    // ========================================
    @GetMapping
    public ResponseEntity<List<CollaborationService>> getAllCollaborationServices() {
        return ResponseEntity.ok(service.getAllCollaborationServices());
    }

    @GetMapping("/pending")
    public ResponseEntity<List<CollaborationService>> getPendingServices() {
        return ResponseEntity.ok(service.getPendingCollaborationServices());
    }

    @GetMapping("/approved")
    public ResponseEntity<List<CollaborationService>> getApprovedServices() {
        return ResponseEntity.ok(service.getApprovedCollaborationServices());
    }

    @GetMapping("/rejected")
    public ResponseEntity<List<CollaborationService>> getRejectedServices() {
        return ResponseEntity.ok(service.getRejectedCollaborationServices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCollaborationServiceById(@PathVariable Long id) {
        try {
            CollaborationService serviceData = service.getCollaborationServiceById(id);
            return ResponseEntity.ok(serviceData);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/region/{region}")
    public ResponseEntity<List<CollaborationService>> getByRegion(@PathVariable Region region) {
        return ResponseEntity.ok(service.getCollaborationServicesByRegion(region));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<CollaborationService>> getByStatus(@PathVariable ServiceStatus status) {
        return ResponseEntity.ok(service.getCollaborationServicesByStatus(status));
    }

    @GetMapping("/availability/{availability}")
    public ResponseEntity<List<CollaborationService>> getByAvailability(@PathVariable Availability availability) {
        return ResponseEntity.ok(service.getCollaborationServicesByAvailability(availability));
    }

    @GetMapping("/price-range")
    public ResponseEntity<List<CollaborationService>> getByPriceRange(
            @RequestParam(required = false) BigDecimal min,
            @RequestParam(required = false) BigDecimal max) {
        return ResponseEntity.ok(service.getCollaborationServicesByPriceRange(min, max));
    }

    @GetMapping("/region/{region}/domain/{domain}")
    public ResponseEntity<List<CollaborationService>> getByRegionAndDomain(
            @PathVariable Region region,
            @PathVariable String domain) {
        return ResponseEntity.ok(service.getCollaborationServicesByRegionAndDomain(region, domain));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<CollaborationService>> getByCollaborationType(@PathVariable String type) {
        return ResponseEntity.ok(service.getCollaborationServicesByCollaborationType(type));
    }

    @GetMapping("/domain/{domain}")
    public ResponseEntity<List<CollaborationService>> getByActivityDomain(@PathVariable String domain) {
        return ResponseEntity.ok(service.getCollaborationServicesByActivityDomain(domain));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CollaborationService>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(service.searchCollaborationServices(keyword));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", service.getAllCollaborationServices().size());
        stats.put("pending", service.getPendingCollaborationServices().size());
        stats.put("approved", service.getApprovedCollaborationServices().size());
        stats.put("rejected", service.getRejectedCollaborationServices().size());
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