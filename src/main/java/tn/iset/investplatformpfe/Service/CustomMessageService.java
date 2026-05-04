package tn.iset.investplatformpfe.Service;

import org.springframework.stereotype.Service;

@Service
public class CustomMessageService {

    private final OllamaService ollamaService;

    public CustomMessageService(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
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
