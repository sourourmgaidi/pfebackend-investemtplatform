  package tn.iset.investplatformpfe.Service;

import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Dto.ProspectReviewDto;
import tn.iset.investplatformpfe.Entity.EmailSendRecord;
import tn.iset.investplatformpfe.Entity.Prospect;
import tn.iset.investplatformpfe.Repository.EmailSendRecordRepository;
import tn.iset.investplatformpfe.Repository.ProspectRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmailAgentService {

    private final ProspectRepository repo;
    private final MessageService messageService;
    private final EmailService emailService;
    private final CustomMessageService customMessageService;
    private final EmailSendRecordRepository recordRepo;

    private volatile boolean cancelRequested = false;

    public EmailAgentService(ProspectRepository repo,
                             MessageService messageService,
                             EmailService emailService,
                             CustomMessageService customMessageService,
                             EmailSendRecordRepository recordRepo) {
        this.repo = repo;
        this.messageService = messageService;
        this.emailService = emailService;
        this.customMessageService = customMessageService;
        this.recordRepo = recordRepo;
    }

    public void cancelRun() { this.cancelRequested = true; }

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

    public String generate() {
        cancelRequested = false;
        List<Prospect> prospects = repo.findByStatus("PENDING");
        if (prospects.isEmpty()) return "Aucun prospect PENDING à traiter.";

        prospects.forEach(this::calculateScore);
        prospects.sort((a, b) -> b.getScore() - a.getScore());

        int count = 0;
        for (Prospect p : prospects) {
            if (cancelRequested) {
                cancelRequested = false;
                return " Génération annulée — " + count + " message(s) générés.";
            }
            Prospect fresh = repo.findById(p.getId()).orElse(null);
            if (fresh == null || !"PENDING".equals(fresh.getStatus())) continue;

            fresh.setStatus("PROCESSING");
            repo.save(fresh);

            try {
                String msg = messageService.generateHtml(fresh);
                String textVersion = stripHtml(msg);
                List<String> suggestions = customMessageService.generateSuggestions(textVersion);
                String suggestionsJson = toJson(suggestions);

                fresh.setGeneratedMessage(msg);
                fresh.setGeneratedSuggestions(suggestionsJson);
                fresh.setStatus("PENDING_REVIEW");
                repo.save(fresh);
                count++;
                Thread.sleep(500);
            } catch (Exception e) {
                e.printStackTrace();
                fresh.setStatus("FAILED");
                repo.save(fresh);
            }
        }
        return " " + count + " message(s) générés et en attente de validation.";
    }

    public String approveAndSend(Long prospectId, String finalMessage) {
        Prospect p = repo.findById(prospectId)
                .orElseThrow(() -> new RuntimeException("Prospect introuvable"));
        if (!"PENDING_REVIEW".equals(p.getStatus()))
            return " Ce prospect n'est pas en PENDING_REVIEW.";

        String msgToSend = (finalMessage != null && !finalMessage.isBlank())
                ? finalMessage : p.getGeneratedMessage();

        boolean sent = emailService.sendHtml(p.getEmail(), "Opportunity Proposal", msgToSend);
        p.setGeneratedMessage(msgToSend);
        p.setStatus(sent ? "SENT" : "FAILED");
        p.setSentAt(LocalDateTime.now());
        repo.save(p);

        // ── Sauvegarder dans l'historique ──────────────────────────
        if (sent) {
            EmailSendRecord record = new EmailSendRecord();
            record.setProspectId(p.getId());
            record.setProspectName(p.getName());
            record.setProspectEmail(p.getEmail());
            record.setMessage(msgToSend);
            record.setSubject("Opportunity Proposal");
            record.setType("AI");
            record.setSentAt(LocalDateTime.now());
            recordRepo.save(record);
        }

        return sent
                ? " Email envoyé à " + p.getEmail()
                : " Échec d'envoi pour " + p.getEmail();
    }

    public String reject(Long prospectId) {
        Prospect p = repo.findById(prospectId)
                .orElseThrow(() -> new RuntimeException("Prospect introuvable"));
        if (!"PENDING_REVIEW".equals(p.getStatus()))
            return " Ce prospect n'est pas en PENDING_REVIEW.";
        p.setStatus("PENDING");
        p.setGeneratedMessage(null);
        p.setGeneratedSuggestions(null);
        repo.save(p);
        return " Message refusé pour " + p.getName() + ". Remis en PENDING.";
    }

    public String approveAll() {
        List<Prospect> pending = repo.findByStatus("PENDING_REVIEW");
        if (pending.isEmpty()) return "Aucun message en attente de validation.";
        int success = 0, fail = 0;
        for (Prospect p : pending) {
            boolean sent = emailService.sendHtml(
                    p.getEmail(), "Opportunity Proposal", p.getGeneratedMessage());
            p.setStatus(sent ? "SENT" : "FAILED");
            p.setSentAt(LocalDateTime.now());
            repo.save(p);

            // ── Sauvegarder dans l'historique ──────────────────────
            if (sent) {
                EmailSendRecord record = new EmailSendRecord();
                record.setProspectId(p.getId());
                record.setProspectName(p.getName());
                record.setProspectEmail(p.getEmail());
                record.setMessage(p.getGeneratedMessage());
                record.setSubject("Opportunity Proposal");
                record.setType("AI");
                record.setSentAt(LocalDateTime.now());
                recordRepo.save(record);
            }

            if (sent) success++; else fail++;
        }
        return " " + success + " envoyés,  " + fail + " échecs.";
    }

    public List<ProspectReviewDto> getPendingReview() {
        return repo.findByStatus("PENDING_REVIEW").stream()
                .map(p -> new ProspectReviewDto(p, parseJson(p.getGeneratedSuggestions())))
                .collect(Collectors.toList());
    }

    public void run() { generate(); }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " ").trim();
    }

    private String toJson(List<String> list) {
        return "[" + list.stream()
                .map(s -> "\"" + s.replace("\\", "\\\\")
                        .replace("\"", "\\\"")
                        .replace("\n", "\\n")
                        .replace("\r", "") + "\"")
                .collect(Collectors.joining(",")) + "]";
    }

    private List<String> parseJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            json = json.trim();
            if (json.startsWith("[")) json = json.substring(1);
            if (json.endsWith("]")) json = json.substring(0, json.length() - 1);
            List<String> result = new ArrayList<>();
            int i = 0;
            while (i < json.length()) {
                int start = json.indexOf('"', i);
                if (start < 0) break;
                int end = start + 1;
                while (end < json.length()) {
                    if (json.charAt(end) == '\\') { end += 2; continue; }
                    if (json.charAt(end) == '"') break;
                    end++;
                }
                result.add(json.substring(start + 1, end)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\"));
                i = end + 1;
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<String> generateBroadcastSuggestions(String rawMessage) {
        String error = validateBroadcastMessage(rawMessage);
        if (error != null) throw new RuntimeException(error);
        return customMessageService.generateSuggestions(rawMessage);
    }

    public String validateBroadcastMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().length() < 10)
            return " Le message est trop court. Veuillez écrire un message plus complet.";
        return null;
    }

    public String broadcastToAllPending(String finalMessage) {
        List<Prospect> prospects = repo.findAll();
        if (prospects.isEmpty())
            return "Aucun prospect trouvé. Importez d'abord un fichier CSV.";
        int success = 0, fail = 0;
        for (Prospect p : prospects) {
            if (cancelRequested) {
                cancelRequested = false;
                return String.format("Envoi annulé —  %d envoyés,  %d échecs.", success, fail);
            }
            try {
                String personalized = personalizeForProspect(finalMessage, p);
                boolean sent = emailService.sendHtml(p.getEmail(), "Opportunity Proposal", personalized);
                p.setStatus(sent ? "SENT" : "FAILED");
                p.setGeneratedMessage(personalized);
                p.setSentAt(LocalDateTime.now());
                repo.save(p);

                // ── Sauvegarder dans l'historique ──────────────────
                if (sent) {
                    EmailSendRecord record = new EmailSendRecord();
                    record.setProspectId(p.getId());
                    record.setProspectName(p.getName());
                    record.setProspectEmail(p.getEmail());
                    record.setMessage(personalized);
                    record.setSubject("Opportunity Proposal");
                    record.setType("CUSTOM");
                    record.setSentAt(LocalDateTime.now());
                    recordRepo.save(record);
                }

                if (sent) success++; else fail++;
                Thread.sleep(300);
            } catch (Exception e) {
                e.printStackTrace();
                fail++;
            }
        }
        return String.format(" %d emails envoyés,  %d échecs sur %d prospects.", success, fail, prospects.size());
    }

    private String personalizeForProspect(String template, Prospect p) {
        if (template == null) return "";
        String fullName = p.getName() != null ? p.getName().trim() : "";
        String[] parts = fullName.split("\\s+", 2);
        String firstName = parts.length >= 1 ? parts[0] : fullName;
        String lastName  = parts.length >= 2 ? parts[1] : "";
        return template
                .replace("{{firstName}}", firstName)
                .replace("{{lastName}}", lastName)
                .replace("{{fullName}}", fullName)
                .replace("{{email}}", p.getEmail() != null ? p.getEmail() : "");
    }
}