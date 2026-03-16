package tn.iset.investplatformpfe.Controller;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Dto.RejectRequestDto;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Service.InvestmentServiceService;
import tn.iset.investplatformpfe.Service.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/service-requests")
public class ServiceRequestController {

    private final InvestmentServiceService investmentService;
    private final UserService userService;

    public ServiceRequestController(InvestmentServiceService investmentService, UserService userService) {
        this.investmentService = investmentService;
        this.userService = userService;
    }

    // ========================================
    // PARTIE 1: ENDPOINTS POUR PARTENAIRE LOCAL
    // ========================================

    /**
     * Partenaire local : Demander la modification d'un service
     * POST /api/service-requests/edit/{serviceId}
     */
    @PostMapping("/edit/{serviceId}")
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

            ServiceRequest serviceRequest = investmentService.requestEdit(serviceId, email, reason, changes);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Demande de modification envoyée à l'admin",
                    "requestId", serviceRequest.getId(),
                    "status", serviceRequest.getStatus(),
                    "requestDate", serviceRequest.getRequestDate()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "success", false
            ));
        }
    }

    /**
     * Partenaire local : Demander la suppression d'un service
     * POST /api/service-requests/delete/{serviceId}
     */
    @PostMapping("/delete/{serviceId}")
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

            ServiceRequest serviceRequest = investmentService.requestDelete(serviceId, email, reason);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Demande de suppression envoyée à l'admin",
                    "requestId", serviceRequest.getId(),
                    "status", serviceRequest.getStatus(),
                    "requestDate", serviceRequest.getRequestDate()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "success", false
            ));
        }
    }

    /**
     * Partenaire local : Voir mes demandes
     * GET /api/service-requests/partner/my-requests
     */
    @GetMapping("/partner/my-requests")
    public ResponseEntity<?> getMyRequests(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux partenaires locaux"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            Long partnerId = userService.getUserIdByEmailAndRole(email, Role.LOCAL_PARTNER);

            List<ServiceRequest> requests = investmentService.getPartnerRequests(partnerId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "requests", requests,
                    "count", requests.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "success", false
            ));
        }
    }

    /**
     * Partenaire local : Voir le détail d'une demande
     * GET /api/service-requests/partner/{requestId}
     */
    @GetMapping("/partner/{requestId}")
    public ResponseEntity<?> getRequestById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long requestId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            // Vérifier que l'utilisateur a accès à cette demande
            String email = jwt.getClaimAsString("email");
            String userRole = getUserRole(jwt);

            ServiceRequest request = investmentService.getRequestById(requestId); // À ajouter dans le service

            if (userRole.equals("LOCAL_PARTNER")) {
                Long partnerId = userService.getUserIdByEmailAndRole(email, Role.LOCAL_PARTNER);
                if (!request.getPartner().getId().equals(partnerId)) {
                    return ResponseEntity.status(403).body(Map.of("error", "Vous n'avez pas accès à cette demande"));
                }
            } else if (!userRole.equals("ADMIN")) {
                return ResponseEntity.status(403).body(Map.of("error", "Accès non autorisé"));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "request", request
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "success", false
            ));
        }
    }

    /**
     * Partenaire local : Annuler une demande en attente
     * DELETE /api/service-requests/partner/{requestId}/cancel
     */
    @DeleteMapping("/partner/{requestId}/cancel")
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
            Long partnerId = userService.getUserIdByEmailAndRole(email, Role.LOCAL_PARTNER);

            investmentService.cancelRequest(requestId, partnerId); // À ajouter dans le service

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Demande annulée avec succès"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "success", false
            ));
        }
    }

    // ========================================
    // PARTIE 2: ENDPOINTS POUR ADMIN
    // ========================================

    /**
     * Admin : Voir toutes les demandes en attente
     * GET /api/service-requests/admin/pending
     */
    @GetMapping("/admin/pending")
    public ResponseEntity<?> getPendingRequests(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux admins"));
        }

        try {
            List<ServiceRequest> requests = investmentService.getPendingRequests();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "requests", requests,
                    "count", requests.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "success", false
            ));
        }
    }

    /**
     * Admin : Voir toutes les demandes (avec filtres optionnels)
     * GET /api/service-requests/admin/all?type=EDIT&status=PENDING
     */
    @GetMapping("/admin/all")
    public ResponseEntity<?> getAllRequests(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) RequestType type,
            @RequestParam(required = false) ServiceStatus status) {

        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux admins"));
        }

        try {
            List<ServiceRequest> requests;
            if (type != null && status != null) {
                requests = investmentService.getRequestsByTypeAndStatus(type, status); // À ajouter
            } else if (type != null) {
                requests = investmentService.getRequestsByType(type);
            } else if (status != null) {
                requests = investmentService.getRequestsByStatus(status); // À ajouter
            } else {
                requests = investmentService.getAllRequests(); // À ajouter
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "requests", requests,
                    "count", requests.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "success", false
            ));
        }
    }

    /**
     * Admin : Voir les demandes par type
     * GET /api/service-requests/admin/type/{type}
     */
    @GetMapping("/admin/type/{type}")
    public ResponseEntity<?> getRequestsByType(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String type) {

        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux admins"));
        }

        try {
            RequestType requestType = RequestType.valueOf(type.toUpperCase());
            List<ServiceRequest> requests = investmentService.getRequestsByType(requestType);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "requests", requests,
                    "count", requests.size()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Type de demande invalide. Utilisez EDIT ou DELETE"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "success", false
            ));
        }
    }

    /**
     * Admin : Voir le détail d'une demande
     * GET /api/service-requests/admin/{requestId}
     */
    @GetMapping("/admin/{requestId}")
    public ResponseEntity<?> getAdminRequestById(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long requestId) {

        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux admins"));
        }

        try {
            ServiceRequest request = investmentService.getRequestById(requestId); // À ajouter dans le service

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "request", request
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "success", false
            ));
        }
    }

    /**
     * Admin : Approuver une demande de modification
     * POST /api/service-requests/admin/{requestId}/approve-edit
     */
    @PostMapping("/admin/{requestId}/approve-edit")
    public ResponseEntity<?> approveEditRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long requestId,
            @RequestBody(required = false) Map<String, String> body) {

        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux admins"));
        }

        try {
            String adminEmail = jwt.getClaimAsString("email");
            investmentService.approveEditRequest(requestId, adminEmail);  // Plus de retour attendu

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Demande de modification approuvée et supprimée",
                    "requestId", requestId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "success", false
            ));
        }
    }

    /**
     * Admin : Approuver une demande de suppression
     * POST /api/service-requests/admin/{requestId}/approve-delete
     */
    @PostMapping("/admin/{requestId}/approve-delete")
    public ResponseEntity<?> approveDeleteRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long requestId,
            @RequestBody(required = false) Map<String, String> body) {

        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux admins"));
        }

        try {
            String adminEmail = jwt.getClaimAsString("email");
            investmentService.approveDeleteRequest(requestId, adminEmail);  // Plus de retour attendu

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Demande de suppression approuvée et supprimée",
                    "requestId", requestId
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "success", false
            ));
        }
    }

    /**
     * Admin : Rejeter une demande
     * POST /api/service-requests/admin/{requestId}/reject
     */
    @PostMapping("/admin/{requestId}/reject")
    public ResponseEntity<?> rejectRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long requestId,
            @Valid @RequestBody RejectRequestDto rejectRequest) {  // ✅ Utilisation du DTO avec @Valid

        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux admins"));
        }

        try {
            String adminEmail = jwt.getClaimAsString("email");

            // ✅ Appel du service avec la raison (plus besoin de validation manuelle)
            investmentService.rejectRequest(
                    requestId,
                    adminEmail,
                    rejectRequest.getRejectionReason()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Demande rejetée avec succès",
                    "requestId", requestId,
                    "rejectionReason", rejectRequest.getRejectionReason()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "success", false
            ));
        }
    }

    /**
     * Admin : Compter les demandes en attente
     * GET /api/service-requests/admin/count-pending
     */
    @GetMapping("/admin/count-pending")
    public ResponseEntity<?> countPendingRequests(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux admins"));
        }

        try {
            long count = investmentService.getPendingRequestsCount();
            long editCount = investmentService.getPendingEditRequestsCount(); // À ajouter
            long deleteCount = investmentService.getPendingDeleteRequestsCount(); // À ajouter

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "total", count,
                    "editRequests", editCount,
                    "deleteRequests", deleteCount
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "success", false
            ));
        }
    }

    /**
     * Admin : Obtenir des statistiques sur les demandes
     * GET /api/service-requests/admin/statistics
     */
    @GetMapping("/admin/statistics")
    public ResponseEntity<?> getRequestStatistics(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux admins"));
        }

        try {
            Map<String, Object> stats = investmentService.getRequestStatistics(); // À ajouter

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "statistics", stats
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "success", false
            ));
        }
    }

    // ========================================
    // PARTIE 3: ENDPOINTS PUBLICS (pour les notifications)
    // ========================================

    /**
     * Marquer une notification comme lue
     * POST /api/service-requests/notifications/{notificationId}/read
     */
    @PostMapping("/notifications/{notificationId}/read")
    public ResponseEntity<?> markNotificationAsRead(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long notificationId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            // À implémenter si vous avez un service de notification
            // notificationService.markAsRead(notificationId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Notification marquée comme lue"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "success", false
            ));
        }
    }

    /**
     * Récupérer les notifications de l'utilisateur connecté
     * GET /api/service-requests/notifications
     */
    @GetMapping("/notifications")
    public ResponseEntity<?> getUserNotifications(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            String userRole = getUserRole(jwt);
            Long userId = userService.getUserIdByEmailAndRole(email, Role.valueOf(userRole));

            // À implémenter
            // List<Notification> notifications = notificationService.getUserNotifications(Role.valueOf(userRole), userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "notifications", List.of(), // Remplacer par la vraie liste
                    "count", 0
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "success", false
            ));
        }
    }

    // ========================================
    // MÉTHODES UTILITAIRES
    // ========================================

    /**
     * Vérifier si l'utilisateur a un rôle spécifique
     */
    private boolean hasRole(Jwt jwt, String role) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles != null && roles.contains(role);
        }
        return false;
    }

    /**
     * Récupérer le rôle principal de l'utilisateur
     */
    private String getUserRole(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles != null) {
                // Ordre de priorité des rôles
                String[] rolePriority = {"ADMIN", "LOCAL_PARTNER", "INTERNATIONAL_COMPANY",
                        "PARTNER", "INVESTOR", "TOURIST"};
                for (String priorityRole : rolePriority) {
                    if (roles.contains(priorityRole)) {
                        return priorityRole;
                    }
                }
            }
        }
        return null;
    }
}
