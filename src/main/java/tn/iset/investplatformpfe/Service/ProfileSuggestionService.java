package tn.iset.investplatformpfe.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Dto.ProfileSuggestionResponse;
import tn.iset.investplatformpfe.Repository.*;

import tn.iset.investplatformpfe.Entity.Investor;
import tn.iset.investplatformpfe.Entity.Tourist;
import tn.iset.investplatformpfe.Entity.EconomicPartner;
import tn.iset.investplatformpfe.Entity.internationalcompany;
import tn.iset.investplatformpfe.Entity.InvestmentService;
import tn.iset.investplatformpfe.Entity.CollaborationService;
import tn.iset.investplatformpfe.Entity.TouristService;
import tn.iset.investplatformpfe.Entity.ServiceStatus;

import java.util.*;

@Service
public class ProfileSuggestionService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final GroqAiClient groqAiClient;
    private final InvestorRepository investorRepository;
    private final TouristRepository touristRepository;
    private final EconomicPartnerRepository economicPartnerRepository;
    private final InternationalCompanyRepository internationalCompanyRepository;
    private final LocalPartnerRepository localPartnerRepository;
    private final InvestmentServiceRepository investmentServiceRepository;
    private final CollaborationServiceRepository collaborationServiceRepository;
    private final TouristServiceRepository touristServiceRepository;

    public ProfileSuggestionService(
            GroqAiClient groqAiClient,
            InvestorRepository investorRepository,
            TouristRepository touristRepository,
            EconomicPartnerRepository economicPartnerRepository,
            InternationalCompanyRepository internationalCompanyRepository,
            LocalPartnerRepository localPartnerRepository,
            InvestmentServiceRepository investmentServiceRepository,
            CollaborationServiceRepository collaborationServiceRepository,
            TouristServiceRepository touristServiceRepository) {
        this.groqAiClient = groqAiClient;
        this.investorRepository = investorRepository;
        this.touristRepository = touristRepository;
        this.economicPartnerRepository = economicPartnerRepository;
        this.internationalCompanyRepository = internationalCompanyRepository;
        this.localPartnerRepository = localPartnerRepository;
        this.investmentServiceRepository = investmentServiceRepository;
        this.collaborationServiceRepository = collaborationServiceRepository;
        this.touristServiceRepository = touristServiceRepository;
    }

    // =========================================================
    // MÉTHODE PRINCIPALE
    // =========================================================
    public ProfileSuggestionResponse generateSuggestion(String email, String role, String question) {
        return switch (role.toUpperCase()) {
            case "INVESTOR"              -> handleInvestor(email, question);
            case "TOURIST"               -> handleTourist(email, question);
            case "PARTNER"               -> handleEconomicPartner(email, question);
            case "INTERNATIONAL_COMPANY" -> handleInternationalCompany(email, question);
            default -> throw new RuntimeException("Rôle non supporté : " + role);
        };
    }

    // =========================================================
    // INVESTOR → services d'investissement
    // =========================================================
    private ProfileSuggestionResponse handleInvestor(String email, String question) {
        Investor inv = investorRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Investor not found: " + email));

        List<InvestmentService> allServices =
                investmentServiceRepository.findByStatus(ServiceStatus.APPROVED);

        // LOG pour vérifier que les services existent
        System.out.println("=== [INVESTOR] Services APPROVED trouvés : " + allServices.size() + " ===");
        allServices.forEach(s -> System.out.println("  ID:" + s.getId() + " | " + s.getTitle()));

        String catalogue = buildInvestmentCatalogue(allServices);

        String systemPrompt = """
                Respond only in English
                Tu es un agent expert en investissement spécialisé sur la plateforme invest-platform Tunisie.
                Tu dois analyser le profil de l'investisseur et les services disponibles.
                IMPORTANT : Réponds UNIQUEMENT avec un objet JSON valide, sans aucun texte avant ou après, sans balises markdown, sans ```json.
                Format EXACT attendu :
                {"analysis":"ton analyse personnalisée en 2-3 phrases en anglais","recommendedIds":[id1,id2,id3],"reasons":["raison 1","raison 2","raison 3"]}
                Sélectionne les 3 services les plus pertinents selon le secteur d'activité et le profil.
                Si moins de 3 services sont disponibles, retourne ceux qui existent.
                """;

        String userPrompt = String.format("""
                Profil investisseur :
                - Nom : %s %s
                - Société : %s
                - Pays : %s
                - Secteur d'activité : %s
                - Nationalité : %s

                Question : %s

                Services d'investissement disponibles (utilise les IDs exacts listés ci-dessous) :
                %s
                """,
                inv.getFirstName(), inv.getLastName(),
                inv.getCompany() != null ? inv.getCompany() : "N/A",
                inv.getOriginCountry() != null ? inv.getOriginCountry() : "N/A",
                inv.getActivitySector() != null ? inv.getActivitySector().name() : "Non précisé",
                inv.getNationality() != null ? inv.getNationality() : "Non précisée",
                question,
                catalogue
        );

        String aiResponse = groqAiClient.chat(systemPrompt, userPrompt);
        System.out.println("=== [INVESTOR] Réponse IA brute ===\n" + aiResponse + "\n===");

        return buildInvestorResponse(aiResponse, allServices,
                "INVESTOR", inv.getFirstName() + " " + inv.getLastName());
    }

    // =========================================================
    // TOURIST → services touristiques
    // =========================================================
    private ProfileSuggestionResponse handleTourist(String email, String question) {
        Tourist tourist = touristRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Tourist not found: " + email));

        List<TouristService> allServices =
                touristServiceRepository.findByStatus(ServiceStatus.APPROVED);

        System.out.println("=== [TOURIST] Services APPROVED trouvés : " + allServices.size() + " ===");
        allServices.forEach(s -> System.out.println("  ID:" + s.getId() + " | " + s.getName()));

        String catalogue = buildTouristCatalogue(allServices);

        String systemPrompt = """
                Respond only in English
                Tu es un guide touristique expert de la Tunisie sur la plateforme invest-platform.
                Analyse le profil du touriste et les services disponibles.
                IMPORTANT : Réponds UNIQUEMENT avec un objet JSON valide, sans aucun texte avant ou après, sans balises markdown, sans ```json.
                Format EXACT attendu :
                {"analysis":"recommandation personnalisée en 2-3 phrases en anglais","recommendedIds":[id1,id2,id3],"reasons":["raison 1","raison 2","raison 3"]}
                Sélectionne les 3 services les plus adaptés selon la nationalité et les préférences.
                Si moins de 3 services sont disponibles, retourne ceux qui existent.
                """;

        String userPrompt = String.format("""
                Profil touriste :
                - Nom : %s %s
                - Nationalité : %s

                Question : %s

                Services touristiques disponibles (utilise les IDs exacts listés ci-dessous) :
                %s
                """,
                tourist.getFirstName(), tourist.getLastName(),
                tourist.getNationality() != null ? tourist.getNationality() : "Non précisée",
                question,
                catalogue
        );

        String aiResponse = groqAiClient.chat(systemPrompt, userPrompt);
        System.out.println("=== [TOURIST] Réponse IA brute ===\n" + aiResponse + "\n===");

        return buildTouristResponse(aiResponse, allServices,
                "TOURIST", tourist.getFirstName() + " " + tourist.getLastName());
    }

    // =========================================================
    // ECONOMIC PARTNER → services de collaboration
    // =========================================================
    private ProfileSuggestionResponse handleEconomicPartner(String email, String question) {
        EconomicPartner partner = economicPartnerRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Partner not found: " + email));

        List<CollaborationService> allServices =
                collaborationServiceRepository.findByStatus(ServiceStatus.APPROVED);

        System.out.println("=== [PARTNER] Services APPROVED trouvés : " + allServices.size() + " ===");
        allServices.forEach(s -> System.out.println("  ID:" + s.getId() + " | " + s.getName()));

        String catalogue = buildCollaborationCatalogue(allServices);

        String systemPrompt = """
                Respond only in English
                Tu es un expert en partenariats économiques sur la plateforme invest-platform Tunisie.
                Analyse le profil du partenaire économique et les opportunités de collaboration disponibles.
                IMPORTANT : Réponds UNIQUEMENT avec un objet JSON valide, sans aucun texte avant ou après, sans balises markdown, sans ```json.
                Format EXACT attendu :
                {"analysis":"analyse personnalisée en 2-3 phrases en anglais","recommendedIds":[id1,id2,id3],"reasons":["raison 1","raison 2","raison 3"]}
                Sélectionne les 3 collaborations les plus pertinentes selon le secteur d'activité.
                Si moins de 3 services sont disponibles, retourne ceux qui existent.
                """;

        String userPrompt = String.format("""
                Profil partenaire économique :
                - Nom : %s %s
                - Pays : %s
                - Secteur d'activité : %s

                Question : %s

                Services de collaboration disponibles (utilise les IDs exacts listés ci-dessous) :
                %s
                """,
                partner.getFirstName(), partner.getLastName(),
                partner.getCountryOfOrigin() != null ? partner.getCountryOfOrigin() : "Non précisé",
                partner.getBusinessSector() != null ? partner.getBusinessSector().name() : "Non précisé",
                question,
                catalogue
        );

        String aiResponse = groqAiClient.chat(systemPrompt, userPrompt);
        System.out.println("=== [PARTNER] Réponse IA brute ===\n" + aiResponse + "\n===");

        return buildCollaborationResponse(aiResponse, allServices,
                "PARTNER", partner.getFirstName() + " " + partner.getLastName());
    }

    // =========================================================
    // INTERNATIONAL COMPANY → investissement + collaboration
    // =========================================================
    private ProfileSuggestionResponse handleInternationalCompany(String email, String question) {
        internationalcompany company = internationalCompanyRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Company not found: " + email));

        List<InvestmentService> investServices =
                investmentServiceRepository.findByStatus(ServiceStatus.APPROVED);
        List<CollaborationService> colabServices =
                collaborationServiceRepository.findByStatus(ServiceStatus.APPROVED);

        System.out.println("=== [INTL_COMPANY] InvestServices: " + investServices.size()
                + " | ColabServices: " + colabServices.size() + " ===");

        String investCatalogue = buildInvestmentCatalogue(investServices);
        String colabCatalogue  = buildCollaborationCatalogue(colabServices);

        String systemPrompt = """
                Respond only in English
                Tu es un consultant en développement international sur la plateforme invest-platform Tunisie.
                Analyse le profil de l'entreprise internationale et les services disponibles.
                IMPORTANT : Réponds UNIQUEMENT avec un objet JSON valide, sans aucun texte avant ou après, sans balises markdown, sans ```json.
                Format EXACT attendu :
                {"analysis":"analyse stratégique en 2-3 phrases en anglais","recommendedInvestmentIds":[id1,id2,id3],"investmentReasons":["raison 1","raison 2","raison 3"],"recommendedCollaborationIds":[id1,id2,id3],"collaborationReasons":["raison 1","raison 2","raison 3"]}
                Sélectionne les 3 meilleurs services d'investissement ET les 3 meilleures collaborations.
                Si moins de 3 services sont disponibles pour une catégorie, retourne ceux qui existent.
                """;

        String userPrompt = String.format("""
                Profil entreprise internationale :
                - Société : %s
                - Contact : %s %s
                - Pays : %s
                - Secteur d'activité : %s

                Question : %s

                === SERVICES D'INVESTISSEMENT DISPONIBLES ===
                %s

                === SERVICES DE COLLABORATION DISPONIBLES ===
                %s
                """,
                company.getCompanyName(),
                company.getContactFirstName(), company.getContactLastName(),
                company.getOriginCountry() != null ? company.getOriginCountry() : "N/A",
                company.getActivitySector() != null ? company.getActivitySector().name() : "Non précisé",
                question,
                investCatalogue,
                colabCatalogue
        );

        String aiResponse = groqAiClient.chat(systemPrompt, userPrompt);
        System.out.println("=== [INTL_COMPANY] Réponse IA brute ===\n" + aiResponse + "\n===");

        return buildCompanyResponse(aiResponse, investServices, colabServices,
                company.getCompanyName());
    }

    // =========================================================
    // BUILDERS DE CATALOGUES
    // =========================================================
    private String buildInvestmentCatalogue(List<InvestmentService> services) {
        if (services.isEmpty()) return "Aucun service disponible.";
        StringBuilder sb = new StringBuilder();
        for (InvestmentService s : services) {
            sb.append(String.format(
                    "[ID:%d] %s | Secteur: %s | Montant total: %s | Montant min: %s | Zone: %s | Durée: %s\n",
                    s.getId(),
                    s.getTitle() != null ? s.getTitle() : "N/A",
                    s.getEconomicSector() != null ? s.getEconomicSector().getName() : "N/A",
                    s.getTotalAmount() != null ? s.getTotalAmount() + " TND" : "N/A",
                    s.getMinimumAmount() != null ? s.getMinimumAmount() + " TND" : "N/A",
                    s.getZone() != null ? s.getZone() : "N/A",
                    s.getProjectDuration() != null ? s.getProjectDuration() : "N/A"
            ));
        }
        return sb.toString();
    }

    private String buildCollaborationCatalogue(List<CollaborationService> services) {
        if (services.isEmpty()) return "Aucun service disponible.";
        StringBuilder sb = new StringBuilder();
        for (CollaborationService s : services) {
            sb.append(String.format(
                    "[ID:%d] %s | Type: %s | Domaine: %s | Budget: %s | Durée: %s | Compétences: %s\n",
                    s.getId(),
                    s.getName() != null ? s.getName() : "N/A",
                    s.getCollaborationType() != null ? s.getCollaborationType().name() : "N/A",
                    s.getActivityDomain() != null ? s.getActivityDomain().name() : "N/A",
                    s.getRequestedBudget() != null ? s.getRequestedBudget() + " TND" : "N/A",
                    s.getCollaborationDuration() != null ? s.getCollaborationDuration() : "N/A",
                    s.getRequiredSkills() != null ? String.join(", ", s.getRequiredSkills()) : "N/A"
            ));
        }
        return sb.toString();
    }

    private String buildTouristCatalogue(List<TouristService> services) {
        if (services.isEmpty()) return "Aucun service disponible.";
        StringBuilder sb = new StringBuilder();
        for (TouristService s : services) {
            sb.append(String.format(
                    "[ID:%d] %s | Catégorie: %s | Prix: %s TND | Durée: %sh | Capacité: %s | Public: %s\n",
                    s.getId(),
                    s.getName() != null ? s.getName() : "N/A",
                    s.getCategory() != null ? s.getCategory().name() : "N/A",
                    s.getPrice() != null ? s.getPrice() : "N/A",
                    s.getDurationHours() != null ? s.getDurationHours() : "N/A",
                    s.getMaxCapacity() != null ? s.getMaxCapacity() : "N/A",
                    s.getTargetAudience() != null ? s.getTargetAudience().name() : "N/A"
            ));
        }
        return sb.toString();
    }

    // =========================================================
    // PARSERS JSON AVEC JACKSON (robuste)
    // =========================================================

    /**
     * Extrait le bloc JSON { } de la réponse brute de l'IA
     * (qui peut contenir du texte parasite avant/après ou des backticks ```json)
     */
    private JsonNode parseAiJson(String aiResponse) {
        try {
            if (aiResponse == null || aiResponse.isBlank()) {
                System.err.println("[PARSE] Réponse IA vide !");
                return objectMapper.createObjectNode();
            }
            // Supprimer les balises ```json ... ``` si présentes
            String cleaned = aiResponse
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            // Extraire le premier bloc { ... }
            int start = cleaned.indexOf("{");
            int end   = cleaned.lastIndexOf("}");

            if (start == -1 || end == -1 || end <= start) {
                System.err.println("[PARSE] Aucun bloc JSON trouvé dans : " + cleaned);
                return objectMapper.createObjectNode();
            }

            String jsonBlock = cleaned.substring(start, end + 1);
            JsonNode node = objectMapper.readTree(jsonBlock);
            System.out.println("[PARSE] JSON parsé avec succès : " + node.toString());
            return node;

        } catch (Exception e) {
            System.err.println("[PARSE] Erreur Jackson : " + e.getMessage());
            System.err.println("[PARSE] Réponse brute : " + aiResponse);
            return objectMapper.createObjectNode();
        }
    }

    private String getTextField(JsonNode node, String field) {
        JsonNode val = node.get(field);
        if (val == null || val.isNull()) return "Analyse non disponible.";
        return val.asText("Analyse non disponible.");
    }

    private List<Long> getIdsList(JsonNode node, String field) {
        List<Long> ids = new ArrayList<>();
        JsonNode arr = node.get(field);
        if (arr != null && arr.isArray()) {
            arr.forEach(n -> {
                if (!n.isNull()) ids.add(n.asLong());
            });
        }
        return ids;
    }

    private List<String> getStringList(JsonNode node, String field) {
        List<String> list = new ArrayList<>();
        JsonNode arr = node.get(field);
        if (arr != null && arr.isArray()) {
            arr.forEach(n -> {
                if (!n.isNull()) list.add(n.asText());
            });
        }
        return list;
    }

    // =========================================================
    // BUILDERS DE RÉPONSE
    // =========================================================

    private ProfileSuggestionResponse buildInvestorResponse(
            String aiJson,
            List<InvestmentService> allServices,
            String role,
            String userName) {

        JsonNode node    = parseAiJson(aiJson);
        String analysis  = getTextField(node, "analysis");
        List<Long> ids   = getIdsList(node, "recommendedIds");
        List<String> reasons = getStringList(node, "reasons");

        System.out.println("[INVESTOR] IDs recommandés par l'IA : " + ids);

        List<Map<String, Object>> recommended = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            final long id  = ids.get(i);
            final int  idx = i;
            allServices.stream()
                    .filter(s -> s.getId() != null && s.getId() == id)
                    .findFirst()
                    .ifPresent(s -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", s.getId());
                        item.put("title", s.getTitle());
                        item.put("name", s.getName());
                        item.put("description", s.getDescription());
                        item.put("totalAmount", s.getTotalAmount());
                        item.put("minimumAmount", s.getMinimumAmount());
                        item.put("zone", s.getZone());
                        item.put("projectDuration", s.getProjectDuration());
                        item.put("sector", s.getEconomicSector() != null
                                ? s.getEconomicSector().getName() : null);
                        item.put("serviceType", "INVESTMENT");
                        item.put("reason", idx < reasons.size()
                                ? reasons.get(idx) : "Recommandé par l'IA");
                        item.put("score", Math.max(95 - (idx * 10), 65)); // ← AJOUTER
                        recommended.add(item);
                    });
        }

        return new ProfileSuggestionResponse(analysis, role, userName, recommended);
    }

    private ProfileSuggestionResponse buildTouristResponse(
            String aiJson,
            List<TouristService> allServices,
            String role,
            String userName) {

        JsonNode node    = parseAiJson(aiJson);
        String analysis  = getTextField(node, "analysis");
        List<Long> ids   = getIdsList(node, "recommendedIds");
        List<String> reasons = getStringList(node, "reasons");

        System.out.println("[TOURIST] IDs recommandés par l'IA : " + ids);

        List<Map<String, Object>> recommended = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            final long id  = ids.get(i);
            final int  idx = i;
            allServices.stream()
                    .filter(s -> s.getId() != null && s.getId() == id)
                    .findFirst()
                    .ifPresent(s -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", s.getId());
                        item.put("name", s.getName());
                        item.put("description", s.getDescription());
                        item.put("category", s.getCategory() != null ? s.getCategory().name() : null);
                        item.put("price", s.getPrice());
                        item.put("groupPrice", s.getGroupPrice());
                        item.put("durationHours", s.getDurationHours());
                        item.put("maxCapacity", s.getMaxCapacity());
                        item.put("targetAudience", s.getTargetAudience() != null
                                ? s.getTargetAudience().name() : null);
                        item.put("includedServices", s.getIncludedServices());
                        item.put("availableLanguages", s.getAvailableLanguages());
                        item.put("serviceType", "TOURIST");
                        item.put("reason", idx < reasons.size()
                                ? reasons.get(idx) : "Recommandé par l'IA");
                        item.put("score", Math.max(95 - (idx * 10), 65)); // ← AJOUTER
                        recommended.add(item);
                    });
        }

        return new ProfileSuggestionResponse(analysis, role, userName, recommended);
    }

    private ProfileSuggestionResponse buildCollaborationResponse(
            String aiJson,
            List<CollaborationService> allServices,
            String role,
            String userName) {

        JsonNode node    = parseAiJson(aiJson);
        String analysis  = getTextField(node, "analysis");
        List<Long> ids   = getIdsList(node, "recommendedIds");
        List<String> reasons = getStringList(node, "reasons");

        System.out.println("[PARTNER] IDs recommandés par l'IA : " + ids);

        List<Map<String, Object>> recommended = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            final long id  = ids.get(i);
            final int  idx = i;
            allServices.stream()
                    .filter(s -> s.getId() != null && s.getId() == id)
                    .findFirst()
                    .ifPresent(s -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", s.getId());
                        item.put("name", s.getName());
                        item.put("description", s.getDescription());
                        item.put("collaborationType", s.getCollaborationType() != null
                                ? s.getCollaborationType().name() : null);
                        item.put("activityDomain", s.getActivityDomain() != null
                                ? s.getActivityDomain().name() : null);
                        item.put("requestedBudget", s.getRequestedBudget());
                        item.put("collaborationDuration", s.getCollaborationDuration());
                        item.put("requiredSkills", s.getRequiredSkills());
                        item.put("expectedBenefits", s.getExpectedBenefits());
                        item.put("serviceType", "COLLABORATION");
                        item.put("reason", idx < reasons.size()
                                ? reasons.get(idx) : "Recommandé par l'IA");
                        item.put("score", Math.max(95 - (idx * 10), 65));
                        recommended.add(item);
                    });
        }

        return new ProfileSuggestionResponse(analysis, role, userName, recommended);
    }

    private ProfileSuggestionResponse buildCompanyResponse(
            String aiJson,
            List<InvestmentService> investServices,
            List<CollaborationService> colabServices,
            String companyName) {

        JsonNode node            = parseAiJson(aiJson);
        String analysis          = getTextField(node, "analysis");
        List<Long> investIds     = getIdsList(node, "recommendedInvestmentIds");
        List<String> investReasons = getStringList(node, "investmentReasons");
        List<Long> colabIds      = getIdsList(node, "recommendedCollaborationIds");
        List<String> colabReasons  = getStringList(node, "collaborationReasons");

        System.out.println("[INTL_COMPANY] InvestIDs: " + investIds + " | ColabIDs: " + colabIds);

        List<Map<String, Object>> recommended = new ArrayList<>();

        // Services d'investissement
        for (int i = 0; i < investIds.size(); i++) {
            final long id  = investIds.get(i);
            final int  idx = i;
            investServices.stream()
                    .filter(s -> s.getId() != null && s.getId() == id)
                    .findFirst()
                    .ifPresent(s -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", s.getId());
                        item.put("title", s.getTitle());
                        item.put("name", s.getName());
                        item.put("description", s.getDescription());
                        item.put("totalAmount", s.getTotalAmount());
                        item.put("minimumAmount", s.getMinimumAmount());
                        item.put("zone", s.getZone());
                        item.put("projectDuration", s.getProjectDuration());
                        item.put("sector", s.getEconomicSector() != null
                                ? s.getEconomicSector().getName() : null);
                        item.put("serviceType", "INVESTMENT");
                        item.put("reason", idx < investReasons.size()
                                ? investReasons.get(idx) : "Recommandé par l'IA");
                        item.put("score", Math.max(95 - (idx * 10), 65)); // ← AJOUTER
                        recommended.add(item);
                    });
        }

        // Services de collaboration
        for (int i = 0; i < colabIds.size(); i++) {
            final long id  = colabIds.get(i);
            final int  idx = i;
            colabServices.stream()
                    .filter(s -> s.getId() != null && s.getId() == id)
                    .findFirst()
                    .ifPresent(s -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        item.put("id", s.getId());
                        item.put("name", s.getName());
                        item.put("description", s.getDescription());
                        item.put("collaborationType", s.getCollaborationType() != null
                                ? s.getCollaborationType().name() : null);
                        item.put("activityDomain", s.getActivityDomain() != null
                                ? s.getActivityDomain().name() : null);
                        item.put("requestedBudget", s.getRequestedBudget());
                        item.put("collaborationDuration", s.getCollaborationDuration());
                        item.put("requiredSkills", s.getRequiredSkills());
                        item.put("serviceType", "COLLABORATION");
                        item.put("reason", idx < colabReasons.size()
                                ? colabReasons.get(idx) : "Recommandé par l'IA");
                        item.put("score", Math.max(95 - (idx * 10), 65));
                        recommended.add(item);
                    });
        }

        return new ProfileSuggestionResponse(analysis, "INTERNATIONAL_COMPANY",
                companyName, recommended);
    }
}