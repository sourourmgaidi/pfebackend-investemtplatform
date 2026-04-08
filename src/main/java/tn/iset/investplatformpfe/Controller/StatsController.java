package tn.iset.investplatformpfe.Controller;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Dto.MonthlyRegistrationDTO;
import tn.iset.investplatformpfe.Dto.RegionStatsDTO;
import tn.iset.investplatformpfe.Dto.RegistrationBarDTO;
import tn.iset.investplatformpfe.Dto.StatsSummaryDTO;
import tn.iset.investplatformpfe.Service.StatsService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "*")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<StatsSummaryDTO> getSummary() {
        return ResponseEntity.ok(statsService.getSummary());
    }

    @GetMapping("/regions")
    public ResponseEntity<List<RegionStatsDTO>> getRegionStats() {
        return ResponseEntity.ok(statsService.getRegionStats());
    }

    @GetMapping("/daily")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RegistrationBarDTO>> getDailyRegistrations(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        if (year == null)  year  = LocalDate.now().getYear();
        if (month == null) month = LocalDate.now().getMonthValue();

        List<RegistrationBarDTO> data = statsService.getDailyRegistrations(year, month);
        return ResponseEntity.ok(data);
    }

    // ========================================
    // NEW ENDPOINTS FOR MONTHLY STATISTICS
    // ========================================

    @GetMapping("/monthly")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<MonthlyRegistrationDTO>> getMonthlyRegistrations(
            @RequestParam(defaultValue = "12") int months) {

        List<MonthlyRegistrationDTO> data = statsService.getMonthlyRegistrations(months);
        return ResponseEntity.ok(data);
    }

    @GetMapping("/notification")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> getMonthOverMonthNotification() {
        String notification = statsService.getMonthOverMonthNotification();
        return ResponseEntity.ok(notification);
    }
}