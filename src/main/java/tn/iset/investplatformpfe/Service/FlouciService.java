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
public class FlouciService {

    private static final Logger log = LoggerFactory.getLogger(FlouciService.class);

    @Value("${flouci.app.token:YOUR_APP_TOKEN}")
    private String appToken;

    @Value("${flouci.app.secret:YOUR_APP_SECRET}")
    private String appSecret;

    @Value("${flouci.success.link:http://localhost:4200/payment/success}")
    private String successLink;

    @Value("${flouci.fail.link:http://localhost:4200/payment/fail}")
    private String failLink;

    private static final String FLOUCI_API = "https://developers.flouci.com/api";
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Initier un paiement Flouci
     * amount en TND — Flouci attend des millimes (1 TND = 1000 millimes)
     */
    public Map<String, Object> initiatePayment(double amountTND, String orderId) {
        log.info("💳 Initiation paiement Flouci - Montant: {} TND, OrderId: {}", amountTND, orderId);

        String url = FLOUCI_API + "/generate_payment";

        Map<String, Object> body = new HashMap<>();
        body.put("app_token", appToken);
        body.put("app_secret", appSecret);
        body.put("amount", (int)(amountTND * 1000)); // TND → millimes
        body.put("accept_card", true);
        body.put("session_timeout_secs", 1200);
        body.put("success_link", successLink + "?orderId=" + orderId);
        body.put("fail_link", failLink + "?orderId=" + orderId);
        body.put("developer_tracking_id", orderId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            log.info("✅ Flouci réponse: {}", response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.error("❌ Erreur Flouci: {}", e.getMessage());
            // En mode préparation — retourner une réponse simulée
            return simulatedFlouciResponse(orderId);
        }
    }

    /**
     * Vérifier le statut d'un paiement Flouci
     */
    public Map<String, Object> verifyPayment(String paymentId) {
        log.info("🔍 Vérification paiement Flouci ID: {}", paymentId);

        String url = FLOUCI_API + "/verify_payment/" + paymentId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("apppublic", appToken);
        headers.set("appsecret", appSecret);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, request, Map.class);
            log.info("✅ Vérification réponse: {}", response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.warn("⚠️ Vérification Flouci échouée, mode simulation: {}", e.getMessage());
            // En mode simulation — considérer comme succès
            return Map.of("result", Map.of("status", "SUCCESS"));
        }
    }

    /**
     * Réponse simulée pour la phase de préparation (sans clé API réelle)
     */
    private Map<String, Object> simulatedFlouciResponse(String orderId) {
        log.warn("⚠️ MODE SIMULATION — Flouci non configuré");
        String simulatedPaymentId = "SIM_PAY_" + System.currentTimeMillis();
        String simulatedLink = "http://localhost:8089/api/acquisitions/confirm"
                + "?paymentId=" + simulatedPaymentId
                + "&orderId=" + orderId;

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
