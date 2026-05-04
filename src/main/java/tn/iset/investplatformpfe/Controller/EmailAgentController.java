package tn.iset.investplatformpfe.Controller;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.iset.investplatformpfe.Entity.Prospect;
import tn.iset.investplatformpfe.Repository.ProspectRepository;
import tn.iset.investplatformpfe.Service.CsvService;
import tn.iset.investplatformpfe.Service.EmailAgentService;
import tn.iset.investplatformpfe.Dto.CustomMessageRequest;
import tn.iset.investplatformpfe.Service.CustomEmailAgentService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/email-agent")
public class EmailAgentController {

    private final CsvService csvService;
    private final ProspectRepository repo;
    private final EmailAgentService agentService;

    private final CustomEmailAgentService customEmailAgentService;

    // Modifier le constructeur :
    public EmailAgentController(CsvService csvService,
                                ProspectRepository repo,
                                EmailAgentService agentService,
                                CustomEmailAgentService customEmailAgentService) {
        this.csvService = csvService;
        this.repo = repo;
        this.agentService = agentService;
        this.customEmailAgentService = customEmailAgentService;
    }

    private boolean isAdmin(Jwt jwt) {
        if (jwt == null) {
            System.out.println("isAdmin: jwt is null");
            return false;
        }
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            System.out.println("isAdmin: realm_access is null");
            return false;
        }
        List<String> roles = (List<String>) realmAccess.get("roles");
        System.out.println("isAdmin: roles = " + roles);
        return roles != null && roles.contains("ADMIN");
    }

    @PostMapping("/import")
    public ResponseEntity<?> importCsv(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam("file") MultipartFile file) {
        System.out.println("📁 IMPORT appelé, fichier : " + file.getOriginalFilename() + ", taille : " + file.getSize());

        if (!isAdmin(jwt)) {
            System.out.println("⛔ Accès refusé : admin requis");
            return ResponseEntity.status(403).body("Access denied");
        }

        try {
            List<Prospect> list = csvService.parse(file);
            System.out.println("📊 Nombre de prospects parsés : " + list.size());
            repo.saveAll(list);
            System.out.println("✅ Import réussi, " + list.size() + " enregistrements sauvegardés");
            return ResponseEntity.ok("Import success");
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'import : " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Import error: " + e.getMessage());
        }
    }

    @PostMapping("/run")
    public ResponseEntity<?> run(@AuthenticationPrincipal Jwt jwt) {
        System.out.println("🚀 /run appelé");
        if (!isAdmin(jwt)) {
            System.out.println("⛔ Accès refusé pour /run");
            return ResponseEntity.status(403).body("Access denied");
        }
        agentService.run();
        return ResponseEntity.ok("Agent executed");
    }

    @GetMapping("/all")
    public List<Prospect> getAll(@AuthenticationPrincipal Jwt jwt) {
        System.out.println("🔍 /all appelé");
        if (!isAdmin(jwt)) {
            System.out.println("⛔ Accès refusé pour /all");
            return List.of();
        }
        List<Prospect> prospects = repo.findAll();
        System.out.println("📋 Nombre de prospects trouvés : " + prospects.size());
        return prospects;
    }

    // Dans EmailAgentController.java, ajouter :

    @DeleteMapping("/clear")
    public ResponseEntity<?> clearAll(@AuthenticationPrincipal Jwt jwt) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        repo.deleteAll();
        return ResponseEntity.ok("Liste supprimée");
    }

    // Ajouter ces imports en haut :

    // Ajouter dans la classe :


// Ajouter ces endpoints :


    @PostMapping("/send-custom")
    public ResponseEntity<?> sendCustom(@AuthenticationPrincipal Jwt jwt,
                                        @RequestBody CustomMessageRequest request) {
        if (!isAdmin(jwt)) return ResponseEntity.status(403).body("Access denied");
        String result = customEmailAgentService.run(request);
        return ResponseEntity.ok(result);
    }
}

