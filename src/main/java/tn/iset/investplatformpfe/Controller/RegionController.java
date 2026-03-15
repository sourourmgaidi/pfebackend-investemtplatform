package tn.iset.investplatformpfe.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.EconomicSector;
import tn.iset.investplatformpfe.Entity.Region;
import tn.iset.investplatformpfe.Service.RegionService;

import java.util.List;

@RestController
@RequestMapping("/api/regions")
public class RegionController {

    private final RegionService regionService;

    public RegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    @PostMapping
    public ResponseEntity<Region> createRegion(@RequestBody Region region) {
        return new ResponseEntity<>(regionService.createRegion(region), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Region>> getAllRegions() {
        return ResponseEntity.ok(regionService.getAllRegions());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Region> getRegionById(@PathVariable Long id) {
        return ResponseEntity.ok(regionService.getRegionById(id));
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<Region> getRegionByName(@PathVariable String name) {
        return ResponseEntity.ok(regionService.getRegionByName(name));
    }

    @GetMapping("/zone/{zone}")
    public ResponseEntity<List<Region>> getRegionsByZone(@PathVariable String zone) {
        return ResponseEntity.ok(regionService.getRegionsByGeographicalZone(zone));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Region> updateRegion(@PathVariable Long id, @RequestBody Region region) {
        return ResponseEntity.ok(regionService.updateRegion(id, region));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRegion(@PathVariable Long id) {
        regionService.deleteRegion(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/sectors")
    public ResponseEntity<Region> addSectorsToRegion(@PathVariable Long id, @RequestBody List<Long> sectorIds) {
        return ResponseEntity.ok(regionService.addSectorsToRegion(id, sectorIds));
    }

    @DeleteMapping("/{id}/sectors")
    public ResponseEntity<Region> removeSectorsFromRegion(@PathVariable Long id, @RequestBody List<Long> sectorIds) {
        return ResponseEntity.ok(regionService.removeSectorsFromRegion(id, sectorIds));
    }

    @PutMapping("/{id}/products")
    public ResponseEntity<Region> addProductsToRegion(@PathVariable Long id, @RequestBody List<Long> productIds) {
        return ResponseEntity.ok(regionService.addProductsToRegion(id, productIds));
    }

    @PutMapping("/{id}/opportunities")
    public ResponseEntity<Region> addOpportunitiesToRegion(@PathVariable Long id, @RequestBody List<Long> opportunityIds) {
        return ResponseEntity.ok(regionService.addOpportunitiesToRegion(id, opportunityIds));
    }

    @GetMapping("/search")
    public ResponseEntity<List<Region>> searchRegions(@RequestParam String keyword) {
        return ResponseEntity.ok(regionService.searchRegions(keyword));
    }


    @GetMapping("/{regionId}/economic-sectors")
    public ResponseEntity<List<EconomicSector>> getEconomicSectorsByRegion(@PathVariable Long regionId) {
        List<EconomicSector> sectors = regionService.getEconomicSectorsByRegion(regionId);
        return ResponseEntity.ok(sectors);
    }
}