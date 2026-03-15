package tn.iset.investplatformpfe.Service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.investplatformpfe.Entity.CollaborationService;
import tn.iset.investplatformpfe.Entity.Region;
import tn.iset.investplatformpfe.Entity.Availability;
import tn.iset.investplatformpfe.Entity.ServiceStatus;
import tn.iset.investplatformpfe.Repository.CollaborationServiceRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CollaborationServiceService {

    private final CollaborationServiceRepository repository;
    private final PartenaireLocalRepository partenaireLocalRepository;
    private final NotificationService notificationService;

    public CollaborationServiceService(CollaborationServiceRepository repository,
                                       PartenaireLocalRepository partenaireLocalRepository,
                                       NotificationService notificationService) {
        this.repository = repository;
        this.partenaireLocalRepository = partenaireLocalRepository;
        this.notificationService = notificationService;
    }

    // ========================================
    // CREATE WITH PROVIDER ID
    // ========================================
    @Transactional
    public CollaborationService createCollaborationServiceWithProviderId(CollaborationService service, Long providerId) {
        System.out.println("DEBUT CREATION SERVICE");
        System.out.println("Provider ID recu: " + providerId);

        // ETAPE 1 : Recuperer le provider
        System.out.println("Recherche du provider avec ID: " + providerId);
        PartenaireLocal provider = partenaireLocalRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider non trouve avec id: " + providerId));
        System.out.println("Provider trouve: " + provider.getEmail() + " (ID: " + provider.getId() + ")");

        // ETAPE 2 : Creer un nouvel objet
        CollaborationService newService = new CollaborationService();

        // Copier toutes les proprietes
        newService.setName(service.getName());
        newService.setDescription(service.getDescription());
        newService.setRegion(service.getRegion());
        newService.setPrice(service.getPrice());
        newService.setAvailability(service.getAvailability());
        newService.setContactPerson(service.getContactPerson());
        newService.setCollaborationType(service.getCollaborationType());
        newService.setActivityDomain(service.getActivityDomain());
        newService.setExpectedBenefits(service.getExpectedBenefits());
        newService.setRequiredSkills(service.getRequiredSkills());
        newService.setCollaborationDuration(service.getCollaborationDuration());
        newService.setAddress(service.getAddress());

        // ETAPE 3 : Assigner le provider au nouvel objet
        newService.setProvider(provider);
        System.out.println("Provider assigne - ID: " + newService.getProvider().getId());

        // ETAPE 4 : Valider tous les champs
        System.out.println("Validation des champs...");
        validateRequiredFields(newService);
        System.out.println("Tous les champs sont valides");

        // ETAPE 5 : Sauvegarder
        System.out.println("Sauvegarde en base de donnees...");
        try {
            CollaborationService saved = repository.save(newService);
            System.out.println("Service sauvegarde avec ID: " + saved.getId());
            System.out.println("Provider_id en base: " + saved.getProvider().getId());

            // NOTIFICATION: Prevenir les admins
            notificationService.notifyAdminNewService(saved);
            System.out.println("Notification envoyee aux admins");

            return saved;
        } catch (Exception e) {
            System.out.println("ERREUR lors de la sauvegarde: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }

    // ========================================
    // CREATE
    // ========================================
    @Transactional
    public CollaborationService createCollaborationService(CollaborationService service) {
        validateRequiredFields(service);

        if (service.getProvider() != null && service.getProvider().getId() != null) {
            PartenaireLocal provider = partenaireLocalRepository.findById(service.getProvider().getId())
                    .orElseThrow(() -> new RuntimeException("Provider not found with id: " + service.getProvider().getId()));
            service.setProvider(provider);
        }

        CollaborationService saved = repository.save(service);

        notificationService.notifyAdminNewService(saved);

        return saved;
    }

    // ========================================
    // READ (GET ALL)
    // ========================================
    public List<CollaborationService> getAllCollaborationServices() {
        return repository.findAll();
    }

    // ========================================
    // READ (GET BY ID)
    // ========================================
    public CollaborationService getCollaborationServiceById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Collaboration service not found with id: " + id));
    }

    // ========================================
    // READ (GET BY REGION)
    // ========================================
    public List<CollaborationService> getCollaborationServicesByRegion(Region region) {
        return repository.findByRegion(region);
    }

    // ========================================
    // READ (GET BY PROVIDER ID)
    // ========================================
    public List<CollaborationService> getCollaborationServicesByProviderId(Long providerId) {
        PartenaireLocal provider = partenaireLocalRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Provider not found with id: " + providerId));
        return repository.findByProvider(provider);
    }

    // ========================================
    // READ (GET BY PROVIDER)
    // ========================================
    public List<CollaborationService> getCollaborationServicesByProvider(PartenaireLocal provider) {
        return repository.findByProvider(provider);
    }

    // ========================================
    // READ (GET BY STATUS)
    // ========================================
    public List<CollaborationService> getCollaborationServicesByStatus(ServiceStatus status) {
        return repository.findByStatus(status);
    }

    // ========================================
    // READ (GET PENDING SERVICES)
    // ========================================
    public List<CollaborationService> getPendingCollaborationServices() {
        return repository.findByStatus(ServiceStatus.PENDING);
    }

    // ========================================
    // READ (GET APPROVED SERVICES)
    // ========================================
    public List<CollaborationService> getApprovedCollaborationServices() {
        return repository.findByStatus(ServiceStatus.APPROVED);
    }

    // ========================================
    // READ (GET REJECTED SERVICES)
    // ========================================
    public List<CollaborationService> getRejectedCollaborationServices() {
        return repository.findByStatus(ServiceStatus.REJECTED);
    }

    // ========================================
    // READ (GET BY AVAILABILITY)
    // ========================================
    public List<CollaborationService> getCollaborationServicesByAvailability(Availability availability) {
        return repository.findByAvailability(availability);
    }

    // ========================================
    // READ (GET BY PRICE RANGE)
    // ========================================
    public List<CollaborationService> getCollaborationServicesByPriceRange(BigDecimal min, BigDecimal max) {
        if (min == null) min = BigDecimal.ZERO;
        if (max == null) max = BigDecimal.valueOf(Double.MAX_VALUE);
        return repository.findByPriceBetween(min, max);
    }

    // ========================================
    // READ (GET BY REGION AND ACTIVITY DOMAIN)
    // ========================================
    public List<CollaborationService> getCollaborationServicesByRegionAndDomain(Region region, String domain) {
        return repository.findByRegionAndActivityDomain(region, domain);
    }

    // ========================================
    // READ (GET BY COLLABORATION TYPE)
    // ========================================
    public List<CollaborationService> getCollaborationServicesByCollaborationType(String type) {
        return repository.findByCollaborationType(type);
    }

    // ========================================
    // READ (GET BY ACTIVITY DOMAIN)
    // ========================================
    public List<CollaborationService> getCollaborationServicesByActivityDomain(String domain) {
        return repository.findByActivityDomain(domain);
    }

    // ========================================
    // UPDATE
    // ========================================
    // ========================================
// UPDATE (VERSION AVEC LOGS)
// ========================================
    @Transactional
    public CollaborationService updateCollaborationService(Long id, CollaborationService serviceDetails) {
        System.out.println(" TENTATIVE DE MODIFICATION - Service ID: " + id);

        CollaborationService existingService = getCollaborationServiceById(id);
        System.out.println("   Statut actuel: " + existingService.getStatus());

        // VÉRIFICATION CRITIQUE
        if (existingService.getStatus() != ServiceStatus.PENDING) {
            String errorMsg = " MODIFICATION REFUSÉE - Service non modifiable (statut: " + existingService.getStatus() + ")";
            System.out.println(errorMsg);
            throw new RuntimeException("Cannot modify a service that is not in PENDING status. Current status: " + existingService.getStatus());
        }

        System.out.println("Service en PENDING - Modification autorisée");

        // Mise à jour des champs
        if (serviceDetails.getName() != null) {
            System.out.println("   - Mise à jour nom: " + serviceDetails.getName());
            existingService.setName(serviceDetails.getName());
        }
        if (serviceDetails.getDescription() != null) {
            existingService.setDescription(serviceDetails.getDescription());
        }
        if (serviceDetails.getRegion() != null) {
            existingService.setRegion(serviceDetails.getRegion());
        }

        if (serviceDetails.getProvider() != null && serviceDetails.getProvider().getId() != null) {
            PartenaireLocal provider = partenaireLocalRepository.findById(serviceDetails.getProvider().getId())
                    .orElseThrow(() -> new RuntimeException("Provider not found with id: " + serviceDetails.getProvider().getId()));
            existingService.setProvider(provider);
        }

        if (serviceDetails.getPrice() != null) {
            existingService.setPrice(serviceDetails.getPrice());
        }
        if (serviceDetails.getAvailability() != null) {
            existingService.setAvailability(serviceDetails.getAvailability());
        }
        if (serviceDetails.getContactPerson() != null) {
            existingService.setContactPerson(serviceDetails.getContactPerson());
        }
        if (serviceDetails.getCollaborationType() != null) {
            existingService.setCollaborationType(serviceDetails.getCollaborationType());
        }
        if (serviceDetails.getActivityDomain() != null) {
            existingService.setActivityDomain(serviceDetails.getActivityDomain());
        }
        if (serviceDetails.getExpectedBenefits() != null) {
            existingService.setExpectedBenefits(serviceDetails.getExpectedBenefits());
        }
        if (serviceDetails.getRequiredSkills() != null) {
            existingService.setRequiredSkills(serviceDetails.getRequiredSkills());
        }
        if (serviceDetails.getCollaborationDuration() != null) {
            existingService.setCollaborationDuration(serviceDetails.getCollaborationDuration());
        }
        if (serviceDetails.getAddress() != null) {
            existingService.setAddress(serviceDetails.getAddress());
        }

        CollaborationService updated = repository.save(existingService);
        System.out.println(" Service modifié avec succès - Nouveau statut: " + updated.getStatus());

        return updated;
    }
    // ADMIN: APPROVE SERVICE
    // ========================================
    @Transactional
    public CollaborationService approveService(Long id) {
        CollaborationService service = getCollaborationServiceById(id);
        service.setStatus(ServiceStatus.APPROVED);
        CollaborationService approved = repository.save(service);

        notificationService.notifyLocalPartnerServiceApproved(approved);
        notificationService.notifyPartnersAndCompaniesNewOpportunity(approved);

        return approved;
    }

    // ========================================
    // ADMIN: REJECT SERVICE
    // ========================================
    @Transactional
    public CollaborationService rejectService(Long id) {
        CollaborationService service = getCollaborationServiceById(id);
        service.setStatus(ServiceStatus.REJECTED);
        CollaborationService rejected = repository.save(service);

        notificationService.notifyLocalPartnerServiceRejected(rejected);

        return rejected;
    }

    // ========================================
    // DELETE (HARD DELETE)
    // ========================================
    @Transactional
    public void deleteCollaborationService(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Collaboration service not found with id: " + id);
        }
        repository.deleteById(id);
    }

    // ========================================
    // SEARCH BY KEYWORD
    // ========================================
    public List<CollaborationService> searchCollaborationServices(String keyword) {
        return repository.findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(keyword, keyword);
    }

    // ========================================
    // VALIDATION AVEC PROVIDER
    // ========================================
    private void validateRequiredFields(CollaborationService service) {
        if (service.getName() == null || service.getName().trim().isEmpty()) {
            throw new RuntimeException("Service name is required");
        }
        if (service.getRegion() == null) {
            throw new RuntimeException("Region is required");
        }
        if (service.getProvider() == null) {
            throw new RuntimeException("Provider is required");
        }
        if (service.getPrice() == null) {
            throw new RuntimeException("Price is required");
        }
        if (service.getAvailability() == null) {
            throw new RuntimeException("Availability is required");
        }
        if (service.getContactPerson() == null || service.getContactPerson().trim().isEmpty()) {
            throw new RuntimeException("Contact person is required");
        }
    }

    // ========================================
    // VALIDATION SANS PROVIDER
    // ========================================
    private void validateRequiredFieldsWithoutProvider(CollaborationService service) {
        if (service.getName() == null || service.getName().trim().isEmpty()) {
            throw new RuntimeException("Service name is required");
        }
        if (service.getRegion() == null) {
            throw new RuntimeException("Region is required");
        }
        if (service.getPrice() == null) {
            throw new RuntimeException("Price is required");
        }
        if (service.getAvailability() == null) {
            throw new RuntimeException("Availability is required");
        }
        if (service.getContactPerson() == null || service.getContactPerson().trim().isEmpty()) {
            throw new RuntimeException("Contact person is required");
        }
    }
}