package tn.iset.investplatformpfe.Service;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Entity.Prospect;
/*OLLAMA EMAIL MESSAGE SERVICE */
@Service
public class MessageService {

    private final OllamaService ollamaService;

    @Value("${app.platform.url:https://votre-plateforme.com}")
    private String platformUrl;

    public MessageService(OllamaService ollamaService) {
        this.ollamaService = ollamaService;
    }

    public String generate(Prospect p) {
        // 1. Construire le nom du destinataire et de l'entreprise
        String recipientName = p.getName();
        String companyName = p.getCompany();

        // 2. Le "template" d'email que l'IA doit suivre à la lettre
        String emailTemplate =
                "**Objet : Accédez à une nouvelle plateforme d'investissement**\n\n" +
                        "Bonjour " + recipientName + ",\n\n" +
                        "La gestion des opportunités d'investissement est souvent dispersée et difficile à suivre efficacement.\n\n" +
                        "Nous avons développé une plateforme qui centralise l'ensemble du processus afin de le rendre plus simple, structuré et exploitable en temps réel.\n\n" +
                        "**Ce que permet la solution :**\n" +
                        "* Centralisation des prospects et opportunités\n" +
                        "* Suivi clair et organisé des investissements\n" +
                        "* Interface rapide et intuitive\n\n" +
                        "**Découvrez la plateforme dès maintenant :**\n" +
                        "➡️ **[" + platformUrl + "](" + platformUrl + ")**\n\n" +
                        "Je reste disponible pour toute information complémentaire.\n\n" +
                        "Cordialement,\n" +
                        "L'équipe InvestPlatform\n\n" +
                        "---\n" +
                        "PS : Si vous avez des questions, n'hésitez pas à répondre directement à cet email.";

        // On demande à l'IA d'utiliser strictement ce template.
        // On peut lui demander de ne faire que de très légères adaptations.
        String prompt = "Tu es un assistant qui génère des emails professionnels. " +
                "Tu dois ABSOLUMENT suivre le template ci-dessous à la lettre, sans en changer la structure ni le contenu, " +
                "sauf pour insérer les informations du prospect aux bons endroits. " +
                "Le template à utiliser est :\n\n" +
                emailTemplate;

        // Appel à Ollama pour générer le message
        String generatedMessage = ollamaService.generateMessage(prompt);

        // Petite sécurité : si l'IA ne renvoie rien, on renvoie le template de base
        if (generatedMessage == null || generatedMessage.trim().isEmpty()) {
            return emailTemplate;
        }

        return generatedMessage;
    }
}