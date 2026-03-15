package tn.iset.investplatformpfe.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.investplatformpfe.Entity.EconomicSector;
import tn.iset.investplatformpfe.Repository.EconomicSectorRepository;

import java.util.List;

@Service
public class EconomicSectorService {

    private final EconomicSectorRepository economicSectorRepository;

    public EconomicSectorService(EconomicSectorRepository economicSectorRepository) {
        this.economicSectorRepository = economicSectorRepository;
    }

    // ========================================
    // CREATE
    // ========================================
    @Transactional
    public EconomicSector createEconomicSector(EconomicSector sector) {
        validateSector(sector);
        return economicSectorRepository.save(sector);
    }

    // ========================================
    // READ (GET ALL)
    // ========================================
    public List<EconomicSector> getAllEconomicSectors() {
        return economicSectorRepository.findAll();
    }

    // ========================================
    // READ (GET BY ID)
    // ========================================
    public EconomicSector getEconomicSectorById(Long id) {
        return economicSectorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Economic sector not found with id: " + id));
    }

    // ========================================
    // READ (GET BY NAME)
    // ========================================
    public EconomicSector getEconomicSectorByName(String name) {
        return economicSectorRepository.findByName(name)
                .orElseThrow(() -> new RuntimeException("Economic sector not found with name: " + name));
    }

    // ========================================
    // UPDATE
    // ========================================
    @Transactional
    public EconomicSector updateEconomicSector(Long id, EconomicSector sectorDetails) {
        EconomicSector existingSector = getEconomicSectorById(id);

        // Mise à jour des champs
        if (sectorDetails.getName() != null && !sectorDetails.getName().trim().isEmpty()) {
            // Vérifier si le nouveau nom existe déjà (sauf si c'est le même)
            if (!existingSector.getName().equals(sectorDetails.getName()) &&
                    economicSectorRepository.existsByName(sectorDetails.getName())) {
                throw new RuntimeException("Economic sector with name '" + sectorDetails.getName() + "' already exists");
            }
            existingSector.setName(sectorDetails.getName());
        }

        if (sectorDetails.getDescription() != null) {
            existingSector.setDescription(sectorDetails.getDescription());
        }

        return economicSectorRepository.save(existingSector);
    }

    // ========================================
    // DELETE
    // ========================================
    @Transactional
    public void deleteEconomicSector(Long id) {
        if (!economicSectorRepository.existsById(id)) {
            throw new RuntimeException("Economic sector not found with id: " + id);
        }
        economicSectorRepository.deleteById(id);
    }

    // ========================================
    // SEARCH
    // ========================================
    public List<EconomicSector> searchEconomicSectors(String keyword) {
        return economicSectorRepository.findByNameContainingIgnoreCase(keyword);
    }

    // ========================================
    // VALIDATION
    // ========================================
    private void validateSector(EconomicSector sector) {
        if (sector.getName() == null || sector.getName().trim().isEmpty()) {
            throw new RuntimeException("Economic sector name is required");
        }

        if (economicSectorRepository.existsByName(sector.getName())) {
            throw new RuntimeException("Economic sector with name '" + sector.getName() + "' already exists");
        }
    }
}