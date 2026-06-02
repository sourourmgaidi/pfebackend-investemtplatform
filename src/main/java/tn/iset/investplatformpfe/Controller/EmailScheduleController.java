package tn.iset.investplatformpfe.Controller;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Dto.ScheduleRequest;
import tn.iset.investplatformpfe.Entity.EmailSchedule;
import tn.iset.investplatformpfe.Service.EmailSchedulerService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/email-schedule")
public class EmailScheduleController {

    private final EmailSchedulerService schedulerService;

    public EmailScheduleController(EmailSchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    private boolean isAdmin(Jwt jwt) {
        if (jwt == null) return false;
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return false;
        List<String> roles = (List<String>) realmAccess.get("roles");
        return roles != null && roles.contains("ADMIN");
    }

    // Planifier un nouvel envoi
    @PostMapping
    public ResponseEntity<?> schedule(@AuthenticationPrincipal Jwt jwt,
                                      @RequestBody ScheduleRequest req) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        EmailSchedule s = new EmailSchedule();
        s.setType(req.getType());
        s.setMessage(req.getMessage());
        s.setSubject(req.getSubject());
        s.setScheduledAt(req.getScheduledAt());
        s.setStatus("PENDING");
        return ResponseEntity.ok(schedulerService.create(s));
    }

    // Lister toutes les planifications
    @GetMapping
    public ResponseEntity<?> getAll(@AuthenticationPrincipal Jwt jwt) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        return ResponseEntity.ok(schedulerService.getAll());
    }

    // Annuler
    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@AuthenticationPrincipal Jwt jwt,
                                    @PathVariable Long id) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        schedulerService.cancel(id);
        return ResponseEntity.ok("Planification annulée.");
    }

    // Supprimer
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal Jwt jwt,
                                    @PathVariable Long id) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        schedulerService.delete(id);
        return ResponseEntity.ok("Supprimé.");
    }
}