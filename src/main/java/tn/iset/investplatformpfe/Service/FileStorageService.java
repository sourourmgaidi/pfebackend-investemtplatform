package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${investment.service.upload-dir}")
    private String investmentUploadDir;

    @Value("${collaboration.service.upload-dir}")
    private String collaborationUploadDir;
    @Value("${tourist.service.upload-dir}")
    private String touristUploadDir;

    // Type de service
    public enum ServiceType {
        INVESTMENT,
        COLLABORATION,
        TOURIST
    }

    /**
     * Sauvegarder un fichier dans le dossier approprié
     * @param file le fichier à sauvegarder
     * @param serviceType INVESTMENT ou COLLABORATION
     * @return le nom unique du fichier généré
     * @throws IOException en cas d'erreur
     */
    public String storeFile(MultipartFile file, ServiceType serviceType) throws IOException {
        // Choisir le bon dossier selon le type
        String uploadDir = getUploadDir(serviceType);

        // Créer le dossier si nécessaire
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            System.out.println("✅ Dossier créé: " + uploadPath.toAbsolutePath());
        }

        // Générer un nom unique
        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }

        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        Path filePath = uploadPath.resolve(uniqueFileName);

        // Sauvegarder le fichier
        Files.copy(file.getInputStream(), filePath);

        System.out.println("✅ Fichier sauvegardé: " + filePath.toAbsolutePath());

        // Retourner SEULEMENT le nom du fichier
        return uniqueFileName;
    }

    /**
     * Obtenir le chemin complet d'un fichier
     * @param fileName nom du fichier
     * @param serviceType INVESTMENT ou COLLABORATION
     * @return le chemin Path du fichier
     */
    public Path getFilePath(String fileName, ServiceType serviceType) {
        String uploadDir = getUploadDir(serviceType);
        return Paths.get(uploadDir).resolve(fileName).normalize();
    }

    /**
     * Supprimer un fichier
     * @param fileName nom du fichier
     * @param serviceType INVESTMENT ou COLLABORATION
     * @return true si supprimé, false sinon
     */
    public boolean deleteFile(String fileName, ServiceType serviceType) {
        try {
            String uploadDir = getUploadDir(serviceType);
            Path filePath = Paths.get(uploadDir).resolve(fileName).normalize();
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Récupérer le dossier d'upload selon le type de service
     */
    /**
     * Récupérer le dossier d'upload selon le type de service
     */
    private String getUploadDir(ServiceType serviceType) {
        switch (serviceType) {
            case INVESTMENT:
                return investmentUploadDir;
            case COLLABORATION:
                return collaborationUploadDir;
            case TOURIST:  // ✅ AJOUTEZ CE CASE
                return "C:/uploads/tourist/";  // ou utilisez une variable @Value
            default:
                throw new IllegalArgumentException("Type de service inconnu: " + serviceType);
        }
    }
}