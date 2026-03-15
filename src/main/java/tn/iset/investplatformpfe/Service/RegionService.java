package tn.iset.investplatformpfe.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Repository.*;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RegionService {

    private final RegionRepository regionRepository;
    private final EconomicSectorRepository economicSectorRepository;
    private final LocalProductRepository localProductRepository;  // CORRECTION: Changer le type
    private final BusinessOpportunityRepository businessOpportunityRepository;

    public RegionService(RegionRepository regionRepository,
                         EconomicSectorRepository economicSectorRepository,
                         LocalProductRepository localProductRepository,  // CORRECTION
                         BusinessOpportunityRepository businessOpportunityRepository) {
        this.regionRepository = regionRepository;
        this.economicSectorRepository = economicSectorRepository;
        this.localProductRepository = localProductRepository;  // CORRECTION
        this.businessOpportunityRepository = businessOpportunityRepository;
    }

    // ========================================
    // CREATE
    // ========================================
    @Transactional
    public Region createRegion(Region region) {
        validateRegion(region);
        return regionRepository.save(region);
    }

    // ========================================
    // READ (GET ALL)
    // ========================================
    public List<Region> getAllRegions() {
        return regionRepository.findAll();
    }

    // ========================================
    // READ (GET BY ID)
    // ========================================
    public Region getRegionById(Long id) {
        return regionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Region not found with id: " + id));
    }

    public List<EconomicSector> getEconomicSectorsByRegion(Long regionId) {
        Region region = getRegionById(regionId);
        return region.getRegionEconomicSectors()
                .stream()
                .map(RegionEconomicSector::getEconomicSector)
                .collect(Collectors.toList());
    }
    // ========================================
    // READ (GET BY NAME)
    // ========================================
    public Region getRegionByName(String name) {
        return regionRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Region not found with name: " + name));
    }

    // ========================================
    // READ (GET BY CODE)
    // ========================================
    public Region getRegionByCode(String code) {
        return regionRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Region not found with code: " + code));
    }

    // ========================================
    // READ (GET BY GEOGRAPHICAL ZONE)
    // ========================================
    public List<Region> getRegionsByGeographicalZone(String zone) {
        return regionRepository.findByGeographicalZone(zone);
    }

    // ========================================
    // UPDATE
    // ========================================
    @Transactional
    public Region updateRegion(Long id, Region regionDetails) {
        Region existingRegion = getRegionById(id);

        existingRegion.setName(regionDetails.getName());
        existingRegion.setEconomicDescription(regionDetails.getEconomicDescription());
        existingRegion.setTaxIncentives(regionDetails.getTaxIncentives());
        existingRegion.setInfrastructure(regionDetails.getInfrastructure());
        existingRegion.setCode(regionDetails.getCode());
        existingRegion.setGeographicalZone(regionDetails.getGeographicalZone());

        return regionRepository.save(existingRegion);
    }

    // ========================================
    // DELETE
    // ========================================
    @Transactional
    public void deleteRegion(Long id) {
        if (!regionRepository.existsById(id)) {
            throw new RuntimeException("Region not found with id: " + id);
        }
        regionRepository.deleteById(id);
    }

    // ========================================
    // ADD SECTORS TO REGION
    // ========================================
    @Transactional
    public Region addSectorsToRegion(Long regionId, List<Long> sectorIds) {
        Region region = getRegionById(regionId);
        List<EconomicSector> sectors = economicSectorRepository.findAllById(sectorIds);

        if (sectors.isEmpty()) {
            throw new RuntimeException("No sectors found with provided ids");
        }

        region.getPotentialSectors().addAll(sectors);
        return regionRepository.save(region);
    }

    // ========================================
    // REMOVE SECTORS FROM REGION
    // ========================================
    @Transactional
    public Region removeSectorsFromRegion(Long regionId, List<Long> sectorIds) {
        Region region = getRegionById(regionId);
        region.getPotentialSectors().removeIf(sector -> sectorIds.contains(sector.getId()));
        return regionRepository.save(region);
    }

    // ========================================
    // ADD PRODUCTS TO REGION
    // ========================================
    @Transactional
    public Region addProductsToRegion(Long regionId, List<Long> productIds) {
        Region region = getRegionById(regionId);
        List<LocalProduct> products = localProductRepository.findAllById(productIds);  // CORRECTION: Utiliser LocalProduct

        if (products.isEmpty()) {
            throw new RuntimeException("No products found with provided ids");
        }

        region.getLocalProducts().addAll(products);
        return regionRepository.save(region);
    }

    // ========================================
    // REMOVE PRODUCTS FROM REGION
    // ========================================
    @Transactional
    public Region removeProductsFromRegion(Long regionId, List<Long> productIds) {
        Region region = getRegionById(regionId);
        region.getLocalProducts().removeIf(product -> productIds.contains(product.getId()));
        return regionRepository.save(region);
    }

    // ========================================
    // ADD OPPORTUNITIES TO REGION
    // ========================================
    @Transactional
    public Region addOpportunitiesToRegion(Long regionId, List<Long> opportunityIds) {
        Region region = getRegionById(regionId);
        List<BusinessOpportunity> opportunities = businessOpportunityRepository.findAllById(opportunityIds);

        if (opportunities.isEmpty()) {
            throw new RuntimeException("No opportunities found with provided ids");
        }

        region.getBusinessOpportunities().addAll(opportunities);
        return regionRepository.save(region);
    }

    // ========================================
    // REMOVE OPPORTUNITIES FROM REGION
    // ========================================
    @Transactional
    public Region removeOpportunitiesFromRegion(Long regionId, List<Long> opportunityIds) {
        Region region = getRegionById(regionId);
        region.getBusinessOpportunities().removeIf(opp -> opportunityIds.contains(opp.getId()));
        return regionRepository.save(region);
    }

    // ========================================
    // GET REGIONS WITH COLLABORATION STATS
    // ========================================
    public List<Object[]> getRegionsWithCollaborationStats() {
        return regionRepository.findRegionsWithCollaborationCount();
    }

    // ========================================
    // SEARCH REGIONS
    // ========================================
    public List<Region> searchRegions(String keyword) {
        return regionRepository.findByNameContainingIgnoreCaseOrGeographicalZoneContainingIgnoreCase(keyword, keyword);
    }

    // ========================================
    // VALIDATION
    // ========================================
    private void validateRegion(Region region) {
        if (region.getName() == null || region.getName().trim().isEmpty()) {
            throw new RuntimeException("Region name is required");
        }

        if (regionRepository.existsByName(region.getName())) {
            throw new RuntimeException("Region with name '" + region.getName() + "' already exists");
        }
    }
}