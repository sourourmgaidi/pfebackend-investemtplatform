package tn.iset.investplatformpfe.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KonnectSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(KonnectSubscriptionService.class);

    @Value("${konnect.api.key:YOUR_API_KEY}")
    private String apiKey;

    @Value("${konnect.wallet.id:YOUR_WALLET_ID}")
    private String receiverWalletId;

    @Value("${konnect.subscription.success.link:http://localhost:4200/subscription/payment-success}")
    private String successLink;

    @Value("${konnect.subscription.fail.link:http://localhost:4200/subscription/payment-failed}")
    private String failLink;

    @Value("${konnect.webhook.url:http://localhost:8080/api/konnect/webhook}")
    private String webhookUrl;

    //  MODE TEST — mettre false en production
    @Value("${app.payment.test-mode:true}")
    private boolean testMode;

    private static final String KONNECT_API = "https://api.preprod.konnect.network/api/v2";

    private final RestTemplate restTemplate = new RestTemplate();

    // =========================================================
    // INITIER UN PAIEMENT
    // =========================================================
    public Map<String, Object> initiateSubscriptionPayment(double amountTND, String orderId) {
        log.info(" Initiation paiement ABONNEMENT Konnect - Montant: {} TND, OrderId: {}", amountTND, orderId);

        //  MODE TEST → simulation directe sans appel Konnect
        if (testMode) {
            log.warn(" MODE TEST ACTIVÉ → paiement simulé pour orderId: {}", orderId);
            return simulatedKonnectResponse(orderId);
        }

        String url = KONNECT_API + "/payments/init-payment";

        Map<String, Object> body = new HashMap<>();
        body.put("receiverWalletId", receiverWalletId);
        body.put("token", "TND");
        body.put("amount", (int) Math.round(amountTND * 1000));
        body.put("type", "immediate");
        body.put("description", "Abonnement mensuel - Accès Local Partners");
        body.put("acceptedPaymentMethods", List.of("wallet", "bank_card", "e-DINAR"));
        body.put("lifespan", 20);
        body.put("checkoutForm", true);
        body.put("addPaymentFeesToAmount", false);
        body.put("orderId", orderId);
        body.put("webhook", webhookUrl + "?orderId=" + orderId);
        body.put("theme", "light");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            log.info("Konnect réponse abonnement: {}", response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.error("❌ Erreur Konnect abonnement: {}", e.getMessage());
            throw new RuntimeException("Erreur Konnect: " + e.getMessage());
        }
    }

    // =========================================================
    // VÉRIFIER LE STATUT D'UN PAIEMENT
    // =========================================================
    public Map<String, Object> verifySubscriptionPayment(String paymentRef) {
        log.info("🔍 Vérification paiement ABONNEMENT Konnect - Ref: {}", paymentRef);

        // ✅ MODE TEST → succès automatique sans vérification Konnect
        if (testMode) {
            log.warn("🧪 MODE TEST ACTIVÉ → paiement accepté automatiquement");
            log.warn("⚠️  Mettre app.payment.test-mode=false en PRODUCTION !");
            return Map.of(
                    "simulated", true,
                    "testMode",  true,
                    "paymentRef", paymentRef,
                    "message",   "Paiement simulé en mode test"
            );
        }

        // ✅ MODE SIMULATION (paymentRef généré par simulatedKonnectResponse)
        if (paymentRef != null && paymentRef.startsWith("SIM_KONNECT_")) {
            log.info("🧪 Ref simulée détectée → paiement confirmé automatiquement");
            return Map.of("simulated", true);
        }

        // ✅ MODE PRODUCTION → vérification réelle Konnect
        String url = KONNECT_API + "/payments/" + paymentRef;

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-api-key", apiKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, Map.class);
            Map<String, Object> body = response.getBody();
            log.info("✅ Vérification Konnect réponse: {}", body);

            if (body != null && body.containsKey("payment")) {
                Map<String, Object> payment = (Map<String, Object>) body.get("payment");
                String status = (String) payment.get("status");
                log.info("📌 Statut Konnect brut reçu: '{}'", status);

                boolean isSuccess = status != null &&
                        (status.equalsIgnoreCase("completed") ||
                                status.equalsIgnoreCase("paid")      ||
                                status.equalsIgnoreCase("success"));

                Map<String, Object> result = new HashMap<>();
                result.put("status",       isSuccess ? "SUCCESS" : "PENDING");
                result.put("konnectStatus", status);
                result.put("paymentRef",    paymentRef);
                return Map.of("result", result);
            }

            log.warn("⚠️ Réponse Konnect sans champ 'payment': {}", body);
            return Map.of("result", Map.of("status", "PENDING"));

        } catch (Exception e) {
            log.error("❌ Erreur vérification Konnect - URL: {} - Erreur: {}", url, e.getMessage(), e);
            return Map.of("result", Map.of("status", "PENDING"));
        }
    }

    // =========================================================
    // RÉPONSE SIMULÉE
    // =========================================================
    private Map<String, Object> simulatedKonnectResponse(String orderId) {
        log.warn("⚠️ MODE SIMULATION — génération réponse simulée");

        String simulatedPaymentRef = "SIM_KONNECT_" + System.currentTimeMillis();
        String simulatedPayUrl = successLink
                + "?paymentId=" + orderId
                + "&paymentRef=" + simulatedPaymentRef;

        Map<String, Object> response = new HashMap<>();
        response.put("payUrl",      simulatedPayUrl);
        response.put("paymentRef",  simulatedPaymentRef);
        response.put("simulated",   true);
        return response;
    }
}