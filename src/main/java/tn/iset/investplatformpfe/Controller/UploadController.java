package tn.iset.investplatformpfe.Controller;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.iset.investplatformpfe.Service.UploadService;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    /**
     * Endpoint pour uploader une photo de profil
     * Fonctionne pour tous les types d'utilisateurs
     */
    @PostMapping("/profile-photo")
    public ResponseEntity<?> uploadProfilePhoto(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("fichier") MultipartFile fichier) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("erreur", "Non authentifié"));
        }

        String email = jwt.getClaimAsString("email");

        try {
            // Vérifier que le fichier n'est pas vide
            if (fichier.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("erreur", "Fichier vide"));
            }

            // Vérifier le type de fichier
            String contentType = fichier.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("erreur", "Le fichier doit être une image"));
            }

            // Vérifier la taille (max 10MB)
            if (fichier.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest().body(Map.of("erreur", "La taille du fichier ne doit pas dépasser 10MB"));
            }

            // Uploader la photo et obtenir l'URL
            String photoUrl = uploadService.uploadProfilePhoto(email, fichier);

            return ResponseEntity.ok(Map.of(
                    "message", "Photo uploadée avec succès",
                    "photoUrl", photoUrl
            ));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "erreur", "Erreur lors de l'upload: " + e.getMessage()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "erreur", e.getMessage()
            ));
        }
    }

    /**
     * Endpoint pour supprimer la photo de profil
     */
    @DeleteMapping("/profile-photo")
    public ResponseEntity<?> deleteProfilePhoto(@AuthenticationPrincipal Jwt jwt) {

        if (jwt == null) {
            return ResponseEntity.status(401).body(Map.of("erreur", "Non authentifié"));
        }

        String email = jwt.getClaimAsString("email");

        try {
            uploadService.deleteProfilePhoto(email);
            return ResponseEntity.ok(Map.of("message", "Photo supprimée avec succès"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("erreur", e.getMessage()));
        }
    }
}
