package tn.iset.investplatformpfe.Service;

// src/main/java/tn/iset/investplatformpfe/Service/CustomMessageService.java
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

@Service
public class CustomMessageService {

    private final OllamaService ollamaService;

    // Mots considérés comme invalides / nonsensiques
    private static final List<String> INVALID_KEYWORDS = List.of(
            "rien", "hahah", "haha", "lol", "test", "aaa", "bbb", "xxx", "...", "????", "!!!!"
    );

    public CustomMessageService(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    // Valide le message brut avant tout traitement
    public String validate(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().length() < 10) {
            return "❌ Le message est trop court ou vide. Veuillez réécrire un message plus complet.";
        }
        String lower = rawMessage.toLowerCase();
        for (String keyword : INVALID_KEYWORDS) {
            if (lower.contains(keyword)) {
                return "❌ Le message contient un mot invalide (\"" + keyword + "\"). Veuillez réécrire un message clair et professionnel.";
            }
        }
        return null; // null = message valide
    }

    // Retourne 3 suggestions reformulées
    public List<String> generateSuggestions(String rawMessage) {
        String prompt = "You are an expert in professional communication. " +
                "Generate EXACTLY 3 different professional reformulations of the following message. " +
                "Rules:\n" +
                "- Respond ONLY in English\n" +
                "- Do NOT invent or add any information not present in the original\n" +
                "- Do NOT change the meaning, names, places, numbers, or dates\n" +
                "- Keep each version concise and professional\n" +
                "- Separate each version with the exact delimiter: ###\n" +
                "- Output ONLY the 3 versions separated by ###, no extra comments\n\n" +
                "Original message:\n" + rawMessage;

        String result = ollamaService.generateMessage(prompt);
        if (result == null || result.trim().isEmpty()) {
            return List.of(rawMessage); // fallback
        }

        String[] parts = result.split("###");
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(3)
                .collect(Collectors.toList());
    }

    public String reformulate(String rawMessage) {
        String prompt = "You are an expert in professional communication. " +
                "Rewrite the following message in a professional, clear, and convincing style. " +
                "IMPORTANT: You MUST respond ONLY in English, no matter the language of the original message. " +
                "Keep all factual information (names, places, numbers, dates) exactly as they appear. " +
                "Do not change the country, city, or any specific details. " +
                "Output ONLY the rewritten message, without any extra comments or explanations.\n\n" +
                "Original message:\n" + rawMessage;

        String result = ollamaService.generateMessage(prompt);
        if (result == null || result.trim().isEmpty()) return rawMessage;
        return result.trim();
    }
}