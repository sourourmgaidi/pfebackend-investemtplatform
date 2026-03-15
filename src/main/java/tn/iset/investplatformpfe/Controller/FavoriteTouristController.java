package tn.iset.investplatformpfe.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.Tourist;
import tn.iset.investplatformpfe.Entity.TouristService;
import tn.iset.investplatformpfe.Repository.TouristRepository;
import tn.iset.investplatformpfe.Service.FavoriteTouristService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tourist/favorites")
@CrossOrigin(origins = "http://localhost:4200")
public class FavoriteTouristController {

    private final FavoriteTouristService favoriteTouristService;
    private final TouristRepository touristRepository;

    public FavoriteTouristController(
            FavoriteTouristService favoriteTouristService,
            TouristRepository touristRepository) {
        this.favoriteTouristService = favoriteTouristService;
        this.touristRepository = touristRepository;
    }

    /**
     * Obtenir l'ID du touriste connecté à partir du JWT
     */
    private Long getTouristIdFromJwt(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        Tourist tourist = touristRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Touriste non trouvé avec l'email: " + email));
        return tourist.getId();
    }

    /**
     * Ajouter un service aux favoris
     * POST /api/tourist/favorites/add/{serviceId}
     */
    @PostMapping("/add/{serviceId}")
    public ResponseEntity<?> addFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serviceId) {

        try {
            Long touristId = getTouristIdFromJwt(jwt);
            TouristService service = favoriteTouristService.addTouristFavorite(touristId, serviceId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Service ajouté aux favoris",
                    "service", service
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Retirer un service des favoris
     * DELETE /api/tourist/favorites/remove/{serviceId}
     */
    @DeleteMapping("/remove/{serviceId}")
    public ResponseEntity<?> removeFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serviceId) {

        try {
            Long touristId = getTouristIdFromJwt(jwt);
            favoriteTouristService.removeTouristFavorite(touristId, serviceId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Service retiré des favoris"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Récupérer tous les favoris du touriste connecté
     * GET /api/tourist/favorites
     */
    @GetMapping
    public ResponseEntity<?> getFavorites(@AuthenticationPrincipal Jwt jwt) {
        try {
            Long touristId = getTouristIdFromJwt(jwt);
            List<TouristService> favorites = favoriteTouristService.getTouristFavorites(touristId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "favorites", favorites,
                    "count", favorites.size()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Vérifier si un service est en favori
     * GET /api/tourist/favorites/check/{serviceId}
     */
    @GetMapping("/check/{serviceId}")
    public ResponseEntity<?> checkFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long serviceId) {

        try {
            Long touristId = getTouristIdFromJwt(jwt);
            boolean isFavorite = favoriteTouristService.isTouristFavorite(touristId, serviceId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "isFavorite", isFavorite
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Compter le nombre de favoris
     * GET /api/tourist/favorites/count
     */
    @GetMapping("/count")
    public ResponseEntity<?> countFavorites(@AuthenticationPrincipal Jwt jwt) {
        try {
            Long touristId = getTouristIdFromJwt(jwt);
            int count = favoriteTouristService.countTouristFavorites(touristId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", count
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
