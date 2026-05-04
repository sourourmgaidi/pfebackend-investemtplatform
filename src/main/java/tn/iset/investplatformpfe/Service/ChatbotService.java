package tn.iset.investplatformpfe.Service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tn.iset.investplatformpfe.Dto.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ChatbotService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ollama.model}")
    private String modelName;

    private static final String SYSTEM_PROMPT =
            "Tu es l'assistant virtuel officiel de ForsaTN, une plateforme tunisienne d'investissement.\n" +
                    "IMPORTANT : Tu DOIS répondre EXACTEMENT dans la même langue que celle utilisée par l'utilisateur.\n" +
                    "  - Si l'utilisateur écrit en français → réponds en français.\n" +
                    "  - Si l'utilisateur écrit en anglais → réponds en anglais.\n" +
                    "  - Si l'utilisateur écrit en arabe → réponds en arabe.\n" +
                    "  - Si la langue n'est pas claire, utilise le français par défaut.\n\n" +
                    "Ton rôle :\n" +
                    "- Présenter la plateforme ForsaTN et ses services\n" +
                    "- Orienter les visiteurs selon leur profil (investisseur étranger, partenaire économique, entreprise internationale, partenaire local, touriste)\n" +
                    "- Expliquer les secteurs porteurs en Tunisie : technologie, agriculture, énergie renouvelable, tourisme, industrie\n" +
                    "- Informer sur le processus d'investissement et l'accompagnement juridique disponible\n" +
                    "- Recommander de s'inscrire pour accéder aux projets et partenaires\n" +
                    "- Répondre aux questions sur la plateforme, les services, et le contexte économique tunisien\n\n" +
                    "Règles importantes :\n" +
                    "- Sois chaleureux, professionnel et concis (3-4 phrases maximum)\n" +
                    "- Ne génère jamais d'informations fausses\n" +
                    "- N'utilise pas de markdown complexe, juste du texte simple avec d'éventuels émojis";

    public String chat(List<ChatMessage> history) {
        String url = "http://localhost:11434/api/chat";

        List<Map<String, String>> messages = new ArrayList<>();

        // System prompt en premier
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));

        // Historique de la conversation
        for (ChatMessage msg : history) {
            messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }

        Map<String, Object> request = Map.of(
                "model", modelName,
                "messages", messages,
                "stream", false
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("message")) {
                Map<String, String> msg = (Map<String, String>) body.get("message");
                return msg.get("content");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "Je suis désolé, je rencontre une difficulté technique. Veuillez réessayer dans quelques instants. 🙏";
    }
}