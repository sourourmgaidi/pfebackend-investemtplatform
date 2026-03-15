package tn.iset.investplatformpfe.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Repository.LocalPartnerRepository;
import tn.iset.investplatformpfe.Service.FileStorageService;
import tn.iset.investplatformpfe.Service.TouristServiceService;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tourist-services")
@CrossOrigin(origins = "http://localhost:4200")
public class TouristServiceController {
    private static final Logger log = LoggerFactory.getLogger(TouristServiceController.class);
    private final TouristServiceService touristServiceService;
    private final LocalPartnerRepository localPartnerRepository;
    private final FileStorageService fileStorageService;

    public TouristServiceController(
            TouristServiceService touristServiceService,
            LocalPartnerRepository localPartnerRepository,
            FileStorageService fileStorageService) { // AJOUTÉ
        this.touristServiceService = touristServiceService;
        this.localPartnerRepository = localPartnerRepository;
        this.fileStorageService = fileStorageService;
    }

    // ================ ENDPOINTS PUBLICS ================

    // ✅ GET les services approuvés (PUBLIC)
    @GetMapping("/approved")
    public ResponseEntity<List<TouristService>> getApprovedServices() {
        return ResponseEntity.ok(touristServiceService.getApprovedServices());
    }

    // ================ ENDPOINTS ADMIN (protégés) ================

    // ✅ GET tous les services (ADMIN uniquement)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<TouristService>> getAllServices() {
        return ResponseEntity.ok(touristServiceService.getAllServices());
    }

    // ✅ GET services en attente (ADMIN uniquement)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/pending")
    public ResponseEntity<List<TouristService>> getPendingServices() {
        return ResponseEntity.ok(touristServiceService.getPendingServices());
    }

    // ✅ PUT approuver un service (ADMIN uniquement)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/approve")
    public ResponseEntity<TouristService> approveService(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(touristServiceService.approveService(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ✅ PUT rejeter un service (ADMIN uniquement)
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectService(@PathVariable Long id) {
        try {
            TouristService rejected = touristServiceService.rejectService(id);
            return ResponseEntity.ok(Map.of(
                    "message", "Service rejeté (en attente de suppression par le partenaire)",
                    "service", rejected
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ================ ENDPOINTS PARTENAIRE (avec authentification) ================

    // ✅ GET tous les services du partenaire connecté
    @GetMapping("/my-services")
    public ResponseEntity<List<TouristService>> getMyServices(@AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        return ResponseEntity.ok(touristServiceService.getServicesByProviderEmail(userEmail));
    }

    // ✅ GET un service par ID (vérification que c'est le sien)
    @GetMapping("/{id}")
    public ResponseEntity<TouristService> getServiceById(@PathVariable Long id,
                                                         @AuthenticationPrincipal Jwt jwt) {
        try {
            String userEmail = jwt.getClaimAsString("email");
            TouristService service = touristServiceService.getServiceByIdForPartner(id, userEmail);
            return ResponseEntity.ok(service);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).body(null);
        }
    }

    // ✅ GET services rejetés du partenaire connecté
    @GetMapping("/my-rejected")
    public ResponseEntity<List<TouristService>> getMyRejectedServices(@AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        return ResponseEntity.ok(touristServiceService.getRejectedServicesByProviderEmail(userEmail));
    }

    // ✅ GET services par provider (vérification que c'est le sien)
    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<TouristService>> getByProvider(@PathVariable Long providerId,
                                                              @AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        List<TouristService> services = touristServiceService.getTouristServicesByProvider(providerId, userEmail);
        return ResponseEntity.ok(services);
    }

    // ✅ POST créer un service avec notification
    @PostMapping("/create")
    public ResponseEntity<TouristService> createWithNotification(@RequestBody TouristService service,
                                                                 @AuthenticationPrincipal Jwt jwt) {
        String userEmail = jwt.getClaimAsString("email");
        return ResponseEntity.ok(touristServiceService.createServiceWithNotification(service, userEmail));
    }

    // ✅ PUT modifier un service (vérification que c'est le sien et PENDING)
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody TouristService service,
                                    @AuthenticationPrincipal Jwt jwt) {
        try {
            String userEmail = jwt.getClaimAsString("email");
            TouristService updated = touristServiceService.updateService(id, service, userEmail);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ DELETE supprimer un service (PENDING ou REJECTED et propriétaire)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @AuthenticationPrincipal Jwt jwt) {
        try {
            String userEmail = jwt.getClaimAsString("email");
            touristServiceService.deleteService(id, userEmail);
            return ResponseEntity.ok(Map.of("message", "Service supprimé définitivement"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ================ ENDPOINTS POUR LES DEMANDES (PARTENAIRE) ================

    // ✅ Partenaire: Demander la modification d'un service APPROUVÉ
    @PostMapping("/{id}/request-edit")
    public ResponseEntity<?> requestEdit(@PathVariable Long id,
                                         @RequestBody Map<String, String> request,
                                         @AuthenticationPrincipal Jwt jwt) {
        try {
            String userEmail = jwt.getClaimAsString("email");
            String reason = request.get("reason");
            String requestedChanges = request.get("requestedChanges");

            TouristServiceRequest editRequest = touristServiceService.requestEdit(id, userEmail, reason, requestedChanges);

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Demande de modification envoyée à l'admin",
                    "requestId", editRequest.getId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Partenaire: Demander la suppression d'un service APPROUVÉ
    @PostMapping("/{id}/request-delete")
    public ResponseEntity<?> requestDelete(@PathVariable Long id,
                                           @RequestBody Map<String, String> request,
                                           @AuthenticationPrincipal Jwt jwt) {
        try {
            String userEmail = jwt.getClaimAsString("email");
            String reason = request.get("reason");

            TouristServiceRequest deleteRequest = touristServiceService.requestDelete(id, userEmail, reason);

            return ResponseEntity.ok(Map.of(
                    "message", "✅ Demande de suppression envoyée à l'admin",
                    "requestId", deleteRequest.getId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-requests")
    public ResponseEntity<?> getMyRequests(@AuthenticationPrincipal Jwt jwt) {
        try {
            String userEmail = jwt.getClaimAsString("email");
            LocalPartner partner = localPartnerRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new RuntimeException("Partner not found"));

            List<TouristServiceRequest> requests = touristServiceService.getPartnerRequests(partner.getId());

            // ✅ Retourner un objet avec la structure attendue
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "requests", requests,
                    "count", requests.size()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    // ================ ENDPOINTS ADMIN POUR LES DEMANDES ================

    // ✅ Admin: Voir toutes les demandes
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/requests")
    public ResponseEntity<?> getAllRequests() {
        try {
            return ResponseEntity.ok(touristServiceService.getAllRequests());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Admin: Voir les demandes en attente
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/requests/pending")
    public ResponseEntity<?> getPendingRequests() {
        try {
            return ResponseEntity.ok(touristServiceService.getPendingRequests());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Admin: Voir les demandes par type (EDIT ou DELETE)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/requests/type/{type}")
    public ResponseEntity<?> getRequestsByType(@PathVariable RequestType type) {
        try {
            return ResponseEntity.ok(touristServiceService.getRequestsByType(type));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Admin: Voir une demande spécifique
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/requests/{requestId}")
    public ResponseEntity<?> getRequestById(@PathVariable Long requestId) {
        try {
            return ResponseEntity.ok(touristServiceService.getRequestById(requestId));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ✅ Admin: Approuver une demande de modification
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/requests/{requestId}/approve-edit")
    public ResponseEntity<?> approveEditRequest(@PathVariable Long requestId,
                                                @AuthenticationPrincipal Jwt jwt) {
        try {
            String adminEmail = jwt.getClaimAsString("email");
            touristServiceService.approveEditRequest(requestId, adminEmail);
            return ResponseEntity.ok(Map.of(
                    "message", "✅ Demande de modification approuvée",
                    "requestId", requestId
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Admin: Approuver une demande de suppression
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/requests/{requestId}/approve-delete")
    public ResponseEntity<?> approveDeleteRequest(@PathVariable Long requestId,
                                                  @AuthenticationPrincipal Jwt jwt) {
        try {
            String adminEmail = jwt.getClaimAsString("email");
            touristServiceService.approveDeleteRequest(requestId, adminEmail);
            return ResponseEntity.ok(Map.of(
                    "message", "✅ Demande de suppression approuvée",
                    "requestId", requestId
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Admin: Rejeter une demande
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/requests/{requestId}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable Long requestId,
                                           @RequestBody Map<String, String> request,
                                           @AuthenticationPrincipal Jwt jwt) {
        try {
            String adminEmail = jwt.getClaimAsString("email");
            String rejectionReason = request.get("rejectionReason");
            touristServiceService.rejectRequest(requestId, adminEmail, rejectionReason);
            return ResponseEntity.ok(Map.of(
                    "message", "✅ Demande rejetée",
                    "requestId", requestId
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ✅ Admin: Statistiques des demandes
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/requests/stats")
    public ResponseEntity<?> getRequestStatistics() {
        try {
            return ResponseEntity.ok(touristServiceService.getRequestStatistics());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    // Ajoutez cette méthode dans TouristServiceController.java

    // ✅ Partenaire: Annuler sa propre demande
    @DeleteMapping("/requests/{requestId}/cancel")
    public ResponseEntity<?> cancelOwnRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal Jwt jwt) {

        log.info("📥 DELETE /requests/{}/cancel - Cancelling request", requestId);

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            String userEmail = jwt.getClaimAsString("email");

            // Appel à la méthode du service
            touristServiceService.cancelOwnRequest(requestId, userEmail);

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

    private boolean hasRole(Jwt jwt, String role) {
        if (jwt == null) return false;
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles != null && roles.contains(role);
        }
        return false;
    }
    // ========================================
// CREATE avec fichiers (multipart)
// ========================================
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createTouristServiceWithFiles(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("service") String serviceJson,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les partenaires locaux peuvent créer des services touristiques"));
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            TouristService touristServiceData = mapper.readValue(serviceJson, TouristService.class);

            String email = jwt.getClaimAsString("email");
            TouristService created = this.touristServiceService.createServiceWithNotification(touristServiceData, email);

            if (files != null && files.length > 0) {
                List<TouristServiceDocument> documents = new ArrayList<>();
                for (int i = 0; i < files.length; i++) {
                    MultipartFile file = files[i];
                    if (!file.isEmpty()) {
                        String fileName = fileStorageService.storeFile(file, FileStorageService.ServiceType.TOURIST);
                        TouristServiceDocument doc = TouristServiceDocument.builder()
                                .fileName(file.getOriginalFilename())
                                .fileType(file.getContentType())
                                .fileSize(file.getSize())
                                .filePath(fileName)
                                .downloadUrl("/api/tourist-services/files/" + fileName)
                                .isPrimary(i == 0)
                                .build();
                        documents.add(doc);
                    }
                }
                if (!documents.isEmpty()) {
                    this.touristServiceService.addDocumentsToService(created.getId(), documents);
                }
            }

            TouristService result = this.touristServiceService.getServiceById(created.getId());
            return new ResponseEntity<>(result, HttpStatus.CREATED);

        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }


    // ========================================
// UPDATE avec fichiers (multipart)
// ========================================
    @PutMapping(value = "/{id}", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> updateTouristServiceWithFiles(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestPart(value = "service", required = false) String serviceJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les partenaires locaux peuvent modifier des services touristiques"));
        }

        String email = jwt.getClaimAsString("email");

        try {
            TouristService existingService = this.touristServiceService.getServiceById(id);

            if (!existingService.getProvider().getEmail().equals(email)) {
                return ResponseEntity.status(403).body(Map.of("error",
                        "Vous ne pouvez modifier que vos propres services"));
            }

            // ✅ SAUVEGARDER L'ÉTAT AVANT MODIFICATION
            boolean wasEditAuthorized = existingService.isEditAuthorized();
            boolean wasApprovedService = existingService.getStatus() == ServiceStatus.APPROVED && wasEditAuthorized;

            log.info("📝 État avant modification - Status: {}, editAuthorized: {}, wasApprovedService: {}",
                    existingService.getStatus(), wasEditAuthorized, wasApprovedService);

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

            TouristService serviceDetails = null;
            boolean hasTextUpdates = false;

            // ✅ Mettre à jour les champs texte si fournis
            if (serviceJson != null && !serviceJson.trim().isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                serviceDetails = mapper.readValue(serviceJson, TouristService.class);
                existingService = touristServiceService.updateService(id, serviceDetails, email);
                hasTextUpdates = true;
                log.info("✅ Champs texte mis à jour");
            }

            // Traiter les nouveaux fichiers
            List<TouristServiceDocument> newDocuments = new ArrayList<>();
            boolean hasFileUpdates = false;

            if (files != null && !files.isEmpty()) {
                log.info("📎 Traitement de {} fichier(s)", files.size());
                hasFileUpdates = true;

                for (int i = 0; i < files.size(); i++) {
                    MultipartFile file = files.get(i);
                    if (file != null && !file.isEmpty()) {
                        String fileName = fileStorageService.storeFile(file, FileStorageService.ServiceType.TOURIST);
                        TouristServiceDocument doc = TouristServiceDocument.builder()
                                .fileName(file.getOriginalFilename())
                                .fileType(file.getContentType())
                                .fileSize(file.getSize())
                                .filePath(fileName)
                                .downloadUrl("/api/tourist-services/files/" + fileName)
                                .isPrimary(i == 0 && existingService.getDocuments().isEmpty())
                                .build();
                        newDocuments.add(doc);
                    }
                }

                // Ajouter les nouveaux documents
                for (TouristServiceDocument doc : newDocuments) {
                    this.touristServiceService.addDocumentToService(id, doc);
                }
                log.info("✅ {} documents ajoutés", newDocuments.size());
            }

            // ✅ RÉCUPÉRER LE SERVICE APRÈS TOUTES LES MODIFICATIONS
            TouristService result = this.touristServiceService.getServiceById(id);

            // ✅ SI C'ÉTAIT UN SERVICE APPROUVÉ AVEC AUTORISATION ET QU'IL Y A EU DES MODIFICATIONS
            if (wasApprovedService && (hasTextUpdates || hasFileUpdates)) {
                log.info("🔄 Réinitialisation de l'autorisation pour le service ID: {} (modifications effectuées)", id);

                // Réinitialiser l'autorisation
                result.setEditAuthorizedUntil(null);
                result.setAuthorizedByAdminId(null);

                // Sauvegarder les changements
                result = touristServiceService.save(result);

                log.info("✅ Autorisation réinitialisée - editAuthorizedUntil: null");
            }

            log.info("📝 État final - editAuthorizedUntil: {}, deleteAuthorized: {}",
                    result.getEditAuthorizedUntil(), result.getDeleteAuthorized());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
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
            java.nio.file.Path filePath = fileStorageService.getFilePath(fileName, FileStorageService.ServiceType.TOURIST);

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
// DELETE DOCUMENT - Supprimer un document spécifique
// ========================================
    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<?> deleteDocument(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long documentId) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les partenaires locaux peuvent supprimer des documents"));
        }

        try {
            String email = jwt.getClaimAsString("email");

            // Récupérer le document
            TouristServiceDocument document = touristServiceService.getDocumentById(documentId);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }

            // Récupérer le service associé
            TouristService touristService = document.getTouristService();

            // Vérifier que le partenaire est bien le propriétaire du service
            if (!touristService.getProvider().getEmail().equals(email)) {
                return ResponseEntity.status(403).body(Map.of("error",
                        "Vous ne pouvez supprimer que les documents de vos propres services"));
            }

            // ✅ Vérifier l'autorisation de modification pour les services approuvés
            if (touristService.getStatus() == ServiceStatus.APPROVED) {
                if (!touristService.isEditAuthorized()) {
                    return ResponseEntity.status(403).body(Map.of("error",
                            "Vous n'avez pas l'autorisation de modifier ce service"));
                }
                log.info("✅ Suppression de document autorisée pour service approuvé");
            }

            // Supprimer le fichier physique
            fileStorageService.deleteFile(document.getFilePath(), FileStorageService.ServiceType.TOURIST);

            // Supprimer de la base de données
            touristServiceService.deleteDocument(documentId);

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