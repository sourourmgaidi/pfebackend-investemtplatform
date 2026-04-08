package tn.iset.investplatformpfe.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.iset.investplatformpfe.Dto.*;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Repository.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserSessionService {

    private static final Logger log = LoggerFactory.getLogger(UserSessionService.class);

    private final UserSessionRepository sessionRepository;
    private final InvestorRepository investorRepository;
    private final TouristRepository touristRepository;
    private final EconomicPartnerRepository economicPartnerRepository;
    private final LocalPartnerRepository localPartnerRepository;
    private final InternationalCompanyRepository internationalCompanyRepository;
    private final AdminRepository adminRepository;

    public UserSessionService(
            UserSessionRepository sessionRepository,
            InvestorRepository investorRepository,
            TouristRepository touristRepository,
            EconomicPartnerRepository economicPartnerRepository,
            LocalPartnerRepository localPartnerRepository,
            InternationalCompanyRepository internationalCompanyRepository,
            AdminRepository adminRepository) {
        this.sessionRepository = sessionRepository;
        this.investorRepository = investorRepository;
        this.touristRepository = touristRepository;
        this.economicPartnerRepository = economicPartnerRepository;
        this.localPartnerRepository = localPartnerRepository;
        this.internationalCompanyRepository = internationalCompanyRepository;
        this.adminRepository = adminRepository;
    }

    // ========================================
    // DÉMARRER UNE SESSION
    // ========================================
    @Transactional
    public UserSession startSession(String email, String role) {
        Optional<UserSession> activeSession = sessionRepository.findByUserEmailAndLogoutTimeIsNull(email);

        if (activeSession.isPresent()) {
            UserSession existing = activeSession.get();

            if (existing.getLoginTime() != null &&
                    existing.getLoginTime().isBefore(LocalDateTime.now().minusHours(12))) {
                log.warn("⚠️ Session orpheline détectée pour {}, fermeture forcée", email);
                existing.setLogoutTime(LocalDateTime.now());
                existing.setDurationSeconds(
                        Duration.between(existing.getLoginTime(), LocalDateTime.now()).getSeconds()
                );
                sessionRepository.save(existing);
            } else {
                log.info("⚠️ Session déjà active pour {}", email);
                return existing;
            }
        }

        UserSession session = new UserSession();
        session.setUserEmail(email);
        session.setUserRole(role);
        session.setLoginTime(LocalDateTime.now());

        UserSession saved = sessionRepository.save(session);
        log.info("✅ Session démarrée pour {} à {}", email, saved.getLoginTime());
        return saved;
    }

    // ========================================
    // TERMINER UNE SESSION
    // ========================================
    @Transactional
    public UserSession endSession(String email) {
        Optional<UserSession> activeSession = sessionRepository.findByUserEmailAndLogoutTimeIsNull(email);

        if (activeSession.isEmpty()) {
            log.warn("⚠️ Aucune session active trouvée pour {}", email);
            return null;
        }

        UserSession session = activeSession.get();
        LocalDateTime logout = LocalDateTime.now();
        session.setLogoutTime(logout);

        LocalDateTime login = session.getLoginTime();
        if (login == null) {
            log.warn("⚠️ loginTime null pour {}, durée mise à 0", email);
            session.setDurationSeconds(0L);
        } else {
            long durationSeconds = Duration.between(login, logout).getSeconds();
            session.setDurationSeconds(durationSeconds);
            log.info("✅ Session terminée pour {} - Durée: {} min", email, durationSeconds / 60);
        }

        return sessionRepository.save(session);
    }

    // ========================================
    // OBTENIR LES STATISTIQUES D'UN UTILISATEUR
    // ========================================
    public UserTimeStatsDTO getUserStats(String email) {
        log.info("📊 Calcul des statistiques pour: {}", email);

        UserTimeStatsDTO stats = new UserTimeStatsDTO();
        stats.setUserEmail(email);
        stats.setUserFullName(getUserFullName(email));
        stats.setUserRole(getUserRole(email));
        stats.setProfilePhoto(getUserProfilePhoto(email));

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate startOfLastWeek = startOfWeek.minusWeeks(1);

        LocalDateTime thisWeekStart = startOfWeek.atStartOfDay();
        LocalDateTime thisWeekEnd = today.plusDays(1).atStartOfDay();
        LocalDateTime lastWeekStart = startOfLastWeek.atStartOfDay();
        LocalDateTime lastWeekEnd = startOfWeek.atStartOfDay();
        LocalDateTime sixWeeksAgo = today.minusWeeks(6).with(DayOfWeek.MONDAY).atStartOfDay();

        log.info("Période cette semaine: {} -> {}", thisWeekStart, thisWeekEnd);
        log.info("Période semaine dernière: {} -> {}", lastWeekStart, lastWeekEnd);

        Long thisWeekSeconds = sessionRepository.calculateTotalSecondsForPeriod(email, thisWeekStart, thisWeekEnd);
        Long lastWeekSeconds = sessionRepository.calculateTotalSecondsForPeriod(email, lastWeekStart, lastWeekEnd);

        log.info("Cette semaine: {} secondes, Semaine dernière: {} secondes", thisWeekSeconds, lastWeekSeconds);

        stats.setTotalSecondsThisWeek(thisWeekSeconds != null ? thisWeekSeconds : 0);
        stats.setTotalSecondsLastWeek(lastWeekSeconds != null ? lastWeekSeconds : 0);
        stats.setFormattedThisWeek(formatSeconds(stats.getTotalSecondsThisWeek()));
        stats.setFormattedLastWeek(formatSeconds(stats.getTotalSecondsLastWeek()));

        long difference = stats.getTotalSecondsThisWeek() - stats.getTotalSecondsLastWeek();
        stats.setDifferenceSeconds(difference);
        stats.setFormattedDifference(formatSeconds(Math.abs(difference)));
        stats.setNotificationMessage(generateNotificationMessage(stats));

        // ✅ FIX PRINCIPAL : passer thisWeekStart et today (pas thisWeekEnd)
        // pour que getDailyStatsForPeriod génère Lun → aujourd'hui
        stats.setDailySeconds(getDailyStatsForPeriod(email, thisWeekStart, thisWeekEnd));
        stats.setWeeklyStats(getWeeklyStatsForPeriod(email, sixWeeksAgo, thisWeekEnd));
        stats.setDailySessions(getDailySessionsForPeriod(email, thisWeekStart, thisWeekEnd));

        return stats;
    }

    // ========================================
    // OBTENIR TOUS LES UTILISATEURS (POUR ADMIN)
    // ========================================
    public List<UserTimeStatsDTO> getAllUsersStats() {
        List<UserTimeStatsDTO> allStats = new ArrayList<>();

        Set<String> allEmails = getAllUserEmails();

        for (String email : allEmails) {
            try {
                allStats.add(getUserStats(email));
            } catch (Exception e) {
                log.error("Erreur pour {}", email, e);
            }
        }

        allStats.sort((a, b) -> Long.compare(b.getTotalSecondsThisWeek(), a.getTotalSecondsThisWeek()));
        return allStats;
    }

    // ========================================
    // MÉTHODES PRIVÉES
    // ========================================

    private Set<String> getAllUserEmails() {
        Set<String> emails = new HashSet<>();
        investorRepository.findAll().forEach(u -> emails.add(u.getEmail()));
        touristRepository.findAll().forEach(u -> emails.add(u.getEmail()));
        economicPartnerRepository.findAll().forEach(u -> emails.add(u.getEmail()));
        localPartnerRepository.findAll().forEach(u -> emails.add(u.getEmail()));
        internationalCompanyRepository.findAll().forEach(u -> emails.add(u.getEmail()));
        adminRepository.findAll().forEach(u -> emails.add(u.getEmail()));
        return emails;
    }

    private String getUserFullName(String email) {
        Optional<Investor> investor = investorRepository.findByEmail(email);
        if (investor.isPresent()) return investor.get().getFirstName() + " " + investor.get().getLastName();

        Optional<Tourist> tourist = touristRepository.findByEmail(email);
        if (tourist.isPresent()) return tourist.get().getFirstName() + " " + tourist.get().getLastName();

        Optional<EconomicPartner> partner = economicPartnerRepository.findByEmail(email);
        if (partner.isPresent()) return partner.get().getFirstName() + " " + partner.get().getLastName();

        Optional<LocalPartner> localPartner = localPartnerRepository.findByEmail(email);
        if (localPartner.isPresent()) return localPartner.get().getFirstName() + " " + localPartner.get().getLastName();

        Optional<internationalcompany> company = internationalCompanyRepository.findByEmail(email);
        if (company.isPresent()) return company.get().getContactFirstName() + " " + company.get().getContactLastName();

        Optional<Admin> admin = adminRepository.findByEmail(email);
        if (admin.isPresent()) return admin.get().getFirstName() + " " + admin.get().getLastName();

        return email.split("@")[0];
    }

    private String getUserRole(String email) {
        if (investorRepository.existsByEmail(email)) return "INVESTOR";
        if (touristRepository.existsByEmail(email)) return "TOURIST";
        if (economicPartnerRepository.existsByEmail(email)) return "PARTNER";
        if (localPartnerRepository.existsByEmail(email)) return "LOCAL_PARTNER";
        if (internationalCompanyRepository.existsByEmail(email)) return "INTERNATIONAL_COMPANY";
        if (adminRepository.existsByEmail(email)) return "ADMIN";
        return "UNKNOWN";
    }

    private String getUserProfilePhoto(String email) {
        Optional<Investor> investor = investorRepository.findByEmail(email);
        if (investor.isPresent()) return investor.get().getProfilePicture();

        Optional<Tourist> tourist = touristRepository.findByEmail(email);
        if (tourist.isPresent()) return tourist.get().getProfilePhoto();

        Optional<EconomicPartner> partner = economicPartnerRepository.findByEmail(email);
        if (partner.isPresent()) return partner.get().getProfilePhoto();

        Optional<LocalPartner> localPartner = localPartnerRepository.findByEmail(email);
        if (localPartner.isPresent()) return localPartner.get().getProfilePhoto();

        Optional<internationalcompany> company = internationalCompanyRepository.findByEmail(email);
        if (company.isPresent()) return company.get().getProfilePicture();

        Optional<Admin> admin = adminRepository.findByEmail(email);
        if (admin.isPresent()) return admin.get().getProfilePhoto();

        return null;
    }

    // ========================================
    // ✅ FIX PRINCIPAL : getDailyStatsForPeriod
    // Génère TOUS les jours Lun → Dim (ou aujourd'hui)
    // avec 0 par défaut, puis écrase avec les vraies valeurs
    // ========================================
    private Map<String, Long> getDailyStatsForPeriod(String email, LocalDateTime start, LocalDateTime end) {
        Map<String, Long> dailyStats = new LinkedHashMap<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE dd/MM", Locale.FRENCH);

        // ✅ ÉTAPE 1 : Pré-remplir tous les jours Lun → Dim avec 0
        LocalDate current = start.toLocalDate();
        LocalDate today = LocalDate.now();
        // On génère jusqu'à la fin de la semaine (Dim) mais pas au-delà d'aujourd'hui
        // pour ne pas afficher des jours futurs avec 0
        LocalDate endDate = end.toLocalDate().minusDays(1); // thisWeekEnd est J+1, donc on recule d'un jour
        // Toujours afficher tous les 7 jours (Lun → Dim), y compris les jours futurs à 0
        LocalDate weekEnd = start.toLocalDate().plusDays(6); // Lundi + 6 = Dimanche

        while (!current.isAfter(weekEnd)) {
            dailyStats.put(current.format(formatter), 0L);
            current = current.plusDays(1);
        }

        // ✅ ÉTAPE 2 : Écraser avec les vraies valeurs depuis la DB
        List<Object[]> results = sessionRepository.getDailyStats(email, start, end);

        for (Object[] result : results) {
            try {
                java.sql.Date sqlDate = (java.sql.Date) result[0];
                LocalDate date = sqlDate.toLocalDate();

                Long seconds = 0L;
                if (result[1] != null) {
                    seconds = ((Number) result[1]).longValue();
                }

                // Écrase le 0 pré-rempli avec la vraie valeur
                dailyStats.put(date.format(formatter), seconds);
            } catch (Exception e) {
                log.error("Erreur lors du traitement des stats journalières", e);
            }
        }

        return dailyStats;
    }

    private List<WeeklyStatDTO> getWeeklyStatsForPeriod(String email, LocalDateTime start, LocalDateTime end) {
        List<WeeklyStatDTO> weeklyStats = new ArrayList<>();
        List<Object[]> results = sessionRepository.getWeeklyStats(email, start, end);

        for (Object[] result : results) {
            try {
                int year = 0;
                int week = 0;
                long seconds = 0L;

                if (result[0] != null) year = ((Number) result[0]).intValue();
                if (result[1] != null) week = ((Number) result[1]).intValue();
                if (result[2] != null) seconds = ((Number) result[2]).longValue();

                weeklyStats.add(new WeeklyStatDTO(week, year, seconds));
            } catch (Exception e) {
                log.error("Erreur lors du traitement des stats hebdomadaires", e);
            }
        }

        return weeklyStats;
    }

    private List<DailySessionDTO> getDailySessionsForPeriod(String email, LocalDateTime start, LocalDateTime end) {
        List<DailySessionDTO> dailySessions = new ArrayList<>();
        List<UserSession> sessions = sessionRepository.getSessionsByPeriod(email, start, end);

        Map<LocalDate, List<UserSession>> sessionsByDate = sessions.stream()
                .filter(s -> s.getDurationSeconds() != null)
                .collect(Collectors.groupingBy(s -> s.getLoginTime().toLocalDate()));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE dd MMMM", Locale.FRENCH);

        for (Map.Entry<LocalDate, List<UserSession>> entry : sessionsByDate.entrySet()) {
            DailySessionDTO daily = new DailySessionDTO();
            daily.setDate(entry.getKey());
            daily.setDayLabel(entry.getKey().format(formatter));

            long totalSeconds = 0;
            List<SessionDetailDTO> details = new ArrayList<>();

            for (UserSession session : entry.getValue()) {
                Long duration = session.getDurationSeconds() != null ? session.getDurationSeconds() : 0;
                totalSeconds += duration;

                SessionDetailDTO detail = new SessionDetailDTO();
                detail.setSessionId(session.getId());
                detail.setLoginTime(session.getLoginTime());
                detail.setLogoutTime(session.getLogoutTime());
                detail.setDurationSeconds(duration);
                detail.setFormattedDuration(formatSeconds(duration));
                details.add(detail);
            }

            daily.setTotalSeconds(totalSeconds);
            daily.setFormattedTime(formatSeconds(totalSeconds));
            daily.setSessions(details);
            dailySessions.add(daily);
        }

        dailySessions.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        return dailySessions;
    }

    private String generateNotificationMessage(UserTimeStatsDTO stats) {
        long diff = stats.getDifferenceSeconds();
        long diffMinutes = Math.abs(diff) / 60;
        long diffHours = diffMinutes / 60;

        String trend = diff > 0 ? "📈 augmentation" : (diff < 0 ? "📉 diminution" : "📊 stable");
        String timeUnit;

        if (diffHours > 0) {
            long remainMins = diffMinutes % 60;
            timeUnit = remainMins > 0
                    ? diffHours + "h " + remainMins + "min"
                    : diffHours + "h";
        } else {
            timeUnit = diffMinutes + " minute" + (diffMinutes > 1 ? "s" : "");
        }

        if (diff == 0) {
            return String.format("⏱️ Vous avez passé %s cette semaine (📊 identique à la semaine dernière)",
                    stats.getFormattedThisWeek());
        }

        return String.format("⏱️ Vous avez passé %s cette semaine (%s de %s par rapport à la semaine dernière)",
                stats.getFormattedThisWeek(),
                trend,
                timeUnit);
    }

    private String formatSeconds(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        if (hours > 0) {
            return hours + "h " + minutes + "min";
        } else if (minutes > 0 && remainingSeconds > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "") + " " + remainingSeconds + " seconde" + (remainingSeconds > 1 ? "s" : "");
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        } else {
            return seconds + " seconde" + (seconds > 1 ? "s" : "");
        }
    }

    // ========================================
    // NETTOYAGE AUTOMATIQUE DES SESSIONS
    // ========================================
    @Scheduled(cron = "0 0 */6 * * *")
    @Transactional
    public void cleanupOrphanedSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(12);

        List<UserSession> orphanedSessions = sessionRepository.findOrphanedSessions(cutoff);

        for (UserSession session : orphanedSessions) {
            LocalDateTime logout = LocalDateTime.now();
            session.setLogoutTime(logout);

            if (session.getLoginTime() != null) {
                session.setDurationSeconds(
                        Duration.between(session.getLoginTime(), logout).getSeconds()
                );
            } else {
                session.setDurationSeconds(0L);
            }

            sessionRepository.save(session);
            log.info("🧹 Session orpheline nettoyée pour {}", session.getUserEmail());
        }
    }
}