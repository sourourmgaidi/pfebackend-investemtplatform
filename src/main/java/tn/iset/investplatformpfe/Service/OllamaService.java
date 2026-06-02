package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class OllamaService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ollama.model}")
    private String modelName;

    public String generateMessage(String prompt) {
        String url = "http://localhost:11434/api/generate";

        Map<String, Object> request = Map.of(
                "model", modelName,   // ← utilise la valeur de application.properties
                "prompt", prompt,
                "stream", false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        return (String) response.getBody().get("response");
    }
}