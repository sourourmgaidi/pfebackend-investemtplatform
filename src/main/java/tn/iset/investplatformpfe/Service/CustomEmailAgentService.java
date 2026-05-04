package tn.iset.investplatformpfe.Service;


import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Dto.CustomMessageRequest;
import tn.iset.investplatformpfe.Entity.Prospect;
import tn.iset.investplatformpfe.Repository.ProspectRepository;

import java.util.List;

@Service
public class CustomEmailAgentService {

    private final ProspectRepository repo;
    private final CustomMessageService customMessageService;
    private final RichEmailService richEmailService;

    public CustomEmailAgentService(ProspectRepository repo,
                                   CustomMessageService customMessageService,
                                   RichEmailService richEmailService) {
        this.repo = repo;
        this.customMessageService = customMessageService;
        this.richEmailService = richEmailService;
    }

    public String run(CustomMessageRequest request) {
        // 1. Reformuler le message via Ollama
        String professionalMessage = customMessageService.reformulate(request.getRawMessage());

        // 2. Récupérer tous les emails du CSV (tous les prospects)
        List<Prospect> prospects = repo.findAll();

        if (prospects.isEmpty()) {
            return "Aucun prospect trouvé. Importez d'abord un fichier CSV.";
        }

        int successCount = 0;
        int failCount = 0;

        // 3. Envoyer à chaque prospect
        for (Prospect p : prospects) {
            try {
                boolean sent = richEmailService.sendWithImage(
                        p.getEmail(),
                        request.getSubject(),
                        professionalMessage,
                        request.getImageBase64(),
                        request.getImageName()
                );
                if (sent) successCount++;
                else failCount++;

                Thread.sleep(500); // éviter le spam
            } catch (Exception e) {
                failCount++;
                e.printStackTrace();
            }
        }

        return String.format("✅ %d emails envoyés, ❌ %d échecs sur %d prospects.",
                successCount, failCount, prospects.size());
    }
}
