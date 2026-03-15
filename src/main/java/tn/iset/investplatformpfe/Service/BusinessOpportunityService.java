package tn.iset.investplatformpfe.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.investplatformpfe.Entity.BusinessOpportunity;
import tn.iset.investplatformpfe.Repository.BusinessOpportunityRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BusinessOpportunityService {

    private final BusinessOpportunityRepository businessOpportunityRepository;

    public BusinessOpportunityService(BusinessOpportunityRepository businessOpportunityRepository) {
        this.businessOpportunityRepository = businessOpportunityRepository;
    }

    // ========================================
    // CREATE
    // ========================================
    @Transactional
    public BusinessOpportunity createBusinessOpportunity(BusinessOpportunity opportunity) {
        validateOpportunity(opportunity);
        opportunity.setCreatedAt(LocalDateTime.now());
        return businessOpportunityRepository.save(opportunity);
    }

    // ========================================
    // READ (GET ALL)
    // ========================================
    public List<BusinessOpportunity> getAllBusinessOpportunities() {
        return businessOpportunityRepository.findAll();
    }

    // ========================================
    // READ (GET BY ID)
    // ========================================
    public BusinessOpportunity getBusinessOpportunityById(Long id) {
        return businessOpportunityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Business opportunity not found with id: " + id));
    }

    // ========================================
    // READ (GET BY TYPE)
    // ========================================
    public List<BusinessOpportunity> getBusinessOpportunitiesByType(String type) {
        return businessOpportunityRepository.findByType(type);
    }

    // ========================================
    // READ (GET RECENT)
    // ========================================
    public List<BusinessOpportunity> getRecentBusinessOpportunities(LocalDateTime since) {
        return businessOpportunityRepository.findByCreatedAtAfter(since);
    }

    // ========================================
    // UPDATE
    // ========================================
    @Transactional
    public BusinessOpportunity updateBusinessOpportunity(Long id, BusinessOpportunity opportunityDetails) {
        BusinessOpportunity existingOpportunity = getBusinessOpportunityById(id);

        existingOpportunity.setTitle(opportunityDetails.getTitle());
        existingOpportunity.setDescription(opportunityDetails.getDescription());
        existingOpportunity.setType(opportunityDetails.getType());

        return businessOpportunityRepository.save(existingOpportunity);
    }

    // ========================================
    // DELETE
    // ========================================
    @Transactional
    public void deleteBusinessOpportunity(Long id) {
        if (!businessOpportunityRepository.existsById(id)) {
            throw new RuntimeException("Business opportunity not found with id: " + id);
        }
        businessOpportunityRepository.deleteById(id);
    }

    // ========================================
    // SEARCH
    // ========================================
    public List<BusinessOpportunity> searchBusinessOpportunities(String keyword) {
        return businessOpportunityRepository.findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword);
    }

    // ========================================
    // SEARCH BY TYPE
    // ========================================
    public List<BusinessOpportunity> searchBusinessOpportunitiesByType(String type, String keyword) {
        return businessOpportunityRepository.findByTypeAndTitleContainingIgnoreCase(type, keyword);
    }

    // ========================================
    // VALIDATION
    // ========================================
    private void validateOpportunity(BusinessOpportunity opportunity) {
        if (opportunity.getTitle() == null || opportunity.getTitle().trim().isEmpty()) {
            throw new RuntimeException("Business opportunity title is required");
        }
    }
}
