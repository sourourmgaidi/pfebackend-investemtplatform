package tn.iset.investplatformpfe.Service;


import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Entity.Prospect;
import tn.iset.investplatformpfe.Repository.ProspectRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmailAgentService {

    private final ProspectRepository repo;
    private final MessageService messageService;
    private final EmailService emailService;

    public EmailAgentService(ProspectRepository repo,
                             MessageService messageService,
                             EmailService emailService) {
        this.repo = repo;
        this.messageService = messageService;
        this.emailService = emailService;
    }

    public void calculateScore(Prospect p) {

        int score = 0;

        if ("HIGH".equalsIgnoreCase(p.getInterestLevel())) score += 50;
        if ("IT".equalsIgnoreCase(p.getCategory())) score += 30;
        if ("Tunis".equalsIgnoreCase(p.getCity())) score += 20;

        p.setScore(score);

        if (score >= 70) p.setPriority("HIGH");
        else if (score >= 40) p.setPriority("MEDIUM");
        else p.setPriority("LOW");
    }

    public void run() {
        System.out.println("🚀 Début de l'exécution de l'agent email");

        List<Prospect> prospects = repo.findByStatus("PENDING");
        System.out.println("📊 Nombre de prospects PENDING : " + prospects.size());

        if (prospects.isEmpty()) {
            System.out.println("⚠️ Aucun prospect à traiter.");
            return;
        }

        prospects.forEach(this::calculateScore);
        System.out.println("✅ Scores calculés.");

        prospects.sort((a, b) -> b.getScore() - a.getScore());
        System.out.println("📈 Tri effectué.");

        for (Prospect p : prospects) {
            System.out.println("--- Traitement de : " + p.getEmail() + " (" + p.getName() + ") ---");
            try {
                System.out.println("🤖 Appel à Ollama pour générer le message...");
                String msg = messageService.generate(p);
                System.out.println("📝 Message généré (longueur : " + (msg != null ? msg.length() : 0) + " caractères)");

                System.out.println("📧 Envoi de l'email à " + p.getEmail());
                boolean sent = emailService.send(p.getEmail(), msg);
                System.out.println("📬 Résultat de l'envoi : " + (sent ? "SUCCÈS" : "ÉCHEC"));

                p.setGeneratedMessage(msg);
                p.setStatus(sent ? "SENT" : "FAILED");
                p.setSentAt(LocalDateTime.now());

                repo.save(p);
                System.out.println("💾 Prospect mis à jour (status: " + p.getStatus() + ")");

                Thread.sleep(1500);
            } catch (Exception e) {
                System.err.println("❌ Exception pour " + p.getEmail() + " : " + e.getMessage());
                e.printStackTrace();
                p.setStatus("FAILED");
                repo.save(p);
            }
        }
        System.out.println("🏁 Fin de l'exécution de l'agent.");
    }
}

