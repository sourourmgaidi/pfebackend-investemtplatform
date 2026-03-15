package tn.iset.investplatformpfe.Service;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.investplatformpfe.Entity.Tourist;
import tn.iset.investplatformpfe.Entity.TouristService;
import tn.iset.investplatformpfe.Entity.ServiceStatus;
import tn.iset.investplatformpfe.Repository.TouristRepository;
import tn.iset.investplatformpfe.Repository.TouristServiceRepository;

import java.util.List;
import java.util.ArrayList;

@Service
public class FavoriteTouristService {

    private final TouristRepository touristRepository;
    private final TouristServiceRepository touristServiceRepository;

    public FavoriteTouristService(
            TouristRepository touristRepository,
            TouristServiceRepository touristServiceRepository) {
        this.touristRepository = touristRepository;
        this.touristServiceRepository = touristServiceRepository;
    }

    // ========================================
    // POUR TOURIST
    // ========================================

    /**
     * Ajouter un service touristique aux favoris d'un touriste
     */
    @Transactional
    public TouristService addTouristFavorite(Long touristId, Long serviceId) {
        Tourist tourist = touristRepository.findById(touristId)
                .orElseThrow(() -> new RuntimeException("Touriste non trouvé avec l'ID: " + touristId));

        TouristService service = touristServiceRepository.findById(serviceId)
                .orElseThrow(() -> new RuntimeException("Service touristique non trouvé avec l'ID: " + serviceId));

        // Vérifier que le service est approuvé
        if (service.getStatus() != ServiceStatus.APPROVED) {
            throw new RuntimeException("Seuls les services approuvés peuvent être ajoutés aux favoris");
        }

        // Initialiser la liste si elle est null
        if (tourist.getFavoriteTouristServices() == null) {
            tourist.setFavoriteTouristServices(new ArrayList<>());
        }

        // Vérifier si le service est déjà en favori
        boolean alreadyFavorite = tourist.getFavoriteTouristServices().stream()
                .anyMatch(s -> s.getId().equals(serviceId));

        if (!alreadyFavorite) {
            tourist.getFavoriteTouristServices().add(service);
            touristRepository.save(tourist);
            System.out.println("✅ Service touristique " + serviceId + " ajouté aux favoris du touriste " + touristId);
        } else {
            System.out.println("ℹ️ Service déjà en favori");
        }

        return service;
    }

    /**
     * Retirer un service touristique des favoris d'un touriste
     */
    @Transactional
    public void removeTouristFavorite(Long touristId, Long serviceId) {
        Tourist tourist = touristRepository.findById(touristId)
                .orElseThrow(() -> new RuntimeException("Touriste non trouvé avec l'ID: " + touristId));

        if (tourist.getFavoriteTouristServices() != null) {
            boolean removed = tourist.getFavoriteTouristServices().removeIf(s -> s.getId().equals(serviceId));
            if (removed) {
                touristRepository.save(tourist);
                System.out.println("✅ Service touristique " + serviceId + " retiré des favoris du touriste " + touristId);
            }
        }
    }

    /**
     * Récupérer la liste des favoris d'un touriste
     */
    public List<TouristService> getTouristFavorites(Long touristId) {
        Tourist tourist = touristRepository.findById(touristId)
                .orElseThrow(() -> new RuntimeException("Touriste non trouvé avec l'ID: " + touristId));

        return tourist.getFavoriteTouristServices() != null ?
                tourist.getFavoriteTouristServices() :
                List.of();
    }

    /**
     * Vérifier si un service est en favori pour un touriste
     */
    public boolean isTouristFavorite(Long touristId, Long serviceId) {
        Tourist tourist = touristRepository.findById(touristId)
                .orElseThrow(() -> new RuntimeException("Touriste non trouvé avec l'ID: " + touristId));

        return tourist.getFavoriteTouristServices() != null &&
                tourist.getFavoriteTouristServices().stream()
                        .anyMatch(s -> s.getId().equals(serviceId));
    }

    /**
     * Compter le nombre de favoris d'un touriste
     */
    public int countTouristFavorites(Long touristId) {
        Tourist tourist = touristRepository.findById(touristId)
                .orElseThrow(() -> new RuntimeException("Touriste non trouvé avec l'ID: " + touristId));

        return tourist.getFavoriteTouristServices() != null ?
                tourist.getFavoriteTouristServices().size() : 0;
    }

    // ========================================
    // ✅ NOUVELLE MÉTHODE: Retirer un service de TOUS les favoris des touristes
    // ========================================
    @Transactional
    public void removeServiceFromAllFavorites(Long serviceId) {
        System.out.println("🗑️ Suppression du service touristique " + serviceId + " de tous les favoris des touristes");

        // Récupérer tous les touristes
        List<Tourist> allTourists = touristRepository.findAll();
        int touristCount = 0;

        for (Tourist tourist : allTourists) {
            if (tourist.getFavoriteTouristServices() != null) {
                boolean removed = tourist.getFavoriteTouristServices().removeIf(s -> s.getId().equals(serviceId));
                if (removed) {
                    touristRepository.save(tourist);
                    touristCount++;
                }
            }
        }

        System.out.println("✅ Service " + serviceId + " retiré des favoris de " + touristCount + " touriste(s)");
    }
}