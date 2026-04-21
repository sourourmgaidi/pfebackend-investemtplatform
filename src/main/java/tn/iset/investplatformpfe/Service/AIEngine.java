package tn.iset.investplatformpfe.Service;

import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Entity.CollaborationService;
import tn.iset.investplatformpfe.Entity.InvestmentService;
import tn.iset.investplatformpfe.Entity.TouristService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.iset.investplatformpfe.Dto.RecommendationRequestDTO;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



@Service
public class AIEngine {

    private static final Logger log = LoggerFactory.getLogger(AIEngine.class);
    private static final String OLLAMA_URL  = "http://localhost:11434/api/generate";
    private static final String MODEL_NAME  = "phi3";
    private static final int    TIMEOUT_SEC = 60;

    private final HttpClient   httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper     = new ObjectMapper();

    // ═════════════════════════════════════════════════════════════════
    //  POINT D'ENTRÉE PRINCIPAL
    //  Pondération : 70% règles métier (déterministes) + 30% IA
    // ═════════════════════════════════════════════════════════════════
    public AIScoreResult computeScoreWithAI(Object service, RecommendationRequestDTO dto) {

        int ruleScore = computeRuleScore(service, dto);

        try {
            String prompt  = buildStrictPrompt(service, dto);
            String rawResp = callOllama(prompt);
            System.out.println("AI RAW RESPONSE: " + rawResp);

            String cleanJson = extractJson(rawResp);
            JsonNode json    = mapper.readTree(cleanJson);

            int    aiScore     = Math.max(0, Math.min(10, json.path("score").asInt(5)));
            String explanation = json.path("explanation").asText("Analyse IA indisponible.");

            // ── Pondération : 70% règles + 30% IA ──────────────────
            int finalScore = (int) Math.round(0.70 * ruleScore + 0.30 * aiScore);
            finalScore     = Math.max(0, Math.min(10, finalScore));

            return new AIScoreResult(finalScore, explanation, true);

        } catch (Exception e) {
            log.warn("⚠️ Erreur IA, fallback règles : {}", e.getMessage());
            return new AIScoreResult(ruleScore, "Score basé sur règles métier.", false);
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  PROMPT STRICT — JSON uniquement
    // ═════════════════════════════════════════════════════════════════
    private String buildStrictPrompt(Object service, RecommendationRequestDTO dto) {
        String userJson    = buildUserJson(dto);
        String serviceDesc = describeService(service);

        return "You are a JSON-only scoring engine. Respond with EXACTLY this format and nothing else:\n" +
                "{\"score\": <integer 0-10>, \"explanation\": \"<one sentence in French>\"}\n\n" +
                "Rules:\n" +
                "- Output ONLY valid JSON. No markdown, no backticks, no extra text.\n" +
                "- score must be an integer 0-10.\n" +
                "- explanation: one sentence in French, no line breaks, no inner quotes.\n" +
                "- Do NOT add any other keys.\n\n" +
                "User: " + userJson + "\n" +
                "Service: " + serviceDesc + "\n" +
                "JSON:";
    }

    // ── Profil utilisateur compact ────────────────────────────────
    private String buildUserJson(RecommendationRequestDTO dto) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;

        first = appendStr(sb, "type",        dto.getUserType() != null ? dto.getUserType().name() : null, first);
        first = appendNum(sb, "budget",      dto.getBudget() != null   ? dto.getBudget().doubleValue() : null, first);
        first = appendNum(sb, "regionId",    dto.getRegionId() != null ? dto.getRegionId().doubleValue() : null, first);
        first = appendStr(sb, "domain",      dto.getActivityDomain() != null ? dto.getActivityDomain().name() : null, first);
        first = appendStr(sb, "sector",      sanitize(dto.getPreferredSector()), first);
        first = appendStr(sb, "horizon",     sanitize(dto.getInvestmentHorizon()), first);
        first = appendStr(sb, "collabType",  sanitize(dto.getCollaborationType()), first);
        first = appendStr(sb, "goal",        truncate(sanitize(dto.getCollaborationGoal()), 100), first);
        first = appendStr(sb, "audience",    dto.getTargetAudience() != null ? dto.getTargetAudience().name() : null, first);
        first = appendStr(sb, "filter",      sanitize(dto.getServiceTypeFilter()), first);
        first = appendStr(sb, "desc",        truncate(sanitize(dto.getProjectDescription()), 120), first);
        first = appendStr(sb, "riskLevel",   sanitize(dto.getRiskLevel()), first);
        first = appendStr(sb, "country",     sanitize(dto.getOriginCountry()), first);
        first = appendStr(sb, "goal2",       sanitize(dto.getStrategicGoal()), first);
        first = appendStr(sb, "skills",      dto.getOfferedSkills() != null ? String.join(",", dto.getOfferedSkills()) : null, first);
        first = appendStr(sb, "partnerCrit", truncate(sanitize(dto.getPartnerCriteria()), 100), first);

        sb.append("}");
        return sb.toString();
    }

    // ── Description service compact ──────────────────────────────
    private String describeService(Object service) {
        if (service instanceof TouristService ts) {
            return String.format(
                    "{\"type\":\"TOURIST\",\"name\":\"%s\",\"price\":%s,\"region\":\"%s\",\"audience\":\"%s\",\"availability\":\"%s\"}",
                    sanitize(ts.getName()),
                    ts.getPrice() != null ? ts.getPrice().longValue() : "null",
                    ts.getRegion() != null ? sanitize(ts.getRegion().getName()) : "N/A",
                    ts.getTargetAudience() != null ? ts.getTargetAudience().name() : "N/A",
                    ts.getAvailability() != null ? ts.getAvailability().name() : "N/A"
            );
        }
        if (service instanceof InvestmentService is) {
            return String.format(
                    "{\"type\":\"INVESTMENT\",\"name\":\"%s\",\"minAmount\":%s,\"totalAmount\":%s,\"sector\":\"%s\",\"region\":\"%s\",\"availability\":\"%s\",\"projectDuration\":\"%s\"}",
                    sanitize(is.getTitle() != null ? is.getTitle() : is.getName()),
                    is.getMinimumAmount() != null ? is.getMinimumAmount().longValue() : "null",
                    is.getTotalAmount() != null ? is.getTotalAmount().longValue() : "null",
                    is.getEconomicSector() != null ? sanitize(is.getEconomicSector().getName()) : "N/A",
                    is.getRegion() != null ? sanitize(is.getRegion().getName()) : "N/A",
                    is.getAvailability() != null ? is.getAvailability().name() : "N/A",
                    is.getProjectDuration() != null ? sanitize(is.getProjectDuration()) : "N/A"
            );
        }
        if (service instanceof CollaborationService cs) {
            String skills = cs.getRequiredSkills() != null
                    ? cs.getRequiredSkills().stream().limit(5).toList().toString()
                    .replace("[", "").replace("]", "")
                    : "";
            return String.format(
                    "{\"type\":\"COLLABORATION\",\"name\":\"%s\",\"domain\":\"%s\",\"collab\":\"%s\",\"skills\":\"%s\",\"region\":\"%s\",\"budget\":%s,\"availability\":\"%s\"}",
                    sanitize(cs.getName()),
                    cs.getActivityDomain() != null ? cs.getActivityDomain().name() : "N/A",
                    cs.getCollaborationType() != null ? cs.getCollaborationType().name() : "N/A",
                    sanitize(skills),
                    cs.getRegion() != null ? sanitize(cs.getRegion().getName()) : "N/A",
                    cs.getRequestedBudget() != null ? cs.getRequestedBudget().longValue() : "null",
                    cs.getAvailability() != null ? cs.getAvailability().name() : "N/A"
            );
        }
        return "{\"type\":\"UNKNOWN\"}";
    }

    // ═════════════════════════════════════════════════════════════════
    //  APPEL HTTP OLLAMA
    // ═════════════════════════════════════════════════════════════════
    private String callOllama(String prompt) throws Exception {
        Map<String, Object> body = Map.of(
                "model",  MODEL_NAME,
                "prompt", prompt,
                "stream", false,
                "options", Map.of(
                        "temperature", 0.1,
                        "num_predict", 150,
                        "stop", new String[]{"\n\n", "```", "User:", "Service:"}
                )
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(OLLAMA_URL))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(TIMEOUT_SEC))
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode node = mapper.readTree(response.body());
        return node.path("response").asText("");
    }

    // ═════════════════════════════════════════════════════════════════
    //  EXTRACTION JSON ROBUSTE
    // ═════════════════════════════════════════════════════════════════
    String extractJson(String raw) {
        if (raw == null || raw.isBlank()) return fallbackJson(5, "Réponse IA vide.");

        String candidate = findFirstValidJsonObject(raw);
        if (candidate != null && hasScoreAndExplanation(candidate)) return candidate;

        String cleaned = raw.replace("```json", "").replace("```", "").trim();
        candidate = findFirstValidJsonObject(cleaned);
        if (candidate != null && hasScoreAndExplanation(candidate)) return candidate;

        Integer score       = extractScoreRegex(raw);
        String  explanation = extractExplanationRegex(raw);
        if (score != null && explanation != null) return buildJson(score, explanation);
        if (score != null) return buildJson(score, "Score attribué par analyse IA.");

        log.warn("⚠️ JSON non extractible : {}", raw.substring(0, Math.min(150, raw.length())));
        return fallbackJson(5, "Analyse IA non exploitable.");
    }

    private String findFirstValidJsonObject(String text) {
        int start = text.indexOf('{');
        if (start < 0) return null;
        int depth = 0; boolean inStr = false; boolean esc = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (esc) { esc = false; continue; }
            if (c == '\\' && inStr) { esc = true; continue; }
            if (c == '"') { inStr = !inStr; continue; }
            if (inStr) continue;
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    String cand = text.substring(start, i + 1);
                    try { mapper.readTree(cand); return cand; }
                    catch (Exception ex) {
                        start = text.indexOf('{', i + 1);
                        if (start < 0) return null;
                        i = start - 1; depth = 0;
                    }
                }
            }
        }
        return null;
    }

    private boolean hasScoreAndExplanation(String json) {
        try {
            JsonNode n = mapper.readTree(json);
            return n.has("score") && n.has("explanation") && n.get("score").isInt();
        } catch (Exception e) { return false; }
    }

    private Integer extractScoreRegex(String text) {
        Matcher m = Pattern.compile("\"score\"\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) return Math.max(0, Math.min(10, Integer.parseInt(m.group(1))));
        return null;
    }

    private String extractExplanationRegex(String text) {
        Matcher m = Pattern.compile("\"explanation\"\\s*:\\s*\"([^\"]{5,400})\"",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL).matcher(text);
        if (m.find()) return m.group(1).replaceAll("[\\r\\n]+", " ").trim();
        return null;
    }

    private String buildJson(int score, String explanation) {
        String safe = explanation.replace("\"", "'").replaceAll("[\\r\\n]+", " ");
        return "{\"score\": " + score + ", \"explanation\": \"" + safe + "\"}";
    }

    private String fallbackJson(int score, String explanation) {
        return buildJson(score, explanation);
    }

    // ═════════════════════════════════════════════════════════════════
    //  SCORE PAR RÈGLES MÉTIER — VERSION AMÉLIORÉE
    //
    //  Principe : chaque critère rempli par l'utilisateur DOIT être
    //  pris en compte avec un poids significatif.
    //  Score sur 100 points puis ramené sur 10.
    // ═════════════════════════════════════════════════════════════════
    public int computeRuleScore(Object service, RecommendationRequestDTO dto) {

        // ── SERVICE TOURISTIQUE ──────────────────────────────────────
        if (service instanceof TouristService ts) {
            return scoreTourist(ts, dto);
        }

        // ── OPPORTUNITÉ D'INVESTISSEMENT ─────────────────────────────
        if (service instanceof InvestmentService is) {
            return scoreInvestment(is, dto);
        }

        // ── SERVICE DE COLLABORATION ─────────────────────────────────
        if (service instanceof CollaborationService cs) {
            return scoreCollaboration(cs, dto);
        }

        return 0;
    }

    // ─────────────────────────────────────────────────────────────────
    //  TOURISTE  — 100 pts max
    // ─────────────────────────────────────────────────────────────────
    private int scoreTourist(TouristService ts, RecommendationRequestDTO dto) {

        int pts = 0;
        int max = 0;

        // Région (poids fort) — 30 pts
        if (dto.getRegionId() != null) {
            max += 30;
            if (ts.getRegion() != null && ts.getRegion().getId().equals(dto.getRegionId())) {
                pts += 30;
            }
        }

        // Budget (poids fort) — 25 pts
        if (dto.getBudget() != null && ts.getPrice() != null) {
            max += 25;
            if (ts.getPrice().compareTo(dto.getBudget()) <= 0) {
                pts += 25;
            } else {
                // Pénalité progressive si légèrement au-dessus du budget
                double ratio = dto.getBudget().doubleValue() / ts.getPrice().doubleValue();
                if (ratio >= 0.8) pts += 10; // 20% au-dessus → partial
            }
        }

        // Audience cible — 25 pts
        if (dto.getTargetAudience() != null && ts.getTargetAudience() != null) {
            max += 25;
            if (ts.getTargetAudience().name().equalsIgnoreCase(dto.getTargetAudience().name())) {
                pts += 25;
            }
        }

        // Disponibilité — 15 pts
        if (dto.getAvailability() != null && ts.getAvailability() != null) {
            max += 15;
            if (ts.getAvailability().name().equalsIgnoreCase(dto.getAvailability())) {
                pts += 15;
            }
        }

        // Domaine d'activité — 5 pts (bonus)
        /*if (dto.getActivityDomain() != null && ts.getActivityDomain() != null) {
            max += 5;
            if (ts.getActivityDomain().name().equalsIgnoreCase(dto.getActivityDomain().name())) {
                pts += 5;
            }
        }*/

        // Si aucun critère de filtrage fourni → score de base 5
        if (max == 0) return 5;

        // Ramener sur 10 (proportionnellement aux critères fournis)
        double ratio = (double) pts / max;
        return (int) Math.round(ratio * 10);
    }

    // ─────────────────────────────────────────────────────────────────
    //  INVESTISSEMENT  — 100 pts max
    // ─────────────────────────────────────────────────────────────────
    private int scoreInvestment(InvestmentService is, RecommendationRequestDTO dto) {

        int pts = 0;
        int max = 0;

        // Région (poids fort) — 30 pts
        if (dto.getRegionId() != null) {
            max += 30;
            if (is.getRegion() != null && is.getRegion().getId().equals(dto.getRegionId())) {
                pts += 30;
            }
        }

        // Budget — 25 pts
        if (dto.getBudget() != null) {
            max += 25;
            java.math.BigDecimal minAmt = is.getMinimumAmount();
            if (minAmt == null) minAmt = is.getTotalAmount();

            if (minAmt != null) {
                if (minAmt.compareTo(dto.getBudget()) <= 0) {
                    pts += 25; // Budget suffisant
                } else {
                    // Pénalité progressive
                    double ratio = dto.getBudget().doubleValue() / minAmt.doubleValue();
                    if (ratio >= 0.8) pts += 12;
                    else if (ratio >= 0.5) pts += 5;
                }
            } else {
                pts += 15; // Pas de montant minimum → neutre favorable
            }
        }

        // Domaine d'activité / Secteur économique — 20 pts
        if (dto.getActivityDomain() != null) {
            max += 20;
            if (is.getEconomicSector() != null) {
                String sectorName = is.getEconomicSector().getName().toUpperCase();
                String domain     = dto.getActivityDomain().name().toUpperCase();
                if (sectorName.contains(domain) || domain.contains(sectorName)
                        || sectorDomainMatch(sectorName, domain)) {
                    pts += 20;
                }
            }
        }

        // Secteur préféré (texte libre) — 10 pts bonus
        if (dto.getPreferredSector() != null && !dto.getPreferredSector().isBlank()) {
            max += 10;
            if (is.getEconomicSector() != null) {
                String sectorName = is.getEconomicSector().getName().toLowerCase();
                String preferred  = dto.getPreferredSector().toLowerCase();
                if (sectorName.contains(preferred) || preferred.contains(sectorName)) {
                    pts += 10;
                }
            }
        }

        // Disponibilité — 10 pts
        if (dto.getAvailability() != null && is.getAvailability() != null) {
            max += 10;
            if (is.getAvailability().name().equalsIgnoreCase(dto.getAvailability())) {
                pts += 10;
            }
        }

        // Horizon d'investissement — 5 pts
        if (dto.getInvestmentHorizon() != null && !dto.getInvestmentHorizon().isBlank()) {
            max += 5;
            if (is.getProjectDuration() != null) {
                if (horizonMatchesDuration(dto.getInvestmentHorizon(), is.getProjectDuration())) {
                    pts += 5;
                }
            } else {
                pts += 2; // Durée non renseignée → neutre
            }
        }

        // Bonus : Société Internationale — 5 pts
        if (dto.getUserType() != null && dto.getUserType().name().equals("INTERNATIONAL_COMPANY")) {
            max += 5;
            pts += 5;
        }

        if (max == 0) return 5;
        double ratio = (double) pts / max;
        return (int) Math.round(ratio * 10);
    }

    // ─────────────────────────────────────────────────────────────────
    //  COLLABORATION  — 100 pts max
    // ─────────────────────────────────────────────────────────────────
    private int scoreCollaboration(CollaborationService cs, RecommendationRequestDTO dto) {

        int pts = 0;
        int max = 0;

        // Région (poids fort) — 25 pts
        if (dto.getRegionId() != null) {
            max += 25;
            if (cs.getRegion() != null && cs.getRegion().getId().equals(dto.getRegionId())) {
                pts += 25;
            }
        }

        // Domaine d'activité — 25 pts
        if (dto.getActivityDomain() != null) {
            max += 25;
            if (cs.getActivityDomain() != null
                    && cs.getActivityDomain().name().equalsIgnoreCase(dto.getActivityDomain().name())) {
                pts += 25;
            }
        }

        // Compétences requises ↔ offertes — jusqu'à 20 pts
        if (dto.getOfferedSkills() != null && !dto.getOfferedSkills().isEmpty()
                && cs.getRequiredSkills() != null && !cs.getRequiredSkills().isEmpty()) {
            max += 20;
            long matches = cs.getRequiredSkills().stream()
                    .filter(required -> dto.getOfferedSkills().stream()
                            .anyMatch(offered -> offered.trim().equalsIgnoreCase(required.trim())))
                    .count();
            // Proportionnel au nombre de compétences requises
            double matchRatio = (double) matches / cs.getRequiredSkills().size();
            pts += (int) Math.round(matchRatio * 20);
        }

        // Type de collaboration — 15 pts
        if (dto.getCollaborationType() != null && !dto.getCollaborationType().isBlank()) {
            max += 15;
            if (cs.getCollaborationType() != null
                    && cs.getCollaborationType().name().equalsIgnoreCase(dto.getCollaborationType())) {
                pts += 15;
            }
        }

        // Disponibilité — 10 pts
        if (dto.getAvailability() != null && cs.getAvailability() != null) {
            max += 10;
            if (cs.getAvailability().name().equalsIgnoreCase(dto.getAvailability())) {
                pts += 10;
            }
        }

        // Budget — 5 pts
        if (dto.getBudget() != null && cs.getRequestedBudget() != null) {
            max += 5;
            if (cs.getRequestedBudget().compareTo(dto.getBudget()) <= 0) {
                pts += 5;
            }
        }

        if (max == 0) return 5;
        double ratio = (double) pts / max;
        return (int) Math.round(ratio * 10);
    }

    // ═════════════════════════════════════════════════════════════════
    //  HELPERS — correspondances métier
    // ═════════════════════════════════════════════════════════════════

    /**
     * Correspondance secteur économique ↔ domaine d'activité
     * (les noms en base peuvent différer des enum front)
     */
    private boolean sectorDomainMatch(String sectorUpper, String domainUpper) {
        return switch (domainUpper) {
            case "TECHNOLOGY", "IT"        -> sectorUpper.contains("TECH") || sectorUpper.contains("INFORMAT") || sectorUpper.contains("DIGIT") || sectorUpper.contains("LOGIC");
            case "AGRICULTURE", "AGRI_FOOD"-> sectorUpper.contains("AGRI") || sectorUpper.contains("AGRO") || sectorUpper.contains("ALIMENTAIRE");
            case "TOURISM", "HOTEL"        -> sectorUpper.contains("TOUR") || sectorUpper.contains("HOTEL") || sectorUpper.contains("HOSPITAL");
            case "INDUSTRY","MANUFACTURING"-> sectorUpper.contains("INDUS") || sectorUpper.contains("MANUFACTUR") || sectorUpper.contains("FABRI");
            case "ENERGY","RENEWABLE_ENERGY"-> sectorUpper.contains("ENERG") || sectorUpper.contains("SOLAIRE") || sectorUpper.contains("RENOUV");
            case "FINANCE"                 -> sectorUpper.contains("FINANC") || sectorUpper.contains("BANQUE") || sectorUpper.contains("ASSUR");
            case "HEALTH"                  -> sectorUpper.contains("SANT") || sectorUpper.contains("MEDICAL") || sectorUpper.contains("PHARMA");
            case "EDUCATION"               -> sectorUpper.contains("EDUC") || sectorUpper.contains("FORM");
            case "CONSTRUCTION","REAL_ESTATE"-> sectorUpper.contains("CONSTRU") || sectorUpper.contains("IMMOB") || sectorUpper.contains("BTP");
            case "TEXTILE"                 -> sectorUpper.contains("TEXTILE") || sectorUpper.contains("HABILLEMENT");
            case "TRADE","SERVICES"        -> sectorUpper.contains("COMMERC") || sectorUpper.contains("SERVICE") || sectorUpper.contains("TRADE");
            default                        -> false;
        };
    }

    /**
     * Correspondance horizon d'investissement ↔ durée de projet
     */
    private boolean horizonMatchesDuration(String horizon, String duration) {
        if (horizon == null || duration == null) return false;
        String h = horizon.toLowerCase();
        String d = duration.toLowerCase();

        if (h.contains("court")) {
            return d.contains("1") || d.contains("2") || d.contains("court") || d.contains("< 2") || d.contains("an");
        }
        if (h.contains("moyen")) {
            return d.contains("2") || d.contains("3") || d.contains("4") || d.contains("5") || d.contains("moyen");
        }
        if (h.contains("long")) {
            return d.contains("5") || d.contains("6") || d.contains("7") || d.contains("10") || d.contains("long") || d.contains("+");
        }
        return false;
    }

    // ═════════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ═════════════════════════════════════════════════════════════════
    private boolean appendStr(StringBuilder sb, String key, String value, boolean first) {
        if (value == null || value.isBlank()) return first;
        if (!first) sb.append(",");
        sb.append("\"").append(key).append("\":\"")
                .append(value.replace("\"", "'")).append("\"");
        return false;
    }

    private boolean appendNum(StringBuilder sb, String key, Double value, boolean first) {
        if (value == null) return first;
        if (!first) sb.append(",");
        sb.append("\"").append(key).append("\":").append(value.longValue());
        return false;
    }

    private String sanitize(String text) {
        if (text == null) return null;
        return text
                .replaceAll("[`\\{\\}\\[\\]]", "")
                .replaceAll("\\\\", "")
                .replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private String truncate(String text, int max) {
        if (text == null) return null;
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }

    // ═════════════════════════════════════════════════════════════════
    //  RÉSULTAT
    // ═════════════════════════════════════════════════════════════════
    public static class AIScoreResult {
        public final int     totalScore;
        public final String  explanation;
        public final boolean isAIScored;

        public AIScoreResult(int totalScore, String explanation, boolean isAIScored) {
            this.totalScore  = totalScore;
            this.explanation = explanation;
            this.isAIScored  = isAIScored;
        }
    }
}