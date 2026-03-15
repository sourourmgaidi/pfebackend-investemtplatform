package tn.iset.investplatformpfe.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Service.*;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RestController
@RequestMapping("/api/collaboration-services")
public class CollaborationServiceController {

    private static final Logger log = LoggerFactory.getLogger(CollaborationServiceController.class);

    private final CollaborationServiceService service;
    private final LocalPartnerAuthService localPartnerAuthService;
    private final NotificationService notificationService;
    private final UserService userService;
    private final FileStorageService fileStorageService;
    @Value("${collaboration.service.upload-dir}")
    private String uploadDir;


    public CollaborationServiceController(
            CollaborationServiceService service,
            LocalPartnerAuthService localPartnerAuthService,
            NotificationService notificationService,
            UserService userService,FileStorageService fileStorageService) {
        this.service = service;
        this.localPartnerAuthService = localPartnerAuthService;
        this.notificationService = notificationService;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    // ========================================
    // CREATE - Réservé aux LOCAL_PARTNER
    // ========================================
    @PostMapping("/with-provider/{providerId}")
    public ResponseEntity<?> createCollaborationServiceWithProvider(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long providerId,
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
            Map<String, Object> partner = localPartnerAuthService.getProfile(email);
            Long partnerId = (Long) partner.get("id");

            if (!partnerId.equals(providerId)) {
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
            CollaborationService created = service.createCollaborationService(email, serviceData);
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
            String email = jwt.getClaimAsString("email");
            boolean isAdmin = hasRole(jwt, "ADMIN");

            service.deleteCollaborationService(id, email, isAdmin);
            return ResponseEntity.ok(Map.of("message", "Service supprimé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // DELETE - Service rejeté (par le partenaire local)
    // ========================================
    @DeleteMapping("/rejected/{id}")
    public ResponseEntity<?> deleteRejectedService(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les partenaires locaux peuvent supprimer leurs services rejetés"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            service.deleteRejectedService(id, email);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Service rejeté supprimé avec succès"
            ));
        } catch (Exception e) {
            log.error("❌ Erreur suppression service rejeté: {}", e.getMessage());
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
            return ResponseEntity.ok(approved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // APPROVE AND NOTIFY - Réservé aux ADMIN
    // ========================================
    @PutMapping("/{id}/approve-and-notify")
    public ResponseEntity<?> approveAndNotifyService(
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
            // Les notifications sont déjà dans la méthode approveService
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

    @GetMapping("/region/{regionId}")
    public ResponseEntity<List<CollaborationService>> getByRegionId(@PathVariable Long regionId) {
        return ResponseEntity.ok(service.getCollaborationServicesByRegionId(regionId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<CollaborationService>> getByStatus(@PathVariable ServiceStatus status) {
        return ResponseEntity.ok(service.getCollaborationServicesByStatus(status));
    }

    @GetMapping("/availability/{availability}")
    public ResponseEntity<List<CollaborationService>> getByAvailability(@PathVariable Availability availability) {
        return ResponseEntity.ok(service.getCollaborationServicesByAvailability(availability));
    }

    @GetMapping("/budget-range")
    public ResponseEntity<List<CollaborationService>> getByBudgetRange(
            @RequestParam(required = false) BigDecimal min,
            @RequestParam(required = false) BigDecimal max) {
        if (min == null && max == null) {
            return ResponseEntity.ok(service.getAllCollaborationServices());
        }
        return ResponseEntity.ok(service.getCollaborationServicesByMaxBudget(max != null ? max : BigDecimal.valueOf(Double.MAX_VALUE)));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<CollaborationService>> getByCollaborationType(@PathVariable CollaborationType type) {
        return ResponseEntity.ok(service.getCollaborationServicesByCollaborationType(type));
    }

    @GetMapping("/domain/{domain}")
    public ResponseEntity<List<CollaborationService>> getByActivityDomain(@PathVariable ActivityDomain domain) {
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
    // ENDPOINTS POUR LES DEMANDES (REQUESTS)
    // ========================================

    @PostMapping("/requests/edit/{serviceId}")
    public ResponseEntity<?> requestEdit(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serviceId,
            @RequestBody Map<String, String> request) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux partenaires locaux"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            String reason = request.get("reason");
            String changes = request.get("changes");

            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "La raison est obligatoire"));
            }

            CollaborationServiceRequest created = service.requestEdit(serviceId, email, reason, changes);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Demande de modification envoyée à l'admin",
                    "requestId", created.getId(),
                    "status", created.getStatus(),
                    "requestDate", created.getRequestDate()
            ));
        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/requests/delete/{serviceId}")
    public ResponseEntity<?> requestDelete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serviceId,
            @RequestBody Map<String, String> request) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux partenaires locaux"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            String reason = request.get("reason");

            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "La raison est obligatoire"));
            }

            CollaborationServiceRequest created = service.requestDelete(serviceId, email, reason);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Demande de suppression envoyée à l'admin",
                    "requestId", created.getId(),
                    "status", created.getStatus(),
                    "requestDate", created.getRequestDate()
            ));
        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/requests/partner/my-requests")
    public ResponseEntity<?> getMyRequests(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Long partnerId = userService.getUserIdByEmailAndRole(email, Role.LOCAL_PARTNER);

            List<CollaborationServiceRequest> requests = service.getPartnerRequests(partnerId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "requests", requests,
                    "count", requests.size()
            ));
        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/requests/partner/{requestId}")
    public ResponseEntity<?> getRequestById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long requestId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            CollaborationServiceRequest request = service.getRequestById(requestId);

            String email = jwt.getClaimAsString("email");
            Long partnerId = userService.getUserIdByEmailAndRole(email, Role.LOCAL_PARTNER);

            if (!request.getPartner().getId().equals(partnerId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Vous n'avez pas accès à cette demande"));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "request", request
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // ADMIN ENDPOINTS FOR REQUESTS - MODIFIÉS POUR SUPPRESSION AUTOMATIQUE
    // ========================================

    @GetMapping("/requests/admin/pending")
    public ResponseEntity<?> getPendingRequests(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux admins"));
        }

        try {
            List<CollaborationServiceRequest> requests = service.getPendingRequests();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "requests", requests,
                    "count", requests.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/requests/admin/all")
    public ResponseEntity<?> getAllRequests(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) RequestType type,
            @RequestParam(required = false) ServiceStatus status) {

        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux admins"));
        }

        try {
            List<CollaborationServiceRequest> requests;
            if (type != null && status != null) {
                requests = service.getRequestsByTypeAndStatus(type, status);
            } else if (type != null) {
                requests = service.getRequestsByType(type);
            } else if (status != null) {
                requests = service.getRequestsByStatus(status);
            } else {
                requests = service.getPendingRequests();
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "requests", requests,
                    "count", requests.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/requests/admin/{requestId}")
    public ResponseEntity<?> getAdminRequestById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long requestId) {

        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux admins"));
        }

        try {
            CollaborationServiceRequest request = service.getRequestById(requestId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "request", request
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Admin: Approve an edit request - La demande est automatiquement supprimée après approbation
     */
    @PostMapping("/requests/admin/{requestId}/approve-edit")
    public ResponseEntity<?> approveEditRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long requestId) {

        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux admins"));
        }

        try {
            String adminEmail = jwt.getClaimAsString("email");
            service.approveEditRequest(requestId, adminEmail);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Demande de modification approuvée et supprimée",
                    "requestId", requestId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Admin: Approve a delete request - La demande est automatiquement supprimée après approbation
     */
    @PostMapping("/requests/admin/{requestId}/approve-delete")
    public ResponseEntity<?> approveDeleteRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long requestId) {

        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux admins"));
        }

        try {
            String adminEmail = jwt.getClaimAsString("email");
            service.approveDeleteRequest(requestId, adminEmail);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Demande de suppression approuvée et supprimée",
                    "requestId", requestId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Admin: Reject a request - La demande est automatiquement supprimée après rejet
     */
    @PostMapping("/requests/admin/{requestId}/reject")
    public ResponseEntity<?> rejectRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long requestId,
            @RequestBody(required = false) Map<String, String> body) {

        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux admins"));
        }

        try {
            String adminEmail = jwt.getClaimAsString("email");
            service.rejectRequest(requestId, adminEmail);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Demande rejetée et supprimée",
                    "requestId", requestId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/requests/admin/count-pending")
    public ResponseEntity<?> countPendingRequests(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux admins"));
        }

        try {
            long count = service.getPendingRequestsCount();
            long editCount = service.getPendingEditRequestsCount();
            long deleteCount = service.getPendingDeleteRequestsCount();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "total", count,
                    "editRequests", editCount,
                    "deleteRequests", deleteCount
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/requests/admin/statistics")
    public ResponseEntity<?> getRequestStatistics(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux admins"));
        }

        try {
            Map<String, Object> stats = service.getRequestStatistics();
            return ResponseEntity.ok(Map.of("success", true, "statistics", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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

    @DeleteMapping("/requests/partner/{requestId}/cancel")
    public ResponseEntity<?> cancelRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long requestId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux partenaires locaux"));
        }

        try {
            String email = jwt.getClaimAsString("email");

            // ✅ APPEL À LA NOUVELLE MÉTHODE
            service.cancelRequest(requestId, email);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Demande annulée avec succès"
            ));
        } catch (RuntimeException e) {
            log.error("❌ Error cancelling request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("❌ Unexpected error: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "error", "Une erreur inattendue est survenue"
            ));
        }
    }

    // ========================================
// CREATE avec fichiers (multipart)
// ========================================
// CREATE avec fichiers (multipart)
// ========================================
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createCollaborationServiceWithFiles(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("service") String serviceJson,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les partenaires locaux peuvent créer des services"));
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            CollaborationService collaborationServiceData = mapper.readValue(serviceJson, CollaborationService.class);

            String email = jwt.getClaimAsString("email");
            // ✅ CORRECTION : utiliser this.service (le service injecté)
            CollaborationService created = this.service.createCollaborationService(email, collaborationServiceData);

            if (files != null && files.length > 0) {
                List<CollaborationServiceDocument> documents = new ArrayList<>();
                for (int i = 0; i < files.length; i++) {
                    MultipartFile file = files[i];
                    if (!file.isEmpty()) {
                        String fileName = fileStorageService.storeFile(file, FileStorageService.ServiceType.COLLABORATION);
                        CollaborationServiceDocument doc = CollaborationServiceDocument.builder()
                                .fileName(file.getOriginalFilename())
                                .fileType(file.getContentType())
                                .fileSize(file.getSize())
                                .filePath(fileName)
                                .downloadUrl("/api/collaboration-services/files/" + fileName)
                                .isPrimary(i == 0)
                                .build();
                        documents.add(doc);
                    }
                }
                if (!documents.isEmpty()) {
                    // ✅ CORRECTION : utiliser this.service
                    this.service.addDocumentsToService(created.getId(), documents);
                }
            }

            // ✅ CORRECTION : utiliser this.service
            CollaborationService result = this.service.getCollaborationServiceById(created.getId());
            return new ResponseEntity<>(result, HttpStatus.CREATED);

        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
// Télécharger un fichier
// ========================================
    @GetMapping("/files/{fileName:.+}")
    public ResponseEntity<org.springframework.core.io.Resource> downloadFile(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String fileName) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(null);
        }

        try {
            java.nio.file.Path filePath = fileStorageService.getFilePath(fileName, FileStorageService.ServiceType.COLLABORATION);

            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(fileName);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("❌ Erreur téléchargement fichier: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private String determineContentType(String fileName) {
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".gif")) return "image/gif";
        if (fileName.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }
    // ========================================
// UPDATE avec fichiers (multipart)
// ========================================
// ========================================
// UPDATE avec fichiers (multipart)
// ========================================
    @PutMapping(value = "/{id}", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> updateCollaborationServiceWithFiles(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestPart(value = "service", required = false) String serviceJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les partenaires locaux peuvent modifier des services"));
        }

        String email = jwt.getClaimAsString("email");

        try {
            CollaborationService existingService = this.service.getCollaborationServiceById(id);

            if (!existingService.getProvider().getEmail().equals(email)) {
                return ResponseEntity.status(403).body(Map.of("error",
                        "Vous ne pouvez modifier que vos propres services"));
            }

            // Vérification des autorisations
            boolean canModify = false;
            if (existingService.getStatus() == ServiceStatus.PENDING) {
                canModify = true;
            } else if (existingService.getStatus() == ServiceStatus.APPROVED) {
                if (!existingService.isEditAuthorized()) {
                    return ResponseEntity.status(403).body(Map.of("error",
                            "Vous n'avez pas l'autorisation de modifier ce service"));
                }
                canModify = true;
            }

            if (!canModify) {
                return ResponseEntity.status(403).body(Map.of("error",
                        "Ce service ne peut pas être modifié dans son état actuel"));
            }

            CollaborationService serviceDetails = null;

            // ✅ Toujours mettre à jour les champs texte si fournis
            if (serviceJson != null && !serviceJson.trim().isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                serviceDetails = mapper.readValue(serviceJson, CollaborationService.class);

                // Mettre à jour le service avec les nouvelles données
                existingService = service.updateCollaborationService(id, serviceDetails, email);
                log.info("✅ Champs texte mis à jour");
            }

            // Traiter les nouveaux fichiers
            List<CollaborationServiceDocument> newDocuments = new ArrayList<>();
            if (files != null && !files.isEmpty()) {
                log.info("📎 Traitement de {} fichier(s)", files.size());

                for (int i = 0; i < files.size(); i++) {
                    MultipartFile file = files.get(i);
                    if (file != null && !file.isEmpty()) {
                        String fileName = fileStorageService.storeFile(file, FileStorageService.ServiceType.COLLABORATION);
                        CollaborationServiceDocument doc = CollaborationServiceDocument.builder()
                                .fileName(file.getOriginalFilename())
                                .fileType(file.getContentType())
                                .fileSize(file.getSize())
                                .filePath(fileName)
                                .downloadUrl("/api/collaboration-services/files/" + fileName)
                                .isPrimary(i == 0 && existingService.getDocuments().isEmpty())
                                .build();
                        newDocuments.add(doc);
                    }
                }

                // Ajouter les nouveaux documents
                for (CollaborationServiceDocument doc : newDocuments) {
                    this.service.addDocumentToService(id, doc);
                }
                log.info("✅ {} documents ajoutés", newDocuments.size());
            }

            CollaborationService result = this.service.getCollaborationServiceById(id);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    // ========================================
// DELETE DOCUMENT - Supprimer un document spécifique
// ========================================
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<?> deleteDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long documentId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        // ✅ Seulement LOCAL_PARTNER peut supprimer des documents
        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les partenaires locaux peuvent supprimer des documents"));
        }

        try {
            String email = jwt.getClaimAsString("email");

            // Récupérer le document
            CollaborationServiceDocument document = service.getDocumentById(documentId);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }

            // Récupérer le service associé
            CollaborationService collaborationService = document.getCollaborationService();

            // Vérifier que le partenaire est bien le propriétaire du service
            if (!collaborationService.getProvider().getEmail().equals(email)) {
                return ResponseEntity.status(403).body(Map.of("error",
                        "Vous ne pouvez supprimer que les documents de vos propres services"));
            }

            // ✅ Vérifier l'autorisation de modification pour les services approuvés
            if (collaborationService.getStatus() == ServiceStatus.APPROVED) {
                if (!collaborationService.isEditAuthorized()) {
                    return ResponseEntity.status(403).body(Map.of("error",
                            "Vous n'avez pas l'autorisation de modifier ce service"));
                }
                log.info("✅ Suppression de document autorisée pour service approuvé");
            }

            // Supprimer le fichier physique
            fileStorageService.deleteFile(document.getFilePath(), FileStorageService.ServiceType.COLLABORATION);

            // Supprimer de la base de données
            service.deleteDocument(documentId);

            // ❌ LIGNES SUPPRIMÉES - NE PAS RÉVOQUER L'AUTORISATION ICI
            // L'autorisation sera révoquée dans updateCollaborationService quand le partenaire soumettra ses modifications

            log.info("✅ Document {} supprimé avec succès", documentId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Document supprimé avec succès"
            ));

        } catch (Exception e) {
            log.error("❌ Erreur suppression document: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}