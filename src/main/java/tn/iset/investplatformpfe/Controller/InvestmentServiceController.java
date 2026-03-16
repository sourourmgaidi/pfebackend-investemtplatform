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
import tn.iset.investplatformpfe.Dto.RejectServiceRequest;
import tn.iset.investplatformpfe.Entity.InvestmentService;
import tn.iset.investplatformpfe.Entity.InvestmentServiceDocument;
import tn.iset.investplatformpfe.Entity.ServiceStatus;
import tn.iset.investplatformpfe.Service.FileStorageService;
import tn.iset.investplatformpfe.Service.InvestmentServiceService;
import tn.iset.investplatformpfe.Service.LocalPartnerAuthService;
import tn.iset.investplatformpfe.Service.NotificationService;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/investment-services")
public class InvestmentServiceController {

    private final InvestmentServiceService investmentService;
    private final LocalPartnerAuthService localPartnerAuthService;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Value("${investment.service.upload-dir}")
    private String uploadDir;

    private static final Logger log = LoggerFactory.getLogger(InvestmentServiceController.class);

    public InvestmentServiceController(
            InvestmentServiceService investmentService,
            FileStorageService fileStorageService,
            LocalPartnerAuthService localPartnerAuthService,
            NotificationService notificationService) {
        this.investmentService = investmentService;
        this.fileStorageService = fileStorageService;
        this.localPartnerAuthService = localPartnerAuthService;
        this.notificationService = notificationService;
    }

    // ========================================
    // CREATE - UNE SEULE MÉTHODE qui gère JSON ET multipart
    // ========================================
    // ========================================
// CREATE - UNE SEULE MÉTHODE qui gère tout
// ========================================
    // ========================================
// CREATE - UNE SEULE MÉTHODE qui gère JSON ET multipart
// ========================================
// ========================================
// CREATE - Version simplifiée pour multipart
// ========================================
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createInvestmentServiceWithFiles(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("service") String serviceJson,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les partenaires locaux peuvent créer des services d'investissement"));
        }

        try {
            // 1. Créer le service SANS documents
            ObjectMapper mapper = new ObjectMapper();
            InvestmentService service = mapper.readValue(serviceJson, InvestmentService.class);

            String email = jwt.getClaimAsString("email");
            InvestmentService created = investmentService.createInvestmentService(service, email);

            log.info("✅ Service créé avec ID: {}", created.getId());

            // 2. Traiter les fichiers et créer les documents
            if (files != null && files.length > 0) {
                List<InvestmentServiceDocument> documents = new ArrayList<>();

                for (int i = 0; i < files.length; i++) {
                    MultipartFile file = files[i];

                    if (!file.isEmpty()) {
                        log.info("📎 Traitement fichier {}: {}", i, file.getOriginalFilename());

                        // Sauvegarder le fichier
                        String fileName = fileStorageService.storeFile(file, FileStorageService.ServiceType.INVESTMENT);

                        // Créer le document
                        InvestmentServiceDocument doc = new InvestmentServiceDocument();
                        doc.setFileName(file.getOriginalFilename());
                        doc.setFileType(file.getContentType());
                        doc.setFileSize(file.getSize());
                        doc.setFilePath(fileName);
                        doc.setDownloadUrl("/api/investment-services/files/" + fileName);
                        doc.setIsPrimary(i == 0);
                        // ⚠️ NE PAS setInvestmentService ici

                        documents.add(doc);
                    }
                }

                // 3. Ajouter les documents au service créé
                if (!documents.isEmpty()) {
                    log.info("📦 Ajout de {} documents au service", documents.size());
                    investmentService.addDocumentsToService(created.getId(), documents);
                }
            }

            // 4. Retourner le service avec ses documents
            InvestmentService result = investmentService.getInvestmentServiceById(created.getId());
            return new ResponseEntity<>(result, HttpStatus.CREATED);

        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
// CREATE - Version JSON (gardez celle-ci aussi)
// ========================================
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createInvestmentServiceJson(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody InvestmentService service) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les partenaires locaux peuvent créer des services d'investissement"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            InvestmentService created = investmentService.createInvestmentService(service, email);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // CREATE WITH PROVIDER - UNE SEULE MÉTHODE
    // ========================================
    // ========================================
// CREATE WITH PROVIDER - UNE SEULE MÉTHODE
// ========================================
    // ========================================
// CREATE WITH PROVIDER - UNE SEULE MÉTHODE qui gère JSON ET multipart
// ========================================
// CREATE WITH PROVIDER - UNE SEULE MÉTHODE corrigée
// ========================================
    @PostMapping(value = "/with-provider/{providerId}", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> createInvestmentServiceWithProvider(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long providerId,
            @RequestPart(value = "service", required = false) String serviceJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @RequestBody(required = false) InvestmentService serviceBody) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les partenaires locaux peuvent créer des services d'investissement"));
        }

        try {
            // Vérifier que le providerId correspond à l'utilisateur connecté
            String email = jwt.getClaimAsString("email");
            Map<String, Object> partner = localPartnerAuthService.getProfile(email);
            Long partnerId = (Long) partner.get("id");

            if (!partnerId.equals(providerId)) {
                return ResponseEntity.status(403).body(Map.of("error",
                        "Vous ne pouvez créer des services que pour votre propre compte"));
            }

            InvestmentService service;
            List<InvestmentServiceDocument> allDocuments = new ArrayList<>();

            // CAS 1: Requête multipart (avec fichiers)
            if (serviceJson != null) {
                log.info("📦 Requête multipart reçue avec service JSON");
                service = objectMapper.readValue(serviceJson, InvestmentService.class);

                // Traiter les fichiers
                if (files != null && !files.isEmpty()) {
                    log.info("📎 {} fichier(s) reçu(s)", files.size());

                    for (int i = 0; i < files.size(); i++) {
                        MultipartFile file = files.get(i);

                        if (!file.isEmpty()) {
                            log.info("📎 Traitement fichier {}: {}", i, file.getOriginalFilename());

                            String fileName = fileStorageService.storeFile(file, FileStorageService.ServiceType.INVESTMENT);

                            InvestmentServiceDocument doc = InvestmentServiceDocument.builder()
                                    .fileName(file.getOriginalFilename())
                                    .fileType(file.getContentType())
                                    .fileSize(file.getSize())
                                    .filePath(fileName)
                                    .downloadUrl("/api/investment-services/files/" + fileName)
                                    .isPrimary(i == 0)
                                    .build();

                            allDocuments.add(doc);
                        }
                    }
                }
            }
            // CAS 2: Requête JSON pure
            else if (serviceBody != null) {
                log.info("📝 Requête JSON reçue");
                service = serviceBody;
            }
            else {
                return ResponseEntity.badRequest().body(Map.of("error", "Données du service manquantes"));
            }

            // Créer d'abord le service SANS documents
            InvestmentService created = investmentService.createInvestmentServiceWithProvider(service, providerId);
            log.info("✅ Service créé avec ID: {}", created.getId());

            // Ajouter les documents après création
            if (!allDocuments.isEmpty()) {
                log.info("📦 Ajout de {} documents au service", allDocuments.size());
                investmentService.addDocumentsToService(created.getId(), allDocuments);
            }

            InvestmentService result = investmentService.getInvestmentServiceById(created.getId());
            return new ResponseEntity<>(result, HttpStatus.CREATED);

        } catch (Exception e) {
            log.error("❌ Erreur création service avec provider: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ========================================
    // UPDATE - UNE SEULE MÉTHODE
    // ========================================
    // ========================================
// UPDATE - UNE SEULE MÉTHODE
// ========================================
    // ========================================
// UPDATE - Version multipart (avec fichiers) - Respecte l'autorisation
// ========================================
    // ========================================
// UPDATE - UNE SEULE MÉTHODE qui gère JSON ET multipart
// ========================================
    // ========================================
// UPDATE - UNE SEULE MÉTHODE qui fonctionne pour les deux types
// ========================================
    // ========================================
// UPDATE - UNE SEULE MÉTHODE simplifiée
// ========================================
    // ========================================
// UPDATE - Version CORRIGÉE qui accepte multipart/form-data
// ========================================
    @PutMapping(value = "/{id}", consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> updateInvestmentService(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestPart(value = "service", required = false) String serviceJson,
            @RequestPart(value = "files", required = false) List<MultipartFile> files) {

        // 1. Vérification authentification
        if (jwt == null) {
            log.error("❌ JWT null - Non authentifié");
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        if (!hasRole(jwt, "LOCAL_PARTNER")) {
            log.error("❌ Rôle incorrect - Pas LOCAL_PARTNER");
            return ResponseEntity.status(403).body(Map.of("error",
                    "Seuls les partenaires locaux peuvent modifier des services"));
        }

        String email = jwt.getClaimAsString("email");
        log.info("🔄 Tentative de modification du service ID: {} par: {}", id, email);
        log.info("📦 serviceJson présent: {}", serviceJson != null);
        log.info("📦 files présents: {}", files != null ? files.size() : 0);

        try {
            // 2. Récupérer le service existant
            InvestmentService existingService = investmentService.getInvestmentServiceById(id);
            log.info("✅ Service existant trouvé: {}", existingService.getTitle());
            log.info("📊 Statut: {}, editAuthorized: {}",
                    existingService.getStatus(), existingService.isEditAuthorized());

            // Vérifier que le service appartient à l'utilisateur
            if (!existingService.getProvider().getEmail().equals(email)) {
                log.error("❌ L'utilisateur {} n'est pas propriétaire du service {}", email, id);
                return ResponseEntity.status(403).body(Map.of("error",
                        "Vous ne pouvez modifier que vos propres services"));
            }

            // ✅ NOUVELLE VÉRIFICATION : Gestion des différents statuts
            boolean canModify = false;
            String modificationMessage = "";

            switch (existingService.getStatus()) {
                case PENDING:
                    canModify = true;
                    modificationMessage = "Service en attente - modification autorisée";
                    break;

                case APPROVED:
                    if (existingService.isEditAuthorized()) {
                        canModify = true;
                        modificationMessage = "Service approuvé avec autorisation - modification autorisée jusqu'au "
                                + existingService.getEditAuthorizedUntil();
                    } else {
                        canModify = false;
                        modificationMessage = "Service approuvé sans autorisation - modification refusée";
                    }
                    break;

                case REJECTED:
                    canModify = false;
                    modificationMessage = "Service rejeté - impossible de modifier";
                    break;

                default:
                    canModify = false;
                    modificationMessage = "Statut inconnu - modification non autorisée";
            }

            if (!canModify) {
                log.error("❌ {}", modificationMessage);
                return ResponseEntity.status(403).body(Map.of(
                        "error", modificationMessage,
                        "status", existingService.getStatus().toString(),
                        "editAuthorized", existingService.isEditAuthorized(),
                        "editAuthorizedUntil", existingService.getEditAuthorizedUntil() != null ?
                                existingService.getEditAuthorizedUntil().toString() : null
                ));
            }

            log.info("✅ {}", modificationMessage);

            // 3. Déterminer les données du service
            InvestmentService serviceDetails;

            if (serviceJson != null && !serviceJson.trim().isEmpty()) {
                log.info("📄 Désérialisation du service depuis JSON: {}", serviceJson);
                ObjectMapper mapper = new ObjectMapper();
                serviceDetails = mapper.readValue(serviceJson, InvestmentService.class);
            } else {
                log.info("📄 Pas de données service, utilisation d'un objet vide");
                serviceDetails = new InvestmentService();
            }

            // Préserver le statut original et les autorisations
            serviceDetails.setStatus(existingService.getStatus());
            // Important : on ne touche pas aux champs d'autorisation

            // 4. Traiter les nouveaux fichiers
            List<InvestmentServiceDocument> newDocuments = new ArrayList<>();

            if (files != null && !files.isEmpty()) {
                log.info("📎 Traitement de {} fichier(s)", files.size());

                for (int i = 0; i < files.size(); i++) {
                    MultipartFile file = files.get(i);

                    if (file != null && !file.isEmpty()) {
                        log.info("📎 Fichier {}: nom={}, taille={}, type={}",
                                i, file.getOriginalFilename(), file.getSize(), file.getContentType());

                        try {
                            // Valider le type de fichier
                            String contentType = file.getContentType();
                            if (contentType != null && !contentType.startsWith("image/") && !contentType.equals("application/pdf")) {
                                return ResponseEntity.badRequest().body(Map.of("error",
                                        "Type de fichier non supporté: " + contentType +
                                                ". Types acceptés: images (JPEG, PNG, GIF) et PDF"));
                            }

                            // Sauvegarder le fichier
                            String fileName = fileStorageService.storeFile(file, FileStorageService.ServiceType.INVESTMENT);

                            // Créer le document
                            InvestmentServiceDocument doc = InvestmentServiceDocument.builder()
                                    .fileName(file.getOriginalFilename())
                                    .fileType(file.getContentType())
                                    .fileSize(file.getSize())
                                    .filePath(fileName)
                                    .downloadUrl("/api/investment-services/files/" + fileName)
                                    .isPrimary(i == 0 && existingService.getDocuments().isEmpty())
                                    .build();

                            newDocuments.add(doc);
                            log.info("✅ Fichier sauvegardé: {}", fileName);

                        } catch (Exception e) {
                            log.error("❌ Erreur sauvegarde fichier {}: {}", file.getOriginalFilename(), e.getMessage());
                            return ResponseEntity.status(500).body(Map.of("error",
                                    "Erreur lors de la sauvegarde du fichier: " + e.getMessage()));
                        }
                    }
                }
            }

            // 5. Mettre à jour le service (seulement si on a des données à mettre à jour)
            InvestmentService updated = existingService;
            if (serviceJson != null && !serviceJson.trim().isEmpty()) {
                updated = investmentService.updateInvestmentService(id, serviceDetails, email);
                log.info("✅ Service mis à jour avec succès");
            } else {
                log.info("ℹ️ Pas de mise à jour des données du service");
            }

            // 6. Ajouter les nouveaux documents
            if (!newDocuments.isEmpty()) {
                log.info("📦 Ajout de {} nouveau(x) document(s)", newDocuments.size());

                for (InvestmentServiceDocument doc : newDocuments) {
                    investmentService.addDocumentToService(id, doc);
                }
                log.info("✅ Documents ajoutés avec succès");
            }

            // 7. Retourner le service mis à jour
            InvestmentService result = investmentService.getInvestmentServiceById(id);
            log.info("✅ Service retourné avec {} document(s)",
                    result.getDocuments() != null ? result.getDocuments().size() : 0);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Erreur technique: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Erreur technique: " + e.getMessage()
            ));
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
            // ✅ Utiliser le chemin de la configuration
            java.nio.file.Path filePath = fileStorageService.getFilePath(fileName, FileStorageService.ServiceType.INVESTMENT);
            log.info("📁 Recherche fichier: {}", filePath);

            org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(fileName);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                log.error("❌ Fichier non trouvé: {}", filePath);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("❌ Erreur téléchargement fichier: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private String determineContentType(String fileName) {
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".pdf")) {
            return "application/pdf";
        } else {
            return "application/octet-stream";
        }
    }

    // ========================================
    // READ (PUBLIC) - méthodes inchangées
    // ========================================
    @GetMapping
    public ResponseEntity<List<InvestmentService>> getAllInvestmentServices() {
        return ResponseEntity.ok(investmentService.getAllInvestmentServices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getInvestmentServiceById(@PathVariable Long id) {
        try {
            InvestmentService service = investmentService.getInvestmentServiceById(id);
            return ResponseEntity.ok(service);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<InvestmentService>> getByProvider(@PathVariable Long providerId) {
        return ResponseEntity.ok(investmentService.getInvestmentServicesByProviderId(providerId));
    }

    @GetMapping("/region/{regionId}")
    public ResponseEntity<List<InvestmentService>> getByRegion(@PathVariable Long regionId) {
        return ResponseEntity.ok(investmentService.getInvestmentServicesByRegionId(regionId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<InvestmentService>> getByStatus(@PathVariable ServiceStatus status) {
        return ResponseEntity.ok(investmentService.getInvestmentServicesByStatus(status));
    }

    @GetMapping("/rejected")
    public ResponseEntity<List<InvestmentService>> getRejected() {
        return ResponseEntity.ok(investmentService.getInvestmentServicesByStatus(ServiceStatus.REJECTED));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<InvestmentService>> getPending() {
        return ResponseEntity.ok(investmentService.getPendingInvestmentServices());
    }

    @GetMapping("/approved")
    public ResponseEntity<List<InvestmentService>> getApproved() {
        return ResponseEntity.ok(investmentService.getApprovedInvestmentServices());
    }

    @GetMapping("/active")
    public ResponseEntity<List<InvestmentService>> getActive() {
        return ResponseEntity.ok(investmentService.getActiveInvestmentServices());
    }

    @GetMapping("/zone/{zone}")
    public ResponseEntity<List<InvestmentService>> getByZone(@PathVariable String zone) {
        return ResponseEntity.ok(investmentService.getInvestmentServicesByZone(zone));
    }

    // ADMIN: Approbation
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveService(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux administrateurs"));
        }

        try {
            InvestmentService approved = investmentService.approveInvestmentService(id);

            try {
                notificationService.notifyLocalPartnerInvestmentApproved(approved);
                log.info("✅ STEP 1 OK");
            } catch (Exception e) {
                log.error("❌ STEP 1 FAILED: {}", e.getMessage(), e);
            }

            try {
                notificationService.notifyInvestorsNewInvestmentService(approved);
                log.info("✅ STEP 2 OK");
            } catch (Exception e) {
                log.error("❌ STEP 2 FAILED: {}", e.getMessage(), e);
            }

            try {
                notificationService.notifyInternationalCompaniesNewInvestmentService(approved);
                log.info("✅ STEP 3 OK");
            } catch (Exception e) {
                log.error("❌ STEP 3 FAILED: {}", e.getMessage(), e);
            }

            return ResponseEntity.ok(approved);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectService(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id,
            @RequestBody RejectServiceRequest rejectRequest) {

        if (jwt == null || !hasRole(jwt, "ADMIN")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux administrateurs"));
        }

        try {
            // ✅ Récupération de l'email depuis le JWT
            String adminEmail = jwt.getClaimAsString("email");

            // ✅ Appel du service avec les 3 paramètres
            InvestmentService rejected = investmentService.rejectInvestmentService(
                    id,
                    rejectRequest.getRejectionReason(),
                    adminEmail
            );
            return ResponseEntity.ok(rejected);

        } catch (Exception e) {
            log.error("❌ Erreur lors du rejet: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // INVESTOR: Marquer son intérêt
    @PostMapping("/{serviceId}/interest/{investorId}")
    public ResponseEntity<?> markInterest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serviceId,
            @PathVariable Long investorId) {

        if (jwt == null || !hasRole(jwt, "INVESTOR")) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux investisseurs"));
        }

        try {
            InvestmentService service = investmentService.markInterest(serviceId, investorId);
            return ResponseEntity.ok(service);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteInvestmentService(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Non authentifié"));
        }

        try {
            String email = jwt.getClaimAsString("email");
            boolean isAdmin = hasRole(jwt, "ADMIN");

            investmentService.deleteInvestmentService(id, email, isAdmin);
            return ResponseEntity.ok(Map.of("message", "Service supprimé avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE - Service rejeté
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
            investmentService.deleteRejectedService(id, email);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Service rejeté supprimé avec succès"
            ));
        } catch (Exception e) {
            log.error("❌ Erreur suppression service rejeté: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // RECHERCHE
    @GetMapping("/search")
    public ResponseEntity<List<InvestmentService>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(investmentService.searchInvestmentServices(keyword));
    }

    @GetMapping("/advanced-search")
    public ResponseEntity<List<InvestmentService>> advancedSearch(
            @RequestParam(required = false) Long regionId,
            @RequestParam(required = false) Long sectorId,
            @RequestParam(required = false) ServiceStatus status,
            @RequestParam(required = false) BigDecimal maxAmount) {

        return ResponseEntity.ok(investmentService.advancedSearch(regionId, sectorId, status, maxAmount));
    }

    // STATISTIQUES
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        return ResponseEntity.ok(investmentService.getStatistics());
    }

    // TEST
    @GetMapping("/test-notify-international/{serviceId}")
    public ResponseEntity<?> testNotifyInternational(@PathVariable Long serviceId) {
        try {
            InvestmentService service = investmentService.getInvestmentServiceById(serviceId);
            log.info("🔴 TEST - Appel direct de notifyInternationalCompaniesNewInvestmentService");
            notificationService.notifyInternationalCompaniesNewInvestmentService(service);
            return ResponseEntity.ok(Map.of("message", "Test envoyé"));
        } catch (Exception e) {
            log.error("🔴 TEST - Erreur: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Méthode utilitaire pour vérifier les rôles
    private boolean hasRole(Jwt jwt, String role) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles != null && roles.contains(role);
        }
        return false;
    }
    // ========================================
// DELETE DOCUMENT - Supprimer un document spécifique
// ========================================
    // ========================================
// DELETE DOCUMENT - Supprimer un document spécifique (CORRIGÉ)
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
            InvestmentServiceDocument document = investmentService.getDocumentById(documentId);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }

            // Récupérer le service associé
            InvestmentService service = document.getInvestmentService();

            // Vérifier que le partenaire est bien le propriétaire du service
            if (!service.getProvider().getEmail().equals(email)) {
                return ResponseEntity.status(403).body(Map.of("error",
                        "Vous ne pouvez supprimer que les documents de vos propres services"));
            }

            // ✅ Vérifier l'autorisation de modification pour les services approuvés
            if (service.getStatus() == ServiceStatus.APPROVED) {
                if (!service.isEditAuthorized()) {
                    return ResponseEntity.status(403).body(Map.of("error",
                            "Vous n'avez pas l'autorisation de modifier ce service"));
                }
                log.info("✅ Suppression de document autorisée pour service approuvé");
            }

            // Supprimer le fichier physique
            fileStorageService.deleteFile(document.getFilePath(), FileStorageService.ServiceType.INVESTMENT);

            // Supprimer de la base de données
            investmentService.deleteDocument(documentId);

            //  LIGNES SUPPRIMÉES - NE PAS RÉVOQUER L'AUTORISATION ICI
            // L'autorisation sera révoquée dans updateInvestmentService quand le partenaire soumettra ses modifications
            // if (service.getStatus() == ServiceStatus.APPROVED && service.isEditAuthorized()) {
            //     investmentService.revokeEditAuthorization(service.getId());
            //     log.info("🔓 Autorisation de modification révoquée après suppression de document");
            // }

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
    private boolean isServiceOwner(InvestmentService service, String email) {
        return service.getProvider() != null && service.getProvider().getEmail().equals(email);
    }
}