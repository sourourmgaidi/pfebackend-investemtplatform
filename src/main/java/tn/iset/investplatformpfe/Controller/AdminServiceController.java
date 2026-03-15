package tn.iset.investplatformpfe.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Repository.*;
import tn.iset.investplatformpfe.Service.NotificationService;

import java.util.*;

@RestController
@RequestMapping("/api/admin/services")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminServiceController {

    @Autowired private CollaborationServiceRepository collaborationRepo;
    @Autowired private InvestmentServiceRepository investmentRepo;
    @Autowired private TouristServiceRepository touristRepo;
    @Autowired private NotificationService notificationService;

    // ── Tous les services PENDING ──────────────────────────────
    @GetMapping("/pending")
    public ResponseEntity<?> getAllPending() {
        Map<String, Object> result = new HashMap<>();
        result.put("collaboration", collaborationRepo.findByStatus(ServiceStatus.PENDING));
        result.put("investment", investmentRepo.findByStatus(ServiceStatus.PENDING));
        result.put("tourist", touristRepo.findByStatus(ServiceStatus.PENDING));
        return ResponseEntity.ok(result);
    }

    // ── APPROVE ───────────────────────────────────────────────
    @PutMapping("/collaboration/{id}/approve")
    public ResponseEntity<?> approveCollaboration(@PathVariable Long id) {
        CollaborationService svc = collaborationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        svc.setStatus(ServiceStatus.APPROVED);
        collaborationRepo.save(svc);
        notificationService.notifyLocalPartnerServiceApproved(svc);
        notificationService.notifyPartnersNewCollaborationService(svc);
        return ResponseEntity.ok(Map.of("message", "Collaboration service approved"));
    }

    @PutMapping("/investment/{id}/approve")
    public ResponseEntity<?> approveInvestment(@PathVariable Long id) {
        InvestmentService svc = investmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        svc.setStatus(ServiceStatus.APPROVED);
        investmentRepo.save(svc);
        notificationService.notifyLocalPartnerInvestmentApproved(svc);
        notificationService.notifyInvestorsNewInvestmentService(svc);
        return ResponseEntity.ok(Map.of("message", "Investment service approved"));
    }

    @PutMapping("/tourist/{id}/approve")
    public ResponseEntity<?> approveTourist(@PathVariable Long id) {
        TouristService svc = touristRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        svc.setStatus(ServiceStatus.APPROVED);
        touristRepo.save(svc);
        notificationService.notifyLocalPartnerTouristApproved(svc);
        notificationService.notifyTouristsNewService(svc);
        return ResponseEntity.ok(Map.of("message", "Tourist service approved"));
    }

    // ── REJECT (supprime de la base) ──────────────────────────
    @DeleteMapping("/collaboration/{id}/reject")
    public ResponseEntity<?> rejectCollaboration(@PathVariable Long id) {
        CollaborationService svc = collaborationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        notificationService.notifyLocalPartnerServiceRejected(svc);
        collaborationRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Collaboration service rejected and deleted"));
    }

    @DeleteMapping("/investment/{id}/reject")
    public ResponseEntity<?> rejectInvestment(@PathVariable Long id) {
        InvestmentService svc = investmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        notificationService.notifyLocalPartnerInvestmentRejected(svc);
        investmentRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Investment service rejected and deleted"));
    }

    @DeleteMapping("/tourist/{id}/reject")
    public ResponseEntity<?> rejectTourist(@PathVariable Long id) {
        TouristService svc = touristRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        notificationService.notifyLocalPartnerTouristRejected(svc);
        touristRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Tourist service rejected and deleted"));
    }
}