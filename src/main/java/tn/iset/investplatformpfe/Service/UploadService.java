package tn.iset.investplatformpfe.Service;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Repository.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class UploadService {

    private final AdminRepository adminRepository;
    private final TouristRepository touristRepository;
    private final InvestorRepository investorRepository;
    private final EconomicPartnerRepository economicPartnerRepository;
    private final LocalPartnerRepository localPartnerRepository;
    private final InternationalCompanyRepository internationalCompanyRepository;

    @Value("${upload.path.profile-photos:uploads/profile-photos}")
    private String uploadPath;

    @Value("${app.base-url:http://localhost:8089}")
    private String baseUrl;

    public UploadService(
            AdminRepository adminRepository,
            TouristRepository touristRepository,
            InvestorRepository investorRepository,
            EconomicPartnerRepository economicPartnerRepository,
            LocalPartnerRepository localPartnerRepository,
            InternationalCompanyRepository internationalCompanyRepository) {
        this.adminRepository = adminRepository;
        this.touristRepository = touristRepository;
        this.investorRepository = investorRepository;
        this.economicPartnerRepository = economicPartnerRepository;
        this.localPartnerRepository = localPartnerRepository;
        this.internationalCompanyRepository = internationalCompanyRepository;
    }

    /**
     * Uploader une photo de profil pour un utilisateur
     */
    public String uploadProfilePhoto(String email, MultipartFile fichier) throws IOException {
        // Créer le répertoire d'upload s'il n'existe pas
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Générer un nom de fichier unique
        String originalFilename = fichier.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;

        // Sauvegarder le fichier
        Path filePath = uploadDir.resolve(filename);
        Files.copy(fichier.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Construire l'URL de la photo
        String photoUrl = baseUrl + "/uploads/profile-photos/" + filename;

        // Mettre à jour le champ photo dans la base de données selon le type d'utilisateur
        updateUserPhoto(email, photoUrl);

        return photoUrl;
    }

    /**
     * Mettre à jour le champ photo dans la base de données
     */
    private void updateUserPhoto(String email, String photoUrl) {
        // Essayer chaque repository jusqu'à trouver l'utilisateur
        if (updateAdminPhoto(email, photoUrl)) return;
        if (updateTouristPhoto(email, photoUrl)) return;
        if (updateInvestorPhoto(email, photoUrl)) return;
        if (updateEconomicPartnerPhoto(email, photoUrl)) return;
        if (updateLocalPartnerPhoto(email, photoUrl)) return;
        if (updateInternationalCompanyPhoto(email, photoUrl)) return;

        throw new RuntimeException("Utilisateur non trouvé avec l'email: " + email);
    }

    private boolean updateAdminPhoto(String email, String photoUrl) {
        return adminRepository.findByEmail(email).map(admin -> {
            admin.setProfilePhoto(photoUrl);
            adminRepository.save(admin);
            return true;
        }).orElse(false);
    }

    private boolean updateTouristPhoto(String email, String photoUrl) {
        return touristRepository.findByEmail(email).map(tourist -> {
            tourist.setProfilePhoto(photoUrl);
            touristRepository.save(tourist);
            return true;
        }).orElse(false);
    }

    private boolean updateInvestorPhoto(String email, String photoUrl) {
        return investorRepository.findByEmail(email).map(investor -> {
            investor.setProfilePicture(photoUrl);
            investorRepository.save(investor);
            return true;
        }).orElse(false);
    }

    private boolean updateEconomicPartnerPhoto(String email, String photoUrl) {
        return economicPartnerRepository.findByEmail(email).map(partner -> {
            partner.setProfilePhoto(photoUrl);
            economicPartnerRepository.save(partner);
            return true;
        }).orElse(false);
    }

    private boolean updateLocalPartnerPhoto(String email, String photoUrl) {
        return localPartnerRepository.findByEmail(email).map(partner -> {
            partner.setProfilePhoto(photoUrl);
            localPartnerRepository.save(partner);
            return true;
        }).orElse(false);
    }

    private boolean updateInternationalCompanyPhoto(String email, String photoUrl) {
        return internationalCompanyRepository.findByEmail(email).map(company -> {
            company.setProfilePicture(photoUrl);
            internationalCompanyRepository.save(company);
            return true;
        }).orElse(false);
    }

    /**
     * Supprimer la photo de profil
     */
    public void deleteProfilePhoto(String email) throws IOException {
        // Récupérer l'URL de la photo actuelle
        String currentPhotoUrl = getCurrentPhotoUrl(email);

        if (currentPhotoUrl != null && !currentPhotoUrl.isEmpty()) {
            // Extraire le nom du fichier de l'URL
            String filename = currentPhotoUrl.substring(currentPhotoUrl.lastIndexOf("/") + 1);

            // Supprimer le fichier
            Path filePath = Paths.get(uploadPath).resolve(filename);
            Files.deleteIfExists(filePath);
        }

        // Mettre à jour la base de données (supprimer la référence)
        updateUserPhoto(email, null);
    }

    private String getCurrentPhotoUrl(String email) {
        // Chercher l'utilisateur et retourner sa photo
        return adminRepository.findByEmail(email)
                .map(Admin::getProfilePhoto)
                .orElseGet(() -> touristRepository.findByEmail(email)
                        .map(Tourist::getProfilePhoto)
                        .orElseGet(() -> investorRepository.findByEmail(email)
                                .map(Investor::getProfilePicture)
                                .orElseGet(() -> economicPartnerRepository.findByEmail(email)
                                        .map(EconomicPartner::getProfilePhoto)
                                        .orElseGet(() -> localPartnerRepository.findByEmail(email)
                                                .map(LocalPartner::getProfilePhoto)
                                                .orElseGet(() -> internationalCompanyRepository.findByEmail(email)
                                                        .map(internationalcompany::getProfilePicture)
                                                        .orElse(null))))));
    }
}
