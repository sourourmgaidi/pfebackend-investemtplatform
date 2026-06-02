package tn.iset.investplatformpfe.Service;

// src/main/java/tn/iset/investplatformpfe/Service/CustomEmailAgentService.java

import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Dto.CustomMessageRequest;
import tn.iset.investplatformpfe.Dto.SuggestionResponse;
import tn.iset.investplatformpfe.Entity.EmailSendRecord;
import tn.iset.investplatformpfe.Entity.Prospect;
import tn.iset.investplatformpfe.Repository.EmailSendRecordRepository;
import tn.iset.investplatformpfe.Repository.ProspectRepository;

import java.util.List;

@Service
public class CustomEmailAgentService {

    private final ProspectRepository repo;
    private final CustomMessageService customMessageService;
    private final RichEmailService richEmailService;
    private final EmailSendRecordRepository recordRepo; // ← AJOUTER

    public CustomEmailAgentService(ProspectRepository repo,
                                   CustomMessageService customMessageService,
                                   RichEmailService richEmailService,
                                   EmailSendRecordRepository recordRepo) { // ← AJOUTER

        this.repo = repo;
        this.customMessageService = customMessageService;
        this.richEmailService = richEmailService;
        this.recordRepo = recordRepo; // ← AJOUTER

    }

    private volatile boolean cancelRequested = false;

    public void cancelSend() {
        this.cancelRequested = true;
    }

    // === PHASE 1 : Validation + Suggestions (pas d'envoi) ===
    public SuggestionResponse preview(CustomMessageRequest request) {
        String error = customMessageService.validate(request.getRawMessage());
        if (error != null) {
            return SuggestionResponse.invalid(error);
        }
        List<String> suggestions = customMessageService.generateSuggestions(request.getRawMessage());
        return SuggestionResponse.valid(suggestions);
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

        public String send(CustomMessageRequest request) {
            cancelRequested = false;

            String finalMessage;
            if ("OWN".equals(request.getChosenMessage())) {
                finalMessage = request.getRawMessage();
            } else if (request.getChosenMessage() != null && !request.getChosenMessage().isBlank()) {
                finalMessage = request.getChosenMessage();
            } else {
                return "❌ Aucun message sélectionné.";
            }

            List<Prospect> prospects = repo.findAll();
            if (prospects.isEmpty()) return "Aucun prospect trouvé.";

            int successCount = 0, failCount = 0, cancelledCount = 0;

            for (Prospect p : prospects) {
                if (cancelRequested) {
                    cancelledCount = prospects.size() - successCount - failCount;
                    cancelRequested = false;
                    return String.format("🛑 Envoi annulé — ✅ %d envoyés, ❌ %d échecs, ⏸ %d non envoyés.",
                            successCount, failCount, cancelledCount);
                }

                try {
                    boolean sent = richEmailService.sendWithImage(
                            p.getEmail(),
                            request.getSubject(),
                            finalMessage,
                            request.getImageBase64(),
                            request.getImageName()
                    );

                    if (sent) {
                        p.setStatus("SENT");
                        p.setSentAt(java.time.LocalDateTime.now());
                        p.setGeneratedMessage(finalMessage);
                        successCount++;

                        // ← AJOUTER : Sauvegarder dans l'historique
                        EmailSendRecord record = new EmailSendRecord();
                        record.setProspectId(p.getId());
                        record.setProspectName(p.getName());
                        record.setProspectEmail(p.getEmail());
                        record.setMessage(finalMessage);
                        record.setSubject(request.getSubject());
                        record.setType("CUSTOM");
                        record.setSentAt(java.time.LocalDateTime.now());
                        recordRepo.save(record);

                    } else {
                        p.setStatus("FAILED");
                        failCount++;
                    }
                    repo.save(p);
                } catch (Exception e) {
                    failCount++;
                    e.printStackTrace();
                }
            }

            return String.format("✅ %d emails envoyés, ❌ %d échecs sur %d prospects.",
                    successCount, failCount, prospects.size());
        }



}