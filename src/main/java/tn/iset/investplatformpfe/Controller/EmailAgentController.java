package tn.iset.investplatformpfe.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.iset.investplatformpfe.Dto.ApprovalRequest;
import tn.iset.investplatformpfe.Dto.SuggestionResponse;
import tn.iset.investplatformpfe.Entity.EmailSendRecord;
import tn.iset.investplatformpfe.Entity.Prospect;
import tn.iset.investplatformpfe.Repository.EmailSendRecordRepository;
import tn.iset.investplatformpfe.Repository.ProspectRepository;
import tn.iset.investplatformpfe.Service.CsvService;
import tn.iset.investplatformpfe.Service.EmailAgentService;
import tn.iset.investplatformpfe.Dto.CustomMessageRequest;
import tn.iset.investplatformpfe.Service.CustomEmailAgentService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/email-agent")
public class EmailAgentController {

    private final CsvService csvService;
    private final ProspectRepository repo;
    private final EmailAgentService agentService;
    private final CustomEmailAgentService customEmailAgentService;
    private final EmailSendRecordRepository recordRepo;

    public EmailAgentController(CsvService csvService,
                                ProspectRepository repo,
                                EmailAgentService agentService,
                                CustomEmailAgentService customEmailAgentService,
                                EmailSendRecordRepository recordRepo) {
        this.csvService = csvService;
        this.repo = repo;
        this.agentService = agentService;
        this.customEmailAgentService = customEmailAgentService;
        this.recordRepo = recordRepo;
    }

    private boolean isAdmin(Jwt jwt) {
        if (jwt == null) return false;
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) return false;
        List<String> roles = (List<String>) realmAccess.get("roles");
        return roles != null && roles.contains("ADMIN");
    }

    // ── Import CSV ────────────────────────────────────────────────────────────
    @PostMapping("/import")
    public ResponseEntity<?> importCsv(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam("file") MultipartFile file) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        try {
            List<Prospect> list = csvService.parse(file);
            repo.saveAll(list);
            return ResponseEntity.ok("Import success");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Import error: " + e.getMessage());
        }
    }

    // ── /run — délègue à generate() ──────────────────────────────────────────
    @PostMapping("/run")
    public ResponseEntity<?> run(@AuthenticationPrincipal Jwt jwt) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        String result = agentService.generate();
        return ResponseEntity.ok(result);
    }

    // ── PHASE 1 : Générer les messages ────────────────────────────────────────
    @PostMapping("/generate")
    public ResponseEntity<?> generate(@AuthenticationPrincipal Jwt jwt) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        String result = agentService.generate();
        return ResponseEntity.ok(result);
    }

    // ── Récupérer les PENDING_REVIEW ──────────────────────────────────────────
    @GetMapping("/pending-review")
    public ResponseEntity<?> getPendingReview(@AuthenticationPrincipal Jwt jwt) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        return ResponseEntity.ok(agentService.getPendingReview());
    }

    // ── PHASE 2A : Approuver un message ──────────────────────────────────────
    @PostMapping("/approve")
    public ResponseEntity<?> approve(@AuthenticationPrincipal Jwt jwt,
                                     @RequestBody ApprovalRequest req) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        String result = agentService.approveAndSend(req.getProspectId(), req.getFinalMessage());
        return ResponseEntity.ok(result);
    }

    // ── PHASE 2B : Refuser un message ────────────────────────────────────────
    @PostMapping("/reject/{id}")
    public ResponseEntity<?> reject(@AuthenticationPrincipal Jwt jwt,
                                    @PathVariable Long id) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        String result = agentService.reject(id);
        return ResponseEntity.ok(result);
    }

    // ── PHASE 2C : Tout approuver ────────────────────────────────────────────
    @PostMapping("/approve-all")
    public ResponseEntity<?> approveAll(@AuthenticationPrincipal Jwt jwt) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        String result = agentService.approveAll();
        return ResponseEntity.ok(result);
    }

    // ── Liste tous les prospects ──────────────────────────────────────────────
    @GetMapping("/all")
    public List<Prospect> getAll(@AuthenticationPrincipal Jwt jwt) {
        if (!isAdmin(jwt)) return List.of();
        return repo.findAll();
    }

    // ── Vider la liste ────────────────────────────────────────────────────────
    @DeleteMapping("/clear")
    public ResponseEntity<?> clearAll(@AuthenticationPrincipal Jwt jwt) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        repo.deleteAll();
        return ResponseEntity.ok("Liste supprimée");
    }

    // ── Message personnalisé : envoyer ───────────────────────────────────────
    @PostMapping("/send-custom")
    public ResponseEntity<?> sendCustom(@AuthenticationPrincipal Jwt jwt,
                                        @RequestBody CustomMessageRequest request) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        String result = customEmailAgentService.run(request);
        return ResponseEntity.ok(result);
    }

    // ── Message personnalisé : preview ───────────────────────────────────────
    @PostMapping("/custom/preview")
    public ResponseEntity<SuggestionResponse> preview(@RequestBody CustomMessageRequest request) {
        SuggestionResponse response = customEmailAgentService.preview(request);
        return ResponseEntity.ok(response);
    }

    // ── Message personnalisé : send ───────────────────────────────────────────
    @PostMapping("/custom/send")
    public ResponseEntity<String> send(@RequestBody CustomMessageRequest request) {
        String result = customEmailAgentService.send(request);
        return ResponseEntity.ok(result);
    }

    // ── Annulation agent IA ───────────────────────────────────────────────────
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelAiRun(@AuthenticationPrincipal Jwt jwt) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        agentService.cancelRun();
        return ResponseEntity.ok("🛑 Annulation demandée pour l'agent AI.");
    }

    // ── Annulation envoi personnalisé ─────────────────────────────────────────
    @PostMapping("/custom/cancel")
    public ResponseEntity<?> cancelCustomSend(@AuthenticationPrincipal Jwt jwt) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        customEmailAgentService.cancelSend();
        return ResponseEntity.ok("🛑 Annulation demandée pour l'envoi personnalisé.");
    }

    // ── Remettre tous les SENT/FAILED en PENDING ──────────────────────────────
    @PostMapping("/reset-pending")
    public ResponseEntity<?> resetToPending(@AuthenticationPrincipal Jwt jwt) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        List<Prospect> toReset = repo.findAll().stream()
                .filter(p -> "SENT".equals(p.getStatus()) || "FAILED".equals(p.getStatus()))
                .collect(java.util.stream.Collectors.toList());
        toReset.forEach(p -> {
            p.setStatus("PENDING");
            p.setGeneratedMessage(null);
            p.setGeneratedSuggestions(null);
            p.setSentAt(null);
        });
        repo.saveAll(toReset);
        return ResponseEntity.ok("🔄 " + toReset.size() + " prospect(s) remis en PENDING.");
    }

    // ── BROADCAST : Générer suggestions pour message admin ───────────────────
    @PostMapping("/broadcast/preview")
    public ResponseEntity<?> broadcastPreview(@AuthenticationPrincipal Jwt jwt,
                                              @RequestBody Map<String, String> body) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        String rawMessage = body.get("rawMessage");
        String error = agentService.validateBroadcastMessage(rawMessage);
        if (error != null) {
            return ResponseEntity.ok(Map.of("valid", false, "errorMessage", error, "suggestions", List.of()));
        }
        try {
            List<String> suggestions = agentService.generateBroadcastSuggestions(rawMessage);
            return ResponseEntity.ok(Map.of("valid", true, "suggestions", suggestions));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("valid", false, "errorMessage", e.getMessage(), "suggestions", List.of()));
        }
    }

    // ── BROADCAST : Envoyer le message validé à tous les PENDING ─────────────
    @PostMapping("/broadcast/send")
    public ResponseEntity<?> broadcastSend(@AuthenticationPrincipal Jwt jwt,
                                           @RequestBody Map<String, String> body) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        String finalMessage = body.get("finalMessage");
        if (finalMessage == null || finalMessage.isBlank())
            return ResponseEntity.badRequest().body("❌ Message vide.");
        String result = agentService.broadcastToAllPending(finalMessage);
        return ResponseEntity.ok(result);
    }

    // Récupérer l'historique d'un prospect
    @GetMapping("/history/{prospectId}")
    public ResponseEntity<?> getHistoryByProspect(@AuthenticationPrincipal Jwt jwt,
                                                  @PathVariable Long prospectId) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        return ResponseEntity.ok(
                recordRepo.findByProspectIdOrderBySentAtDesc(prospectId));
    }


    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@AuthenticationPrincipal Jwt jwt) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");

        // Lundi de la semaine en cours à 00:00:00
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDateTime weekStart = monday.atStartOfDay();

        // Dimanche à 23:59:59
        LocalDateTime weekEnd = monday.plusDays(6).atTime(23, 59, 59);

        return ResponseEntity.ok(
                recordRepo.findBySentAtBetweenOrderBySentAtDesc(weekStart, weekEnd)
        );
    }
}
