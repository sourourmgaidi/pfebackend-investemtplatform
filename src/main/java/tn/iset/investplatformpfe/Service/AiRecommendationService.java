package tn.iset.investplatformpfe.Service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Dto.AiRecommendationDTO.AiRecommendationResponse;
import tn.iset.investplatformpfe.Dto.AiRecommendationDTO.ScoredService;
import tn.iset.investplatformpfe.Dto.AiRecommendationDTO.ServiceSummary;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Repository.*;
import tn.iset.investplatformpfe.Entity.TouristService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(AiRecommendationService.class);
    private static final int BATCH_SIZE = 5;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:phi3:mini}")
    private String ollamaModel;

    private final InvestorRepository investorRepository;
    private final EconomicPartnerRepository economicPartnerRepository;
    private final InternationalCompanyRepository internationalCompanyRepository;
    private final InvestmentServiceRepository investmentServiceRepository;
    private final CollaborationServiceRepository collaborationServiceRepository;
    private final TouristRepository touristRepository;                   // ← AJOUT
    private final TouristServiceRepository touristServiceRepository;     // ← AJOUT

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public AiRecommendationService(
            InvestorRepository investorRepository,
            EconomicPartnerRepository economicPartnerRepository,
            InternationalCompanyRepository internationalCompanyRepository,
            InvestmentServiceRepository investmentServiceRepository,
            CollaborationServiceRepository collaborationServiceRepository,
            TouristRepository touristRepository,                         // ← AJOUT
            TouristServiceRepository touristServiceRepository            // ← AJOUT
    ) {
        this.investorRepository = investorRepository;
        this.economicPartnerRepository = economicPartnerRepository;
        this.internationalCompanyRepository = internationalCompanyRepository;
        this.investmentServiceRepository = investmentServiceRepository;
        this.collaborationServiceRepository = collaborationServiceRepository;
        this.touristRepository = touristRepository;
        this.touristServiceRepository = touristServiceRepository;
    }

    // ════════════════════════════════════════════════════════════
    // MÉTHODE PRINCIPALE
    // ════════════════════════════════════════════════════════════

    public AiRecommendationResponse recommendForUser(String userEmail, String userRole) {

        UserProfile profile = buildUserProfile(userEmail, userRole);

        log.info("👤 Profil → role={} email={} name={} {} sector={} country={} company={}",
                profile.role, profile.email, profile.firstName, profile.lastName,
                profile.activitySector, profile.originCountry, profile.company);

        List<ServiceSummary> investmentServices    = new ArrayList<>();
        List<ServiceSummary> collaborationServices = new ArrayList<>();
        List<ServiceSummary> touristServices       = new ArrayList<>(); // ← AJOUT

        switch (userRole.toUpperCase()) {
            case "INVESTOR" ->
                    investmentServiceRepository.findByStatus(ServiceStatus.APPROVED)
                            .forEach(s -> investmentServices.add(toSummary(s)));
            case "ECONOMIC_PARTNER" ->
                    collaborationServiceRepository.findByStatus(ServiceStatus.APPROVED)
                            .forEach(s -> collaborationServices.add(toSummary(s)));
            case "INTERNATIONAL_COMPANY" -> {
                investmentServiceRepository.findByStatus(ServiceStatus.APPROVED)
                        .forEach(s -> investmentServices.add(toSummary(s)));
                collaborationServiceRepository.findByStatus(ServiceStatus.APPROVED)
                        .forEach(s -> collaborationServices.add(toSummary(s)));
            }
            // ── TOURIST : uniquement les TouristServices ──────────────
            case "TOURIST" ->
                    touristServiceRepository.findByStatus(ServiceStatus.APPROVED)
                            .forEach(s -> touristServices.add(toTouristSummary(s)));
        }

        log.info("📦 Services chargés → {} investment, {} collaboration, {} tourist",
                investmentServices.size(), collaborationServices.size(), touristServices.size());

        List<ServiceSummary> allServices = new ArrayList<>();
        allServices.addAll(investmentServices);
        allServices.addAll(collaborationServices);
        allServices.addAll(touristServices);

        if (allServices.isEmpty()) {
            AiRecommendationResponse empty = new AiRecommendationResponse();
            empty.setRankedServices(Collections.emptyList());
            empty.setGlobalExplanation("Aucun service disponible pour le moment.");
            return empty;
        }

        // Map composite TYPE_ID → type
        Map<String, String> serviceTypeMap = new HashMap<>();
        investmentServices.forEach(s -> serviceTypeMap.put("INVESTMENT_" + s.getId(), "INVESTMENT"));
        collaborationServices.forEach(s -> serviceTypeMap.put("COLLABORATION_" + s.getId(), "COLLABORATION"));
        touristServices.forEach(s -> serviceTypeMap.put("TOURIST_" + s.getId(), "TOURIST"));

        Map<Long, String> idToTypeMap = new HashMap<>();
        investmentServices.forEach(s -> idToTypeMap.put(s.getId(), "INVESTMENT"));
        collaborationServices.forEach(s -> idToTypeMap.putIfAbsent(s.getId(), "COLLABORATION"));
        touristServices.forEach(s -> idToTypeMap.putIfAbsent(s.getId(), "TOURIST"));

        return scoreBatched(profile, allServices, serviceTypeMap, idToTypeMap);
    }

    // ════════════════════════════════════════════════════════════
    // CONSTRUCTION DU PROFIL UTILISATEUR
    // ════════════════════════════════════════════════════════════

    private UserProfile buildUserProfile(String email, String role) {
        UserProfile profile = new UserProfile();
        profile.role  = role;
        profile.email = email;

        switch (role.toUpperCase()) {
            case "INVESTOR" -> {
                var opt = investorRepository.findByEmail(email);
                if (opt.isPresent()) {
                    var inv = opt.get();
                    profile.firstName      = inv.getFirstName();
                    profile.lastName       = inv.getLastName();
                    profile.activitySector = inv.getActivitySector() != null
                            ? inv.getActivitySector().name() : null;
                    profile.originCountry  = inv.getOriginCountry();
                    profile.company        = inv.getCompany();
                } else {
                    log.warn("⚠️ Aucun Investor trouvé pour email={}", email);
                }
            }
            case "ECONOMIC_PARTNER" -> {
                var opt = economicPartnerRepository.findByEmail(email);
                if (opt.isPresent()) {
                    var ep = opt.get();
                    profile.firstName      = ep.getFirstName();
                    profile.lastName       = ep.getLastName();
                    profile.activitySector = ep.getBusinessSector() != null
                            ? ep.getBusinessSector().name() : null;
                    profile.originCountry  = ep.getCountryOfOrigin();
                } else {
                    log.warn("⚠️ Aucun EconomicPartner trouvé pour email={}", email);
                }
            }
            case "INTERNATIONAL_COMPANY" -> {
                var opt = internationalCompanyRepository.findByEmail(email);
                if (opt.isPresent()) {
                    var ic = opt.get();
                    profile.firstName      = ic.getContactFirstName();
                    profile.lastName       = ic.getContactLastName();
                    profile.company        = ic.getCompanyName();
                    profile.activitySector = ic.getActivitySector() != null
                            ? ic.getActivitySector().name() : null;
                    profile.originCountry  = ic.getOriginCountry();
                } else {
                    log.warn("⚠️ Aucune InternationalCompany trouvée pour email={}", email);
                }
            }
            // ── TOURIST ──────────────────────────────────────────────
            case "TOURIST" -> {
                var opt = touristRepository.findByEmail(email);
                if (opt.isPresent()) {
                    var t = opt.get();
                    profile.firstName     = t.getFirstName();
                    profile.lastName      = t.getLastName();
                    profile.originCountry = t.getNationality();
                } else {
                    log.warn("⚠️ Aucun Tourist trouvé pour email={}", email);
                }
            }
        }
        return profile;
    }

    // ════════════════════════════════════════════════════════════
    // SCORING PAR BATCH
    // ════════════════════════════════════════════════════════════

    private AiRecommendationResponse scoreBatched(
            UserProfile profile,
            List<ServiceSummary> allServices,
            Map<String, String> serviceTypeMap,
            Map<Long, String> idToTypeMap
    ) {
        List<List<ServiceSummary>> batches = new ArrayList<>();
        for (int i = 0; i < allServices.size(); i += BATCH_SIZE) {
            batches.add(allServices.subList(i, Math.min(i + BATCH_SIZE, allServices.size())));
        }

        log.info("🔀 {} services → {} batch(es) de {} max",
                allServices.size(), batches.size(), BATCH_SIZE);

        Map<String, ScoredService> allScores = new LinkedHashMap<>();

        for (int i = 0; i < batches.size(); i++) {
            List<ServiceSummary> batch = batches.get(i);
            log.info("  📤 Batch {}/{} — IDs: {}", i + 1, batches.size(),
                    batch.stream().map(s -> s.getServiceType() + "_" + s.getId())
                            .collect(Collectors.joining(",")));
            List<ScoredService> batchResult = callOllamaForBatch(profile, batch, serviceTypeMap, idToTypeMap);
            batchResult.forEach(ss -> allScores.put(ss.getServiceType() + "_" + ss.getServiceId(), ss));
        }

        // Compléter les absents
        for (ServiceSummary s : allServices) {
            String compositeKey = s.getServiceType() + "_" + s.getId();
            if (!allScores.containsKey(compositeKey)) {
                log.warn("⚠️ Service {}_{} absent → fallback", s.getServiceType(), s.getId());
                ScoredService ss = new ScoredService();
                ss.setServiceId(s.getId());
                ss.setServiceType(s.getServiceType());
                ss.setScore(getFallbackScore(s.getAvailability()));
                ss.setReason("Score basé sur la disponibilité.");
                allScores.put(compositeKey, ss);
            }
        }

        List<ScoredService> ranked = allScores.values().stream()
                .sorted((a, b) -> Integer.compare(b.getScore(), a.getScore()))
                .collect(Collectors.toList());

        String globalExplanation = generateGlobalExplanation(profile, ranked, allServices);

        AiRecommendationResponse result = new AiRecommendationResponse();
        result.setRankedServices(ranked);
        result.setGlobalExplanation(globalExplanation);

        log.info("✅ Résultat final : {} services ({} inv, {} collab, {} tourist) — top score: {}",
                ranked.size(),
                ranked.stream().filter(s -> "INVESTMENT".equals(s.getServiceType())).count(),
                ranked.stream().filter(s -> "COLLABORATION".equals(s.getServiceType())).count(),
                ranked.stream().filter(s -> "TOURIST".equals(s.getServiceType())).count(),
                ranked.isEmpty() ? 0 : ranked.get(0).getScore());

        return result;
    }

    // ════════════════════════════════════════════════════════════
    // APPEL OLLAMA — UN BATCH
    // ════════════════════════════════════════════════════════════

    private List<ScoredService> callOllamaForBatch(
            UserProfile profile,
            List<ServiceSummary> batch,
            Map<String, String> serviceTypeMap,
            Map<Long, String> idToTypeMap
    ) {
        try {
            String prompt = buildBatchPrompt(profile, batch);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", ollamaModel);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            requestBody.put("format", "json");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaBaseUrl + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(90))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Ollama error {}: {}", response.statusCode(), response.body());
                return buildFallbackBatch(batch);
            }

            return parseBatchResponse(response.body(), batch, serviceTypeMap, idToTypeMap);

        } catch (Exception e) {
            log.error("Erreur appel Ollama batch: {}", e.getMessage());
            return buildFallbackBatch(batch);
        }
    }

    // ════════════════════════════════════════════════════════════
    // PROMPT BATCH — adapté selon le rôle
    // ════════════════════════════════════════════════════════════

    private String buildBatchPrompt(UserProfile profile, List<ServiceSummary> batch) {

        String requiredIds = batch.stream()
                .map(s -> String.valueOf(s.getId()))
                .collect(Collectors.joining(", "));

        StringBuilder sb = new StringBuilder();
        sb.append("You are a scoring assistant. Respond ONLY with a JSON object, no other text.\n\n");

        sb.append("USER PROFILE:\n");
        sb.append("role: ").append(profile.role).append("\n");
        if (profile.activitySector != null) sb.append("sector: ").append(profile.activitySector).append("\n");
        if (profile.originCountry != null)  sb.append("country/nationality: ").append(profile.originCountry).append("\n");
        if (profile.company != null)        sb.append("company: ").append(profile.company).append("\n");
        sb.append("\n");

        sb.append("SERVICES:\n");
        for (ServiceSummary s : batch) {
            sb.append("- id:").append(s.getId())
                    .append(" type:").append(s.getServiceType())
                    .append(" name:\"").append(s.getName()).append("\"");
            if (s.getActivityDomain() != null) sb.append(" domain:").append(s.getActivityDomain());
            if (s.getAvailability() != null)   sb.append(" avail:").append(s.getAvailability());
            if (s.getRegion() != null)         sb.append(" region:").append(s.getRegion());
            // Champs spécifiques tourist
            if (s.getCategory() != null)       sb.append(" category:").append(s.getCategory());
            if (s.getDurationHours() != null)  sb.append(" duration:").append(s.getDurationHours()).append("h");
            if (s.getAvailableLanguages() != null && !s.getAvailableLanguages().isEmpty())
                sb.append(" languages:").append(String.join(",", s.getAvailableLanguages()));
            sb.append("\n");
        }

        sb.append("\nSCORING RULES (0-100):\n");

        // Règles de scoring adaptées selon le type d'utilisateur
        if ("TOURIST".equals(profile.role)) {
            sb.append("- Language match with tourist nationality = most important (+40 pts)\n");
            sb.append("- IMMEDIATE availability = +30 pts, ON_DEMAND = +15 pts, UPCOMING = +5 pts\n");
            sb.append("- Category relevance (HOTEL, RESTAURANT, GUIDE, ACTIVITY...) = +20 pts\n");
            sb.append("- Geographic region preference = +10 pts\n\n");
        } else {
            sb.append("- Sector match with user profile = most important (+40 pts)\n");
            sb.append("- IMMEDIATE availability = +30 pts, ON_DEMAND = +15 pts, UPCOMING = +5 pts\n");
            sb.append("- Geographic/regional proximity = +20 pts\n");
            sb.append("- Other relevance = +10 pts\n\n");
        }

        sb.append("MANDATORY: Your response MUST contain scores for ALL of these IDs: ")
                .append(requiredIds).append("\n");
        sb.append("Do NOT skip any ID. Do NOT add IDs not in the list above.\n\n");

        sb.append("REQUIRED JSON FORMAT:\n");
        sb.append("{\"scores\":[\n");
        for (int i = 0; i < batch.size(); i++) {
            ServiceSummary s = batch.get(i);
            sb.append("  {\"serviceId\":").append(s.getId())
                    .append(",\"score\":75,\"reason\":\"reason here\"}");
            if (i < batch.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]}");

        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════
    // PARSING RÉPONSE BATCH
    // ════════════════════════════════════════════════════════════

    private List<ScoredService> parseBatchResponse(
            String responseBody,
            List<ServiceSummary> batch,
            Map<String, String> serviceTypeMap,
            Map<Long, String> idToTypeMap
    ) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root.path("response").asText();

            content = content.trim()
                    .replaceAll("(?s)^```json\\s*", "")
                    .replaceAll("(?s)^```\\s*", "")
                    .replaceAll("(?s)\\s*```$", "");

            JsonNode parsed     = objectMapper.readTree(content);
            JsonNode scoresNode = parsed.path("scores");

            List<ScoredService> result = new ArrayList<>();
            Set<String> parsedKeys = new HashSet<>();

            if (scoresNode.isArray()) {
                for (JsonNode node : scoresNode) {
                    long serviceId = node.path("serviceId").asLong();
                    String serviceType = findTypeInBatch(serviceId, batch);

                    if (serviceType == null) {
                        log.warn("⚠️ Hallucination — serviceId={} inconnu dans ce batch, ignoré", serviceId);
                        continue;
                    }

                    String compositeKey = serviceType + "_" + serviceId;
                    if (parsedKeys.contains(compositeKey)) continue;

                    ScoredService ss = new ScoredService();
                    ss.setServiceId(serviceId);
                    ss.setServiceType(serviceType);
                    ss.setScore(Math.min(100, Math.max(0, node.path("score").asInt(0))));
                    ss.setReason(node.path("reason").asText(""));
                    result.add(ss);
                    parsedKeys.add(compositeKey);
                }
            }

            int forgotten = 0;
            for (ServiceSummary s : batch) {
                String compositeKey = s.getServiceType() + "_" + s.getId();
                if (!parsedKeys.contains(compositeKey)) {
                    forgotten++;
                    ScoredService ss = new ScoredService();
                    ss.setServiceId(s.getId());
                    ss.setServiceType(s.getServiceType());
                    ss.setScore(getFallbackScore(s.getAvailability()));
                    ss.setReason("Score basé sur la disponibilité.");
                    result.add(ss);
                }
            }

            if (forgotten > 0) log.warn("    ⚠️ {}/{} services oubliés par le LLM", forgotten, batch.size());
            log.info("    ✅ {}/{} services scorés par l'IA", parsedKeys.size(), batch.size());

            return result;

        } catch (Exception e) {
            log.error("Erreur parsing batch: {}", e.getMessage());
            return buildFallbackBatch(batch);
        }
    }

    private String findTypeInBatch(long serviceId, List<ServiceSummary> batch) {
        return batch.stream()
                .filter(s -> s.getId() == serviceId)
                .map(ServiceSummary::getServiceType)
                .findFirst()
                .orElse(null);
    }

    // ════════════════════════════════════════════════════════════
    // GLOBAL EXPLANATION
    // ════════════════════════════════════════════════════════════

    private String generateGlobalExplanation(
            UserProfile profile,
            List<ScoredService> ranked,
            List<ServiceSummary> allServices
    ) {
        try {
            Map<String, String> serviceNames = allServices.stream()
                    .collect(Collectors.toMap(
                            s -> s.getServiceType() + "_" + s.getId(),
                            ServiceSummary::getName,
                            (existing, replacement) -> existing
                    ));

            String top3Context = ranked.stream().limit(3)
                    .map(s -> {
                        String key = s.getServiceType() + "_" + s.getServiceId();
                        return "\"" + serviceNames.getOrDefault(key, "id=" + s.getServiceId())
                                + "\" (" + s.getServiceType() + ", score=" + s.getScore() + ")";
                    })
                    .collect(Collectors.joining(", "));

            String prompt = "Write a friendly 2-sentence recommendation intro in English.\n" +
                    "User: role=" + profile.role +
                    (profile.firstName != null ? ", name=" + profile.firstName + " " + profile.lastName : "") +
                    (profile.activitySector != null ? ", sector=" + profile.activitySector : "") +
                    (profile.originCountry != null ? ", nationality=" + profile.originCountry : "") +
                    (profile.company != null ? ", company=" + profile.company : "") + "\n" +
                    "Top recommended services: " + top3Context + "\n" +
                    "Write 2 sentences mentioning the user's profile and the top service name.\n" +
                    "Respond ONLY with JSON: {\"globalExplanation\":\"sentence1. sentence2.\"}";

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", ollamaModel);
            requestBody.put("prompt", prompt);
            requestBody.put("stream", false);
            requestBody.put("format", "json");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ollamaBaseUrl + "/api/generate"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                String content = root.path("response").asText().trim()
                        .replaceAll("(?s)^```json\\s*", "")
                        .replaceAll("(?s)^```\\s*", "")
                        .replaceAll("(?s)\\s*```$", "");
                JsonNode parsed = objectMapper.readTree(content);
                String explanation = parsed.path("globalExplanation").asText("").trim();
                if (!explanation.isBlank() && explanation.length() > 20) {
                    log.info("✅ globalExplanation générée par l'IA");
                    return explanation;
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Impossible de générer globalExplanation via Ollama: {}", e.getMessage());
        }

        return buildSmartFallbackExplanation(profile, ranked, allServices);
    }

    private String buildSmartFallbackExplanation(
            UserProfile profile,
            List<ScoredService> ranked,
            List<ServiceSummary> allServices
    ) {
        Map<String, String> serviceNames = allServices.stream()
                .collect(Collectors.toMap(
                        s -> s.getServiceType() + "_" + s.getId(),
                        ServiceSummary::getName,
                        (existing, replacement) -> existing
                ));

        String firstName = profile.firstName != null ? profile.firstName : "";
        String sector    = profile.activitySector != null
                ? profile.activitySector.replace("_", " ").toLowerCase() : "your sector";
        String country   = profile.originCountry != null ? profile.originCountry : "your country";

        String topService = "the available services";
        if (!ranked.isEmpty()) {
            ScoredService top = ranked.get(0);
            String key = top.getServiceType() + "_" + top.getServiceId();
            topService = "\"" + serviceNames.getOrDefault(key, "top service") + "\"";
        }

        String intro = firstName.isBlank() ? "Based on your profile" : "Hello " + firstName;

        return switch (profile.role.toUpperCase()) {
            case "INVESTOR" ->
                    intro + ", we identified the best investment opportunities in Tunisia matching your " +
                            sector + " background. " + topService + " ranks highest with a strong sector alignment.";
            case "ECONOMIC_PARTNER" ->
                    intro + ", here are the collaboration services in Tunisia best suited for a " +
                            sector + " partner from " + country + ". " +
                            topService + " is your top match based on sector and availability.";
            case "INTERNATIONAL_COMPANY" ->
                    intro + ", as an international company from " + country +
                            " active in " + sector + ", we selected the best investment and collaboration services in Tunisia. " +
                            topService + " is your highest-scored opportunity.";
            case "TOURIST" ->                                              // ← AJOUT
                    intro + ", welcome to Tunisia! Based on your nationality (" + country +
                            "), here are the tourist services we recommend for you. " +
                            topService + " is your top match.";
            default ->
                    intro + ", here are the services best suited to your profile in Tunisia. " +
                            topService + " is your top recommendation.";
        };
    }

    // ════════════════════════════════════════════════════════════
    // CONVERSION ENTITÉS → ServiceSummary
    // ════════════════════════════════════════════════════════════

    private ServiceSummary toSummary(InvestmentService s) {
        ServiceSummary dto = new ServiceSummary();
        dto.setId(s.getId());
        dto.setServiceType("INVESTMENT");
        dto.setName(s.getName());
        dto.setDescription(s.getDescription());
        dto.setActivityDomain(s.getEconomicSector() != null ? s.getEconomicSector().getName() : null);
        dto.setRegion(s.getRegion() != null ? s.getRegion().getName() : null);
        dto.setAvailability(s.getAvailability() != null ? s.getAvailability().name() : null);
        dto.setBudget(s.getMinimumAmount());
        return dto;
    }

    private ServiceSummary toSummary(CollaborationService s) {
        ServiceSummary dto = new ServiceSummary();
        dto.setId(s.getId());
        dto.setServiceType("COLLABORATION");
        dto.setName(s.getName());
        dto.setDescription(s.getDescription());
        dto.setActivityDomain(s.getActivityDomain() != null ? s.getActivityDomain().name() : null);
        dto.setRegion(s.getRegion() != null ? s.getRegion().getName() : null);
        dto.setAvailability(s.getAvailability() != null ? s.getAvailability().name() : null);
        dto.setBudget(s.getRequestedBudget());
        dto.setCollaborationType(s.getCollaborationType() != null ? s.getCollaborationType().name() : null);
        dto.setRequiredSkills(s.getRequiredSkills());
        dto.setExpectedBenefits(s.getExpectedBenefits());
        return dto;
    }

    // ── TOURIST ──────────────────────────────────────────────────
    private ServiceSummary toTouristSummary(TouristService s) {
        ServiceSummary dto = new ServiceSummary();
        dto.setId(s.getId());
        dto.setServiceType("TOURIST");
        dto.setName(s.getName());
        dto.setDescription(s.getDescription());
        dto.setRegion(s.getRegion() != null ? s.getRegion().getName() : null);
        dto.setAvailability(s.getAvailability() != null ? s.getAvailability().name() : null);
        dto.setBudget(s.getPrice());
        dto.setCategory(s.getCategory() != null ? s.getCategory().name() : null);
        dto.setTargetAudience(s.getTargetAudience() != null ? s.getTargetAudience().name() : null);
        dto.setDurationHours(s.getDurationHours());
        dto.setMaxCapacity(s.getMaxCapacity());
        dto.setAvailableLanguages(s.getAvailableLanguages());
        dto.setIncludedServices(s.getIncludedServices());
        return dto;
    }

    // ════════════════════════════════════════════════════════════
    // FALLBACKS
    // ════════════════════════════════════════════════════════════

    private List<ScoredService> buildFallbackBatch(List<ServiceSummary> batch) {
        return batch.stream().map(s -> {
            ScoredService ss = new ScoredService();
            ss.setServiceId(s.getId());
            ss.setServiceType(s.getServiceType());
            ss.setScore(getFallbackScore(s.getAvailability()));
            ss.setReason("Score basé sur la disponibilité (IA indisponible).");
            return ss;
        }).collect(Collectors.toList());
    }

    private int getFallbackScore(String availability) {
        if (availability == null) return 30;
        return switch (availability) {
            case "IMMEDIATE" -> 70;
            case "ON_DEMAND" -> 50;
            default -> 30;
        };
    }

    // ════════════════════════════════════════════════════════════
    // CLASSE INTERNE
    // ════════════════════════════════════════════════════════════

    private static class UserProfile {
        String role, email, firstName, lastName, company, activitySector, originCountry;
    }
}

