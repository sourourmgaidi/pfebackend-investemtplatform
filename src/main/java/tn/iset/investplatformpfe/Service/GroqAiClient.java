package tn.iset.investplatformpfe.Service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class GroqAiClient {

    @Value("${openai.api-key}")
    private String apiKey;

    @Value("${openai.model}")
    private String model;

    @Value("${openai.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public String chat(String systemPrompt, String userMessage) {
        String url = baseUrl + "/v1/chat/completions";

        Map<String, Object> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);

        Map<String, Object> userMsg = new LinkedHashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", List.of(systemMsg, userMsg));
        body.put("max_tokens", 1024);
        body.put("temperature", 0.7);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("choices")) {
                List<Map<String, Object>> choices =
                        (List<Map<String, Object>>) responseBody.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message =
                            (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            return "Erreur lors de la communication avec l'IA : " + e.getMessage();
        }
        return "Aucune réponse disponible.";
    }
}