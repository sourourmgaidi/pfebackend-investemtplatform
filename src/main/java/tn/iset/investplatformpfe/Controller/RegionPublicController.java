package tn.iset.investplatformpfe.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Repository.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;

@RestController
@RequestMapping("/api/public/regions")
@CrossOrigin(origins = "*")
public class RegionPublicController {

    private final RegionRepository regionRepository;
    private final InvestmentServiceRepository investmentServiceRepo;
    private final CollaborationServiceRepository collaborationServiceRepo;
    private final TouristServiceRepository touristServiceRepo;

    public RegionPublicController(
            RegionRepository regionRepository,
            InvestmentServiceRepository investmentServiceRepo,
            CollaborationServiceRepository collaborationServiceRepo,
            TouristServiceRepository touristServiceRepo) {
        this.regionRepository = regionRepository;
        this.investmentServiceRepo = investmentServiceRepo;
        this.collaborationServiceRepo = collaborationServiceRepo;
        this.touristServiceRepo = touristServiceRepo;
    }

    // ✅ Endpoint public pour récupérer tous les services d'une région
    // ✅ INCLUT APPROVED, RESERVED ET TAKEN
    @GetMapping("/{regionId}/services")
    public ResponseEntity<Map<String, Object>> getRegionServices(@PathVariable Long regionId) {

        // Vérifier que la région existe
        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new RuntimeException("Region not found with id: " + regionId));

        // ✅ INCLURE APPROVED, RESERVED ET TAKEN
        List<ServiceStatus> statuses = Arrays.asList(
                ServiceStatus.APPROVED,
                ServiceStatus.RESERVED,
                ServiceStatus.TAKEN
        );

        // Récupérer les services avec les statuts spécifiés
        List<InvestmentService> investmentServices = investmentServiceRepo
                .findByRegionIdAndStatusIn(regionId, statuses);

        List<CollaborationService> collaborationServices = collaborationServiceRepo
                .findByRegionIdAndStatusIn(regionId, statuses);

        List<TouristService> touristServices = touristServiceRepo
                .findByRegionIdAndStatusIn(regionId, statuses);

        Map<String, Object> response = new HashMap<>();
        response.put("investmentServices", investmentServices);
        response.put("collaborationServices", collaborationServices);
        response.put("touristServices", touristServices);

        return ResponseEntity.ok(response);
    }

    // ✅ Endpoint public pour récupérer les statistiques d'une région
    @GetMapping("/{regionId}/stats")
    public ResponseEntity<Map<String, Object>> getRegionStats(@PathVariable Long regionId) {

        Region region = regionRepository.findById(regionId)
                .orElseThrow(() -> new RuntimeException("Region not found with id: " + regionId));

        // ✅ Compter APPROVED, RESERVED et TAKEN
        List<ServiceStatus> statuses = Arrays.asList(
                ServiceStatus.APPROVED,
                ServiceStatus.RESERVED,
                ServiceStatus.TAKEN
        );

        long investmentCount = investmentServiceRepo
                .countByRegionIdAndStatusIn(regionId, statuses);
        long collaborationCount = collaborationServiceRepo
                .countByRegionIdAndStatusIn(regionId, statuses);
        long touristCount = touristServiceRepo
                .countByRegionIdAndStatusIn(regionId, statuses);

        Map<String, Object> stats = new HashMap<>();
        stats.put("regionId", regionId);
        stats.put("regionName", region.getName());
        stats.put("geographicalZone", region.getGeographicalZone());
        stats.put("investmentServices", investmentCount);
        stats.put("collaborationServices", collaborationCount);
        stats.put("touristServices", touristCount);
        stats.put("totalServices", investmentCount + collaborationCount + touristCount);

        return ResponseEntity.ok(stats);
    }
}