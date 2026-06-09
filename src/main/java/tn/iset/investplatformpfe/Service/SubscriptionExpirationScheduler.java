package tn.iset.investplatformpfe.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionExpirationScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(SubscriptionExpirationScheduler.class);

    private final MessagerieService messagerieService;

    public SubscriptionExpirationScheduler(MessagerieService messagerieService) {
        this.messagerieService = messagerieService;
    }

    //  CORRIGÉ : tourne chaque jour à 09h00 (pas chaque minute)
    @Scheduled(cron = "0 0 9 * * *")
    public void checkExpiringSubscriptions() {
        log.info("🕘 [SCHEDULER] Checking subscriptions expiring in 2 days...");
        try {
            messagerieService.checkAndNotifyExpiringSubscriptions();
            log.info("✅ [SCHEDULER] Done.");
        } catch (Exception e) {
            log.error("❌ [SCHEDULER] Error: {}", e.getMessage(), e);
        }
    }
}