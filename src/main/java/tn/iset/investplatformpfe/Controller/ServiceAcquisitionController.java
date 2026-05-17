package tn.iset.investplatformpfe.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.LocalPartner;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Repository.*;
import tn.iset.investplatformpfe.Service.ServiceAcquisitionService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/acquisitions")
@CrossOrigin(origins = "*")
public class ServiceAcquisitionController {

    private static final Logger log = LoggerFactory.getLogger(ServiceAcquisitionController.class);

    private final ServiceAcquisitionService acquisitionService;
    private final LocalPartnerRepository localPartnerRepository;
    private final InvestorRepository investorRepository;
    private final InternationalCompanyRepository internationalCompanyRepository;
    private final EconomicPartnerRepository economicPartnerRepository;
    private final TouristRepository touristRepository;


    public ServiceAcquisitionController(
            ServiceAcquisitionService acquisitionService,
            LocalPartnerRepository localPartnerRepository,
            InvestorRepository investorRepository,
            InternationalCompanyRepository internationalCompanyRepository,
            EconomicPartnerRepository economicPartnerRepository,
            TouristRepository touristRepository) {
        this.acquisitionService = acquisitionService;
        this.localPartnerRepository = localPartnerRepository;
        this.investorRepository = investorRepository;
        this.internationalCompanyRepository = internationalCompanyRepository;
        this.economicPartnerRepository = economicPartnerRepository;
        this.touristRepository = touristRepository;

    }

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────
    private LocalPartner getAuthenticatedPartner(Jwt jwt) {
        if (jwt == null) throw new RuntimeException("Not authenticated");
        String email = jwt.getClaimAsString("email");
        return localPartnerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Local partner not found for email: " + email));
    }

    private String extractRoleFromJwt(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles != null) {
                for (String role : roles) {
                    if (role.equals("INVESTOR")
                            || role.equals("INTERNATIONAL_COMPANY")
                            || role.equals("PARTNER")
                            || role.equals("TOURIST")) {  // ← ajout
                        return role;
                    }
                }
            }
        }
        throw new RuntimeException("No valid role found");
    }

    private Long getUserIdByEmailAndRole(String email, String role) {
        if ("INVESTOR".equals(role)) {
            return investorRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Investor not found")).getId();
        } else if ("INTERNATIONAL_COMPANY".equals(role)) {
            return internationalCompanyRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("International company not found")).getId();
        } else if ("PARTNER".equals(role)) {
            return economicPartnerRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Economic partner not found")).getId();
        } else if ("TOURIST".equals(role)) {  // ← ajout
            return touristRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Tourist not found")).getId();
        }
        throw new RuntimeException("Unknown role");
    }

    // ─────────────────────────────────────────
    // ÉTAPE 1 — User envoie une demande
    // ─────────────────────────────────────────
    @PostMapping("/initiate")
    public ResponseEntity<?> initiate(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, Object> body) {
        try {
            if (jwt == null)
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

            log.info("📥 Body reçu: {}", body);

            String emailFromToken = jwt.getClaimAsString("email");
            String roleFromToken  = extractRoleFromJwt(jwt);
            Long   idFromToken    = getUserIdByEmailAndRole(emailFromToken, roleFromToken);

            if (body.get("serviceType") == null)
                return ResponseEntity.badRequest().body(Map.of("error", "serviceType is required"));
            if (body.get("serviceId") == null)
                return ResponseEntity.badRequest().body(Map.of("error", "serviceId is required"));
            if (body.get("amount") == null)
                return ResponseEntity.badRequest().body(Map.of("error", "amount is required"));

            String serviceType = (String) body.get("serviceType");
            Long   serviceId   = Long.valueOf(body.get("serviceId").toString());
            double amount      = Double.parseDouble(body.get("amount").toString());

            Role role = Role.valueOf(roleFromToken);
            if (role != Role.INVESTOR
                    && role != Role.INTERNATIONAL_COMPANY
                    && role != Role.PARTNER) {
                return ResponseEntity.status(403).body(Map.of(
                        "error", "Only INVESTOR, INTERNATIONAL_COMPANY and ECONOMIC_PARTNER roles can acquire services."));
            }

            Map<String, Object> result = acquisitionService.initiateAcquisition(
                    serviceType, serviceId, idFromToken, emailFromToken, role, amount);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Erreur initiation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // ÉTAPE 2A — Local Partner voit les demandes en attente
    // ─────────────────────────────────────────
    @GetMapping("/partner/pending")
    public ResponseEntity<?> partnerPending(@AuthenticationPrincipal Jwt jwt) {
        try {
            LocalPartner partner = getAuthenticatedPartner(jwt);
            return ResponseEntity.ok(
                    acquisitionService.getPendingRequestsForPartner(partner.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // ÉTAPE 2A — Local Partner APPROUVE la demande
    // → Service passe en RESERVED, user notifié pour payer
    // ─────────────────────────────────────────
    @PostMapping("/partner/approve/{acquisitionId}")
    public ResponseEntity<?> approve(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long acquisitionId) {
        try {
            LocalPartner partner = getAuthenticatedPartner(jwt);
            return ResponseEntity.ok(
                    acquisitionService.partnerApprove(acquisitionId, partner.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // ÉTAPE 2B — Local Partner REFUSE la demande
    // ─────────────────────────────────────────
    @PostMapping("/partner/reject/{acquisitionId}")
    public ResponseEntity<?> reject(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long acquisitionId,
            @RequestBody Map<String, String> body) {
        try {
            LocalPartner partner = getAuthenticatedPartner(jwt);
            String reason = body.get("reason");
            if (reason == null || reason.isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Reason is required"));
            return ResponseEntity.ok(
                    acquisitionService.partnerReject(acquisitionId, partner.getId(), reason));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // ÉTAPE 3 — Local Partner voit les demandes en attente de validation
    //           (paiement effectué hors ligne, partner doit confirmer)
    // ─────────────────────────────────────────
    @GetMapping("/partner/awaiting-validation")
    public ResponseEntity<?> partnerAwaitingValidation(@AuthenticationPrincipal Jwt jwt) {
        try {
            LocalPartner partner = getAuthenticatedPartner(jwt);
            return ResponseEntity.ok(
                    acquisitionService.getAwaitingValidationForPartner(partner.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // ÉTAPE 3 — Local Partner VALIDE (confirme réception du paiement)
    //           → Service passe en TAKEN, acquisition COMPLETED
    // ─────────────────────────────────────────
    @PostMapping("/partner/validate/{acquisitionId}")
    public ResponseEntity<?> validate(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long acquisitionId) {
        try {
            LocalPartner partner = getAuthenticatedPartner(jwt);
            return ResponseEntity.ok(
                    acquisitionService.partnerValidate(acquisitionId, partner.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // Services acquis (COMPLETED) par l'utilisateur connecté
    // ─────────────────────────────────────────
    @GetMapping("/my-services")
    public ResponseEntity<?> myServices(@AuthenticationPrincipal Jwt jwt) {
        try {
            if (jwt == null)
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

            String email         = jwt.getClaimAsString("email");
            String roleFromToken = extractRoleFromJwt(jwt);
            Long   userId        = getUserIdByEmailAndRole(email, roleFromToken);

            log.info("📋 /my-services - userId={}, role={}", userId, roleFromToken);
            Role roleEnum    = Role.valueOf(roleFromToken);
            var  acquisitions = acquisitionService.getUserAcquisitions(userId, roleEnum);
            log.info("✅ {} acquisitions retournées", acquisitions.size());
            return ResponseEntity.ok(acquisitions);

        } catch (Exception e) {
            log.error("❌ Erreur /my-services: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // Toutes les acquisitions (tous statuts) de l'utilisateur connecté
    // ─────────────────────────────────────────
    @GetMapping("/my-all")
    public ResponseEntity<?> myAllAcquisitions(@AuthenticationPrincipal Jwt jwt) {
        try {
            if (jwt == null)
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

            String email         = jwt.getClaimAsString("email");
            String roleFromToken = extractRoleFromJwt(jwt);
            Long   userId        = getUserIdByEmailAndRole(email, roleFromToken);

            return ResponseEntity.ok(
                    acquisitionService.getAllUserAcquisitions(userId, Role.valueOf(roleFromToken)));

        } catch (Exception e) {
            log.error("❌ Erreur récupération acquisitions: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // User annule sa propre demande
    // ─────────────────────────────────────────
    @PostMapping("/cancel-request/{acquisitionId}")
    public ResponseEntity<?> cancelRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long acquisitionId,
            @RequestBody Map<String, String> body) {
        try {
            if (jwt == null)
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

            String email         = jwt.getClaimAsString("email");
            String roleFromToken = extractRoleFromJwt(jwt);
            Long   userId        = getUserIdByEmailAndRole(email, roleFromToken);

            String reason = body.get("reason");
            if (reason == null || reason.isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Reason is required"));

            return ResponseEntity.ok(
                    acquisitionService.cancelUserRequest(acquisitionId, userId, reason));

        } catch (Exception e) {
            log.error("❌ Erreur annulation demande: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // Vérifications d'accès / statut
    // ─────────────────────────────────────────
    @GetMapping("/check")
    public ResponseEntity<?> check(
            @RequestParam Long serviceId,
            @RequestParam String serviceType) {
        return ResponseEntity.ok(Map.of("taken",
                acquisitionService.isServiceTaken(serviceId, serviceType)));
    }

    @GetMapping("/access/user")
    public ResponseEntity<?> checkUserAccess(
            @RequestParam Long serviceId,
            @RequestParam String serviceType,
            @RequestParam Long userId) {
        return ResponseEntity.ok(Map.of("hasAccess",
                acquisitionService.userHasAccess(serviceId, serviceType, userId)));
    }

    @GetMapping("/access/partner")
    public ResponseEntity<?> checkPartnerAccess(
            @RequestParam Long serviceId,
            @RequestParam String serviceType,
            @RequestParam Long partnerId) {
        return ResponseEntity.ok(Map.of("hasAccess",
                acquisitionService.partnerHasAccess(serviceId, serviceType, partnerId)));
    }

    // ─────────────────────────────────────────
    // Admin — suppression
    // ─────────────────────────────────────────
    @DeleteMapping("/{acquisitionId}")
    public ResponseEntity<?> deleteAcquisition(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long acquisitionId) {
        try {
            acquisitionService.deleteAcquisition(acquisitionId);
            return ResponseEntity.ok(Map.of("message", "Acquisition deleted."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @PostMapping("/tourist/initiate")
    public ResponseEntity<?> touristInitiate(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, Object> body) {
        try {
            if (jwt == null)
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

            String email = jwt.getClaimAsString("email");

            // Vérifier que c'est bien un TOURIST
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles == null || !roles.contains("TOURIST"))
                return ResponseEntity.status(403).body(Map.of("error", "Only tourists can access this endpoint"));

            Long touristId = touristRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Tourist not found")).getId();

            if (body.get("serviceId") == null)
                return ResponseEntity.badRequest().body(Map.of("error", "serviceId is required"));
            if (body.get("amount") == null)
                return ResponseEntity.badRequest().body(Map.of("error", "amount is required"));

            Long serviceId = Long.valueOf(body.get("serviceId").toString());
            double amount  = Double.parseDouble(body.get("amount").toString());

            Map<String, Object> result = acquisitionService.initiateAcquisition(
                    "TOURIST", serviceId, touristId, email, Role.TOURIST, amount);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Erreur initiation touriste: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    // Services acquis (COMPLETED) par le touriste connecté
    @GetMapping("/tourist/my-services")
    public ResponseEntity<?> touristMyServices(@AuthenticationPrincipal Jwt jwt) {
        try {
            if (jwt == null)
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

            String email = jwt.getClaimAsString("email");
            Long touristId = touristRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Tourist not found")).getId();

            return ResponseEntity.ok(
                    acquisitionService.getUserAcquisitions(touristId, Role.TOURIST));

        } catch (Exception e) {
            log.error("❌ Erreur tourist/my-services: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Toutes les acquisitions (tous statuts) du touriste connecté
    @GetMapping("/tourist/my-all")
    public ResponseEntity<?> touristMyAll(@AuthenticationPrincipal Jwt jwt) {
        try {
            if (jwt == null)
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));

            String email = jwt.getClaimAsString("email");
            Long touristId = touristRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Tourist not found")).getId();

            return ResponseEntity.ok(
                    acquisitionService.getAllUserAcquisitions(touristId, Role.TOURIST));

        } catch (Exception e) {
            log.error("❌ Erreur tourist/my-all: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @PostMapping("/partner/reject-validation/{acquisitionId}")
    public ResponseEntity<?> rejectValidation(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long acquisitionId,
            @RequestBody Map<String, String> body) {
        try {
            LocalPartner partner = getAuthenticatedPartner(jwt);
            String reason = body.get("reason");
            if (reason == null || reason.isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Reason is required"));
            acquisitionService.partnerRejectAndDelete(acquisitionId, partner.getId(), reason);
            return ResponseEntity.ok(Map.of("message", "Acquisition deleted."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/partner/taken-services")
    public ResponseEntity<?> partnerTakenServices(@AuthenticationPrincipal Jwt jwt) {
        try {
            LocalPartner partner = getAuthenticatedPartner(jwt);
            return ResponseEntity.ok(
                    acquisitionService.getTakenServicesForPartner(partner.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
}