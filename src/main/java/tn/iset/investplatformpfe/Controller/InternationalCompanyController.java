package tn.iset.investplatformpfe.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.CollaborationService;
import tn.iset.investplatformpfe.Entity.InvestmentService;
import tn.iset.investplatformpfe.Service.InternationalCompanyService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/international-companies")
public class InternationalCompanyController {

    private final InternationalCompanyService companyService;

    public InternationalCompanyController(InternationalCompanyService companyService) {
        this.companyService = companyService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Consultation des services de collaboration approuvés
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/services/collaboration")
    public ResponseEntity<?> getApprovedCollaborationServices(@AuthenticationPrincipal Jwt jwt) {
        if (!isInternationalCompany(jwt)) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux sociétés internationales"));
        }
        try {
            List<CollaborationService> services = companyService.getApprovedCollaborationServices();
            return ResponseEntity.ok(services);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/services/collaboration/{id}")
    public ResponseEntity<?> getCollaborationServiceById(@AuthenticationPrincipal Jwt jwt,
                                                         @PathVariable Long id) {
        if (!isInternationalCompany(jwt)) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux sociétés internationales"));
        }
        try {
            CollaborationService service = companyService.getApprovedCollaborationServiceById(id);
            return ResponseEntity.ok(service);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Consultation des services d'investissement approuvés
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/services/investment")
    public ResponseEntity<?> getApprovedInvestmentServices(@AuthenticationPrincipal Jwt jwt) {
        if (!isInternationalCompany(jwt)) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux sociétés internationales"));
        }
        try {
            List<InvestmentService> services = companyService.getApprovedInvestmentServices();
            return ResponseEntity.ok(services);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/services/investment/{id}")
    public ResponseEntity<?> getInvestmentServiceById(@AuthenticationPrincipal Jwt jwt,
                                                      @PathVariable Long id) {
        if (!isInternationalCompany(jwt)) {
            return ResponseEntity.status(403).body(Map.of("error", "Accès réservé aux sociétés internationales"));
        }
        try {
            InvestmentService service = companyService.getApprovedInvestmentServiceById(id);
            return ResponseEntity.ok(service);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Méthode utilitaire pour vérifier le rôle
    // ─────────────────────────────────────────────────────────────────────────

    private boolean isInternationalCompany(Jwt jwt) {
        if (jwt == null) return false;
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            return roles != null && roles.contains("INTERNATIONAL_COMPANY");
        }
        return false;
    }
}