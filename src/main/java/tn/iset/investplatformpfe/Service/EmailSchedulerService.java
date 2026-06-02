package tn.iset.investplatformpfe.Service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Entity.EmailSchedule;
import tn.iset.investplatformpfe.Entity.EmailSendRecord;
import tn.iset.investplatformpfe.Entity.Prospect;
import tn.iset.investplatformpfe.Repository.EmailScheduleRepository;
import tn.iset.investplatformpfe.Repository.EmailSendRecordRepository;
import tn.iset.investplatformpfe.Repository.ProspectRepository;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmailSchedulerService {

    private final EmailScheduleRepository scheduleRepo;
    private final ProspectRepository prospectRepo;
    private final EmailService emailService;
    private final CustomEmailAgentService customEmailAgentService;
    private final EmailSendRecordRepository recordRepo;


    public EmailSchedulerService(EmailScheduleRepository scheduleRepo,
                                 ProspectRepository prospectRepo,
                                 EmailService emailService,
                                 CustomEmailAgentService customEmailAgentService,
                                 EmailSendRecordRepository recordRepo) {

        this.scheduleRepo = scheduleRepo;
        this.prospectRepo = prospectRepo;
        this.emailService = emailService;
        this.customEmailAgentService = customEmailAgentService;
        this.recordRepo = recordRepo;
    }

    // ── Vérifie toutes les 60 secondes les planifications dues ───────────────
    @Scheduled(fixedDelay = 60000)
    public void processScheduledEmails() {
        List<EmailSchedule> due = scheduleRepo
                .findByStatusAndScheduledAtBefore("PENDING", LocalDateTime.now());

        for (EmailSchedule schedule : due) {
            try {
                String result;
                if ("PROSPECT".equals(schedule.getType())) {
                    result = broadcastToAllProspects(schedule.getMessage(), schedule.getSubject());
                } else {
                    result = sendCustomToAll(schedule.getMessage(), schedule.getSubject());
                }
                schedule.setStatus("SENT");
                schedule.setResultMessage(result);
            } catch (Exception e) {
                schedule.setStatus("FAILED");
                schedule.setResultMessage("Erreur: " + e.getMessage());
            }
            schedule.setExecutedAt(LocalDateTime.now());
            scheduleRepo.save(schedule);
        }
    }


    private String broadcastToAllProspects(String message, String subject) {
        List<Prospect> prospects = prospectRepo.findAll();
        int success = 0, fail = 0;
        String subj = (subject != null && !subject.isBlank()) ? subject : "Opportunity Proposal";

        for (Prospect p : prospects) {
            String personalized = personalize(message, p);
            boolean sent = emailService.sendHtml(p.getEmail(), subj, personalized);

            if (sent) {
                success++;
                saveRecord(p, personalized, subj, "CUSTOM");
                p.setStatus("SENT");
                p.setSentAt(LocalDateTime.now());
                prospectRepo.save(p);
            } else {
                fail++;
            }
        }
        return String.format(" %d envoyés,  %d échecs sur %d prospects.", success, fail, prospects.size());
    }

    private String sendCustomToAll(String message, String subject) {
        List<Prospect> prospects = prospectRepo.findAll();
        int success = 0, fail = 0;
        String subj = (subject != null && !subject.isBlank()) ? subject : "Message";

        for (Prospect p : prospects) {
            boolean sent = emailService.sendHtml(p.getEmail(), subj, message);

            if (sent) {
                success++;
                saveRecord(p, message, subj, "CUSTOM");
                p.setStatus("SENT");
                p.setSentAt(LocalDateTime.now());
                prospectRepo.save(p);
            } else {
                fail++;
            }
        }
        return String.format(" %d envoyés,  %d échecs.", success, fail);
    }

    private String personalize(String template, Prospect p) {
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

    // ── CRUD pour le controller ───────────────────────────────────────────────
    public EmailSchedule create(EmailSchedule s) { return scheduleRepo.save(s); }
    public List<EmailSchedule> getAll() { return scheduleRepo.findAllByOrderByScheduledAtDesc(); }
    public void cancel(Long id) {
        scheduleRepo.findById(id).ifPresent(s -> {
            s.setStatus("CANCELLED");
            scheduleRepo.save(s);
        });
    }
    public void delete(Long id) { scheduleRepo.deleteById(id); }

    private void saveRecord(Prospect p, String message, String subject, String type) {
        EmailSendRecord record = new EmailSendRecord();
        record.setProspectId(p.getId());
        record.setProspectName(p.getName());
        record.setProspectEmail(p.getEmail());
        record.setMessage(message);
        record.setSubject(subject);
        record.setType(type);
        record.setSentAt(LocalDateTime.now());
        recordRepo.save(record);
    }

}