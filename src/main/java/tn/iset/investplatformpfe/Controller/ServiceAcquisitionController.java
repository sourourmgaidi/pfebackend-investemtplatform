package tn.iset.investplatformpfe.Controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.LocalPartner;
import tn.iset.investplatformpfe.Entity.Role;
import tn.iset.investplatformpfe.Repository.InvestorRepository;
import tn.iset.investplatformpfe.Repository.InternationalCompanyRepository;
import tn.iset.investplatformpfe.Repository.LocalPartnerRepository;
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

    public ServiceAcquisitionController(
            ServiceAcquisitionService acquisitionService,
            LocalPartnerRepository localPartnerRepository,
            InvestorRepository investorRepository,
            InternationalCompanyRepository internationalCompanyRepository) {
        this.acquisitionService = acquisitionService;
        this.localPartnerRepository = localPartnerRepository;
        this.investorRepository = investorRepository;
        this.internationalCompanyRepository = internationalCompanyRepository;
    }

    // ✅ Helper — extraire le LocalPartner depuis le JWT
    private LocalPartner getAuthenticatedPartner(Jwt jwt) {
        if (jwt == null) throw new RuntimeException("Not authenticated");
        String email = jwt.getClaimAsString("email");
        return localPartnerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Local partner not found for email: " + email));
    }

    // ✅ Helper — extraire le rôle depuis le JWT
    private String extractRoleFromJwt(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles != null) {
                for (String role : roles) {
                    if (role.equals("INVESTOR") || role.equals("INTERNATIONAL_COMPANY")) {
                        return role;
                    }
                }
            }
        }
        throw new RuntimeException("No valid role found");
    }

    // ✅ Helper — récupérer l'ID depuis l'email et le rôle
    private Long getUserIdByEmailAndRole(String email, String role) {
        if ("INVESTOR".equals(role)) {
            return investorRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Investor not found")).getId();
        } else if ("INTERNATIONAL_COMPANY".equals(role)) {
            return internationalCompanyRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("International company not found")).getId();
        }
        throw new RuntimeException("Unknown role");
    }

    // ─────────────────────────────────────────
    // ✅ Investor/InternationalCompany envoie une demande — JWT obligatoire
    // ─────────────────────────────────────────
    @PostMapping("/initiate")
    public ResponseEntity<?> initiate(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, Object> body) {
        try {
            // 1. Vérifier JWT
            if (jwt == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            log.info("📥 Body reçu: {}", body);

            // 2. Extraire les infos du JWT (sécurisé)
            String emailFromToken = jwt.getClaimAsString("email");
            String roleFromToken = extractRoleFromJwt(jwt);
            Long idFromToken = getUserIdByEmailAndRole(emailFromToken, roleFromToken);

            // 3. Valider les champs (sans acquirerId, email, role)
            if (body.get("serviceType") == null)
                return ResponseEntity.badRequest().body(Map.of("error", "serviceType is required"));
            if (body.get("serviceId") == null)
                return ResponseEntity.badRequest().body(Map.of("error", "serviceId is required"));
            if (body.get("amount") == null)
                return ResponseEntity.badRequest().body(Map.of("error", "amount is required"));

            String serviceType = (String) body.get("serviceType");
            Long serviceId = Long.valueOf(body.get("serviceId").toString());
            double amount = Double.parseDouble(body.get("amount").toString());

            // 4. Vérifier le rôle
            Role role = Role.valueOf(roleFromToken);
            if (role != Role.INVESTOR && role != Role.INTERNATIONAL_COMPANY) {
                return ResponseEntity.status(403).body(
                        Map.of("error", "Only INVESTOR and INTERNATIONAL_COMPANY roles can acquire services."));
            }

            // 5. Appeler le service AVEC les valeurs du JWT (pas celles du body)
            Map<String, Object> result = acquisitionService.initiateAcquisition(
                    serviceType, serviceId,
                    idFromToken,      // ← FORCÉ depuis JWT
                    emailFromToken,   // ← FORCÉ depuis JWT
                    role,             // ← FORCÉ depuis JWT
                    amount);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Erreur initiation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // ✅ Local Partner voit ses demandes en attente — JWT obligatoire
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
    // ✅ Local Partner ACCEPTE — JWT obligatoire
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
    // ✅ Local Partner REFUSE — JWT obligatoire
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
    // Confirmer paiement après Flouci
    // ─────────────────────────────────────────
    @GetMapping("/confirm")
    public ResponseEntity<?> confirm(
            @RequestParam(required = false) String paymentId,
            @RequestParam(required = false) String orderId) {
        try {
            return ResponseEntity.ok(acquisitionService.confirmPayment(paymentId, orderId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // Annuler
    // ─────────────────────────────────────────
    @GetMapping("/cancel")
    public ResponseEntity<?> cancel(
            @RequestParam(required = false) String paymentId,
            @RequestParam(required = false) String orderId) {
        acquisitionService.cancelPayment(paymentId, orderId);
        return ResponseEntity.ok(Map.of("message", "Payment cancelled"));
    }

    // ─────────────────────────────────────────
    // ✅ Services acquis par l'utilisateur connecté — JWT obligatoire
    // ─────────────────────────────────────────
    @GetMapping("/my-services")
    public ResponseEntity<?> myServices(@AuthenticationPrincipal Jwt jwt) {
        try {
            if (jwt == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            String email = jwt.getClaimAsString("email");
            String roleFromToken = extractRoleFromJwt(jwt);
            Long userId = getUserIdByEmailAndRole(email, roleFromToken);

            log.info("📋 /my-services - userId={}, role={}", userId, roleFromToken);
            Role roleEnum = Role.valueOf(roleFromToken);
            var acquisitions = acquisitionService.getUserAcquisitions(userId, roleEnum);
            log.info("✅ {} acquisitions retournées", acquisitions.size());
            return ResponseEntity.ok(acquisitions);

        } catch (Exception e) {
            log.error("❌ Erreur /my-services: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // Vérifier si un service est déjà pris
    // ─────────────────────────────────────────
    @GetMapping("/check")
    public ResponseEntity<?> check(
            @RequestParam Long serviceId,
            @RequestParam String serviceType) {
        return ResponseEntity.ok(Map.of("taken",
                acquisitionService.isServiceTaken(serviceId, serviceType)));
    }

    /**
     * GET /api/acquisitions/access/user?serviceId=1&serviceType=INVESTMENT&userId=5
     * Vérifier si un user a accès à un service TAKEN
     */
    @GetMapping("/access/user")
    public ResponseEntity<?> checkUserAccess(
            @RequestParam Long serviceId,
            @RequestParam String serviceType,
            @RequestParam Long userId) {
        return ResponseEntity.ok(Map.of("hasAccess",
                acquisitionService.userHasAccess(serviceId, serviceType, userId)));
    }

    /**
     * GET /api/acquisitions/access/partner?serviceId=1&serviceType=INVESTMENT&partnerId=2
     * Vérifier si un partner a accès à un service TAKEN (son propre service)
     */
    @GetMapping("/access/partner")
    public ResponseEntity<?> checkPartnerAccess(
            @RequestParam Long serviceId,
            @RequestParam String serviceType,
            @RequestParam Long partnerId) {
        return ResponseEntity.ok(Map.of("hasAccess",
                acquisitionService.partnerHasAccess(serviceId, serviceType, partnerId)));
    }

    // ─────────────────────────────────────────
    // ✅ User annule sa demande — JWT obligatoire
    // ─────────────────────────────────────────
    @PostMapping("/cancel-request/{acquisitionId}")
    public ResponseEntity<?> cancelRequest(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long acquisitionId,
            @RequestBody Map<String, String> body) {
        try {
            if (jwt == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            String email = jwt.getClaimAsString("email");
            String roleFromToken = extractRoleFromJwt(jwt);
            Long userId = getUserIdByEmailAndRole(email, roleFromToken);

            String reason = body.get("reason");
            if (reason == null || reason.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Reason is required"));
            }

            // userId est forcé depuis le JWT, pas depuis le body !
            Map<String, Object> result = acquisitionService.cancelUserRequest(
                    acquisitionId, userId, reason);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("❌ Erreur annulation demande: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // ✅ Toutes les acquisitions de l'utilisateur connecté — JWT obligatoire
    // ─────────────────────────────────────────
    @GetMapping("/my-all")
    public ResponseEntity<?> myAllAcquisitions(@AuthenticationPrincipal Jwt jwt) {
        try {
            if (jwt == null) {
                return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
            }

            String email = jwt.getClaimAsString("email");
            String roleFromToken = extractRoleFromJwt(jwt);
            Long userId = getUserIdByEmailAndRole(email, roleFromToken);

            return ResponseEntity.ok(
                    acquisitionService.getAllUserAcquisitions(userId, Role.valueOf(roleFromToken)));

        } catch (Exception e) {
            log.error("❌ Erreur récupération acquisitions: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────
    // ⚠️ DELETE - Gardé tel quel (admin seulement)
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
}