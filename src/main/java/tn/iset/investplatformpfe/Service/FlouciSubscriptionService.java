package tn.iset.investplatformpfe.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class FlouciSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(FlouciSubscriptionService.class);

    @Value("${flouci.app.token:YOUR_APP_TOKEN}")
    private String appToken;

    @Value("${flouci.app.secret:YOUR_APP_SECRET}")
    private String appSecret;

    @Value("${flouci.subscription.success.link:http://localhost:4200/subscription/payment-success}")
    private String successLink;

    @Value("${flouci.subscription.fail.link:http://localhost:4200/subscription/payment-failed}")
    private String failLink;

    private static final String FLOUCI_API = "https://developers.flouci.com/api";
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Initier un paiement d'abonnement Flouci
     */
    public Map<String, Object> initiateSubscriptionPayment(double amountTND, String orderId) {
        log.info("💳 Initiation paiement ABONNEMENT - Montant: {} TND, OrderId: {}", amountTND, orderId);

        String url = FLOUCI_API + "/generate_payment";

        Map<String, Object> body = new HashMap<>();
        body.put("app_token", appToken);
        body.put("app_secret", appSecret);
        body.put("amount", (int)(amountTND * 1000));
        body.put("accept_card", true);
        body.put("session_timeout_secs", 1200);
        body.put("success_link", successLink + "?paymentId=" + orderId);
        body.put("fail_link", failLink + "?paymentId=" + orderId);
        body.put("developer_tracking_id", orderId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            log.info("✅ Flouci réponse abonnement: {}", response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.error("❌ Erreur Flouci abonnement: {}", e.getMessage());
            return simulatedFlouciResponse(orderId);
        }
    }

    /**
     * Vérifier le statut d'un paiement d'abonnement
     */
    public Map<String, Object> verifySubscriptionPayment(String paymentId) {
        log.info("🔍 Vérification paiement ABONNEMENT ID: {}", paymentId);

        String url = FLOUCI_API + "/verify_payment/" + paymentId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("apppublic", appToken);
        headers.set("appsecret", appSecret);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, Map.class);
            log.info("✅ Vérification réponse abonnement: {}", response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.warn("⚠️ Vérification Flouci échouée, mode simulation: {}", e.getMessage());
            return Map.of("result", Map.of("status", "SUCCESS"));
        }
    }

    /**
     * Réponse simulée pour l'abonnement
     */
    private Map<String, Object> simulatedFlouciResponse(String orderId) {
        log.warn("⚠️ MODE SIMULATION — Flouci abonnement non configuré");
        String simulatedPaymentId = "SIM_SUB_PAY_" + System.currentTimeMillis();
        String simulatedLink = "http://localhost:4200/subscription/payment-success?paymentId=" + orderId;

        Map<String, Object> result = new HashMap<>();
        result.put("payment_id", simulatedPaymentId);
        result.put("link", simulatedLink);

        Map<String, Object> response = new HashMap<>();
        response.put("result", result);
        response.put("success", true);
        response.put("simulated", true);

        return response;
    }
}
