package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Entity.CollaborationService;
import tn.iset.investplatformpfe.Entity.InvestmentService;
import tn.iset.investplatformpfe.Entity.TouristService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.iset.investplatformpfe.Dto.RecommendationRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AIEngine {

    private static final Logger log = LoggerFactory.getLogger(AIEngine.class);

    @Autowired
    private GroqAiClient groqAiClient;

    private final ObjectMapper mapper = new ObjectMapper();

    // ═════════════════════════════════════════════════════════════════
    //  POINT D'ENTRÉE PRINCIPAL
    //  Pondération : 70% règles métier + 30% IA
    // ═════════════════════════════════════════════════════════════════
    public AIScoreResult computeScoreWithAI(Object service, RecommendationRequestDTO dto) {

        int ruleScore = computeRuleScore(service, dto);

        try {
            String prompt  = buildUserMessage(service, dto);
            String rawResp = callGroq(prompt);
            System.out.println("AI RAW RESPONSE: " + rawResp);

            String cleanJson = extractJson(rawResp);
            JsonNode json    = mapper.readTree(cleanJson);

            int    aiScore     = Math.max(0, Math.min(10, json.path("score").asInt(5)));
            String explanation = json.path("explanation").asText("Analyse IA indisponible.");

            int finalScore = (int) Math.round(0.70 * ruleScore + 0.30 * aiScore);
            finalScore     = Math.max(0, Math.min(10, finalScore));

            return new AIScoreResult(finalScore, explanation, true);

        } catch (Exception e) {
            log.warn("⚠️ Erreur IA, fallback règles : {}", e.getMessage());
            return new AIScoreResult(ruleScore, "Score basé sur règles métier.", false);
        }
    }

    // ═════════════════════════════════════════════════════════════════
    //  APPEL GROQ — prompt enrichi
    // ═════════════════════════════════════════════════════════════════
    private String callGroq(String prompt) {
        String systemPrompt =
                "You are an expert investment and tourism advisor for Tunisia. " +
                        "You score service-user compatibility from 0 to 10. " +
                        "Scoring rules: " +
                        "- 9-10: Perfect match on region, budget, domain AND availability. " +
                        "- 7-8: Good match on 3 out of 4 criteria. " +
                        "- 5-6: Partial match, 2 criteria aligned. " +
                        "- 3-4: Weak match, only 1 criterion aligned. " +
                        "- 0-2: No meaningful match. " +
                        "Consider: budget compatibility, geographic proximity, " +
                        "sector alignment, collaboration type fit, skill matching. " +
                        "Respond with EXACTLY this JSON and nothing else: " +
                        "{\"score\": <integer 0-10>, \"explanation\": \"<one sentence in French>\"}. " +
                        "No markdown, no backticks, no extra text.";
        return groqAiClient.chat(systemPrompt, prompt);
    }

    // ═════════════════════════════════════════════════════════════════
    //  CONSTRUCTION DU MESSAGE UTILISATEUR — enrichi avec contexte
    // ═════════════════════════════════════════════════════════════════
    private String buildUserMessage(Object service, RecommendationRequestDTO dto) {
        String userJson    = buildUserJson(dto);
        String serviceDesc = describeService(service);
        String context     = buildScoringContext(service, dto);

        return "User profile: " + userJson +
                "\nService to evaluate: " + serviceDesc +
                "\nPre-computed rule score: " + computeRuleScore(service, dto) + "/10" +
                "\nContext: " + context;
    }

    // ── Contexte additionnel pour guider l'IA ────────────────────────
    private String buildScoringContext(Object service, RecommendationRequestDTO dto) {
        StringBuilder ctx = new StringBuilder();

        // Région match
        if (dto.getRegionId() != null) {
            boolean regionMatch = false;
            if (service instanceof TouristService ts && ts.getRegion() != null)
                regionMatch = ts.getRegion().getId().equals(dto.getRegionId());
            if (service instanceof InvestmentService is && is.getRegion() != null)
                regionMatch = is.getRegion().getId().equals(dto.getRegionId());
            if (service instanceof CollaborationService cs && cs.getRegion() != null)
                regionMatch = cs.getRegion().getId().equals(dto.getRegionId());
            ctx.append(regionMatch ? "Region: MATCH. " : "Region: MISMATCH. ");
        }

        // Budget match
        if (dto.getBudget() != null) {
            BigDecimal svcPrice = null;
            if (service instanceof TouristService ts)       svcPrice = ts.getPrice();
            if (service instanceof InvestmentService is)    svcPrice = is.getMinimumAmount() != null ? is.getMinimumAmount() : is.getTotalAmount();
            if (service instanceof CollaborationService cs) svcPrice = cs.getRequestedBudget();

            if (svcPrice != null) {
                double ratio = dto.getBudget().doubleValue() / svcPrice.doubleValue();
                if (ratio >= 1.0)      ctx.append("Budget: COMFORTABLE. ");
                else if (ratio >= 0.8) ctx.append("Budget: TIGHT. ");
                else                   ctx.append("Budget: INSUFFICIENT. ");
            }
        }

        // Skills match pour collaboration
        if (service instanceof CollaborationService cs &&
                dto.getOfferedSkills() != null && !dto.getOfferedSkills().isEmpty() &&
                cs.getRequiredSkills() != null && !cs.getRequiredSkills().isEmpty()) {
            long matches = cs.getRequiredSkills().stream()
                    .filter(req -> dto.getOfferedSkills().stream()
                            .anyMatch(offered -> offered.trim().equalsIgnoreCase(req.trim())))
                    .count();
            int pct = (int) Math.round((double) matches / cs.getRequiredSkills().size() * 100);
            ctx.append("Skills match: ").append(pct).append("%. ");
        }

        return ctx.toString();
    }

    // ── Profil utilisateur compact ────────────────────────────────────
    private String buildUserJson(RecommendationRequestDTO dto) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;

        first = appendStr(sb, "type",        dto.getUserType() != null ? dto.getUserType().name() : null, first);
        first = appendNum(sb, "budget",      dto.getBudget() != null ? dto.getBudget().doubleValue() : null, first);
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
        first = appendNum(sb, "groupSize",   dto.getGroupSize() != null ? dto.getGroupSize().doubleValue() : null, first);
        first = appendNum(sb, "prefDuration",dto.getPreferredDurationHours() != null ? dto.getPreferredDurationHours().doubleValue() : null, first);

        sb.append("}");
        return sb.toString();
    }

    // ── Description service compact ───────────────────────────────────
    private String describeService(Object service) {
        if (service instanceof TouristService ts) {
            return String.format(
                    "{\"type\":\"TOURIST\",\"name\":\"%s\",\"price\":%s,\"region\":\"%s\"," +
                            "\"audience\":\"%s\",\"availability\":\"%s\",\"durationHours\":%s,\"maxCapacity\":%s}",
                    sanitize(ts.getName()),
                    ts.getPrice() != null ? ts.getPrice().longValue() : "null",
                    ts.getRegion() != null ? sanitize(ts.getRegion().getName()) : "N/A",
                    ts.getTargetAudience() != null ? ts.getTargetAudience().name() : "N/A",
                    ts.getAvailability() != null ? ts.getAvailability().name() : "N/A",
                    ts.getDurationHours() != null ? ts.getDurationHours() : "null",
                    ts.getMaxCapacity() != null ? ts.getMaxCapacity() : "null"
            );
        }
        if (service instanceof InvestmentService is) {
            return String.format(
                    "{\"type\":\"INVESTMENT\",\"name\":\"%s\",\"minAmount\":%s,\"totalAmount\":%s," +
                            "\"sector\":\"%s\",\"region\":\"%s\",\"availability\":\"%s\",\"projectDuration\":\"%s\"}",
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
                    ? cs.getRequiredSkills().stream().limit(5).toList()
                    .toString().replace("[", "").replace("]", "")
                    : "";
            return String.format(
                    "{\"type\":\"COLLABORATION\",\"name\":\"%s\",\"domain\":\"%s\",\"collab\":\"%s\"," +
                            "\"skills\":\"%s\",\"region\":\"%s\",\"budget\":%s,\"availability\":\"%s\"}",
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
    //  SCORE PAR RÈGLES MÉTIER
    // ═════════════════════════════════════════════════════════════════
    public int computeRuleScore(Object service, RecommendationRequestDTO dto) {
        if (service instanceof TouristService ts)       return scoreTourist(ts, dto);
        if (service instanceof InvestmentService is)    return scoreInvestment(is, dto);
        if (service instanceof CollaborationService cs) return scoreCollaboration(cs, dto);
        return 0;
    }

    private int scoreTourist(TouristService ts, RecommendationRequestDTO dto) {
        int pts = 0, max = 0;

        // Région — 30 pts
        if (dto.getRegionId() != null && ts.getRegion() != null) {
            max += 30;
            if (ts.getRegion().getId().equals(dto.getRegionId())) pts += 30;
        }

        // Budget — 25 pts avec dégradé
        if (dto.getBudget() != null && ts.getPrice() != null) {
            max += 25;
            double ratio = dto.getBudget().doubleValue() / ts.getPrice().doubleValue();
            if (ratio >= 1.0)      pts += 25;
            else if (ratio >= 0.8) pts += 15;
            else if (ratio >= 0.6) pts += 8;
        }

        // Audience — 25 pts
        if (dto.getTargetAudience() != null && ts.getTargetAudience() != null) {
            max += 25;
            if (ts.getTargetAudience().name().equalsIgnoreCase(
                    dto.getTargetAudience().name())) pts += 25;
        }

        // Disponibilité — 15 pts
        if (dto.getAvailability() != null && ts.getAvailability() != null) {
            max += 15;
            if (ts.getAvailability().name().equalsIgnoreCase(dto.getAvailability())) pts += 15;
        }

        // Durée souhaitée — 10 pts
        if (dto.getPreferredDurationHours() != null && ts.getDurationHours() != null) {
            max += 10;
            int diff = Math.abs(dto.getPreferredDurationHours() - ts.getDurationHours());
            if (diff == 0)      pts += 10;
            else if (diff <= 2) pts += 6;
            else if (diff <= 5) pts += 3;
        }

        // Capacité groupe — 10 pts
        if (dto.getGroupSize() != null && ts.getMaxCapacity() != null) {
            max += 10;
            if (ts.getMaxCapacity() >= dto.getGroupSize()) pts += 10;
        }

        if (max == 0) return 5;
        return (int) Math.round((double) pts / max * 10);
    }

    private int scoreInvestment(InvestmentService is, RecommendationRequestDTO dto) {
        int pts = 0, max = 0;

        // Région — 30 pts
        if (dto.getRegionId() != null) {
            max += 30;
            if (is.getRegion() != null && is.getRegion().getId().equals(dto.getRegionId())) pts += 30;
        }

        // Budget — 25 pts
        if (dto.getBudget() != null) {
            max += 25;
            BigDecimal minAmt = is.getMinimumAmount() != null ? is.getMinimumAmount() : is.getTotalAmount();
            if (minAmt != null) {
                if (minAmt.compareTo(dto.getBudget()) <= 0) pts += 25;
                else {
                    double ratio = dto.getBudget().doubleValue() / minAmt.doubleValue();
                    if (ratio >= 0.8) pts += 12;
                    else if (ratio >= 0.5) pts += 5;
                }
            } else pts += 15;
        }

        // Domaine d'activité — 20 pts
        if (dto.getActivityDomain() != null) {
            max += 20;
            if (is.getEconomicSector() != null) {
                String sectorName = is.getEconomicSector().getName().toUpperCase();
                String domain     = dto.getActivityDomain().name().toUpperCase();
                if (sectorName.contains(domain) || domain.contains(sectorName)
                        || sectorDomainMatch(sectorName, domain)) pts += 20;
            }
        }

        // Secteur préféré — 10 pts
        if (dto.getPreferredSector() != null && !dto.getPreferredSector().isBlank()) {
            max += 10;
            if (is.getEconomicSector() != null) {
                String sectorName = is.getEconomicSector().getName().toLowerCase();
                String preferred  = dto.getPreferredSector().toLowerCase();
                if (sectorName.contains(preferred) || preferred.contains(sectorName)) pts += 10;
            }
        }

        // Disponibilité — 10 pts
        if (dto.getAvailability() != null && is.getAvailability() != null) {
            max += 10;
            if (is.getAvailability().name().equalsIgnoreCase(dto.getAvailability())) pts += 10;
        }

        // Horizon d'investissement — 5 pts
        if (dto.getInvestmentHorizon() != null && !dto.getInvestmentHorizon().isBlank()) {
            max += 5;
            if (is.getProjectDuration() != null) {
                if (horizonMatchesDuration(dto.getInvestmentHorizon(), is.getProjectDuration())) pts += 5;
            } else pts += 2;
        }

        // Bonus société internationale — 5 pts
        if (dto.getUserType() != null && dto.getUserType().name().equals("INTERNATIONAL_COMPANY")) {
            max += 5; pts += 5;
        }

        if (max == 0) return 5;
        return (int) Math.round((double) pts / max * 10);
    }

    private int scoreCollaboration(CollaborationService cs, RecommendationRequestDTO dto) {
        int pts = 0, max = 0;

        // Région — 25 pts
        if (dto.getRegionId() != null) {
            max += 25;
            if (cs.getRegion() != null && cs.getRegion().getId().equals(dto.getRegionId())) pts += 25;
        }

        // Domaine d'activité — 25 pts
        if (dto.getActivityDomain() != null) {
            max += 25;
            if (cs.getActivityDomain() != null &&
                    cs.getActivityDomain().name().equalsIgnoreCase(dto.getActivityDomain().name())) pts += 25;
        }

        // Compétences offertes vs requises — 20 pts
        if (dto.getOfferedSkills() != null && !dto.getOfferedSkills().isEmpty()
                && cs.getRequiredSkills() != null && !cs.getRequiredSkills().isEmpty()) {
            max += 20;
            long matches = cs.getRequiredSkills().stream()
                    .filter(required -> dto.getOfferedSkills().stream()
                            .anyMatch(offered -> offered.trim().equalsIgnoreCase(required.trim())))
                    .count();
            pts += (int) Math.round((double) matches / cs.getRequiredSkills().size() * 20);
        }

        // Type de collaboration — 15 pts
        if (dto.getCollaborationType() != null && !dto.getCollaborationType().isBlank()) {
            max += 15;
            if (cs.getCollaborationType() != null &&
                    cs.getCollaborationType().name().equalsIgnoreCase(dto.getCollaborationType())) pts += 15;
        }

        // Disponibilité — 10 pts
        if (dto.getAvailability() != null && cs.getAvailability() != null) {
            max += 10;
            if (cs.getAvailability().name().equalsIgnoreCase(dto.getAvailability())) pts += 10;
        }

        // Budget — 5 pts
        if (dto.getBudget() != null && cs.getRequestedBudget() != null) {
            max += 5;
            if (cs.getRequestedBudget().compareTo(dto.getBudget()) <= 0) pts += 5;
        }

        if (max == 0) return 5;
        return (int) Math.round((double) pts / max * 10);
    }

    // ═════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═════════════════════════════════════════════════════════════════
    // Méthode publique appelée par RecommendationService pour le filtrage domaine
    public boolean sectorDomainMatchPublic(String sectorUpper, String domainUpper) {
        return sectorDomainMatch(sectorUpper, domainUpper);
    }

    private boolean sectorDomainMatch(String sectorUpper, String domainUpper) {
        return switch (domainUpper) {
            case "TECHNOLOGY", "IT"             -> sectorUpper.contains("TECH") || sectorUpper.contains("INFORMAT") || sectorUpper.contains("DIGIT");
            case "AGRICULTURE", "AGRI_FOOD"     -> sectorUpper.contains("AGRI") || sectorUpper.contains("AGRO") || sectorUpper.contains("ALIMENTAIRE");
            case "TOURISM", "HOTEL"             -> sectorUpper.contains("TOUR") || sectorUpper.contains("HOTEL") || sectorUpper.contains("HOSPITAL");
            case "INDUSTRY", "MANUFACTURING"    -> sectorUpper.contains("INDUS") || sectorUpper.contains("MANUFACTUR");
            case "ENERGY", "RENEWABLE_ENERGY"   -> sectorUpper.contains("ENERG") || sectorUpper.contains("SOLAIRE") || sectorUpper.contains("RENOUV");
            case "FINANCE"                      -> sectorUpper.contains("FINANC") || sectorUpper.contains("BANQUE");
            case "HEALTH"                       -> sectorUpper.contains("SANT") || sectorUpper.contains("MEDICAL") || sectorUpper.contains("PHARMA");
            case "EDUCATION"                    -> sectorUpper.contains("EDUC") || sectorUpper.contains("FORM");
            case "CONSTRUCTION", "REAL_ESTATE"  -> sectorUpper.contains("CONSTRU") || sectorUpper.contains("IMMOB");
            case "TEXTILE"                      -> sectorUpper.contains("TEXTILE") || sectorUpper.contains("HABILLEMENT");
            case "TRADE", "SERVICES"            -> sectorUpper.contains("COMMERC") || sectorUpper.contains("SERVICE");
            default                             -> false;
        };
    }

    private boolean horizonMatchesDuration(String horizon, String duration) {
        if (horizon == null || duration == null) return false;
        String h = horizon.toLowerCase(), d = duration.toLowerCase();
        if (h.contains("court"))  return d.contains("1") || d.contains("2") || d.contains("court");
        if (h.contains("moyen"))  return d.contains("2") || d.contains("3") || d.contains("5") || d.contains("moyen");
        if (h.contains("long"))   return d.contains("5") || d.contains("10") || d.contains("long") || d.contains("+");
        return false;
    }

    private boolean appendStr(StringBuilder sb, String key, String value, boolean first) {
        if (value == null || value.isBlank()) return first;
        if (!first) sb.append(",");
        sb.append("\"").append(key).append("\":\"").append(value.replace("\"", "'")).append("\"");
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
        return text.replaceAll("[`\\{\\}\\[\\]]", "").replaceAll("\\\\", "")
                .replaceAll("[\\r\\n]+", " ").replaceAll("\\s{2,}", " ").trim();
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