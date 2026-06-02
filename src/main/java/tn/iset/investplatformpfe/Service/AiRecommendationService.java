package tn.iset.investplatformpfe.Service;

import tn.iset.investplatformpfe.Dto.AiRecommendationDTO.AiRecommendationResponse;
import tn.iset.investplatformpfe.Dto.AiRecommendationDTO.ScoredService;
import tn.iset.investplatformpfe.Dto.AiRecommendationDTO.ServiceSummary;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import tn.iset.investplatformpfe.Entity.TouristService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Dto.AiRecommendationDTO.*;
import tn.iset.investplatformpfe.Entity.*;
import tn.iset.investplatformpfe.Repository.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class AiRecommendationService {

    private static final Logger log = LoggerFactory.getLogger(AiRecommendationService.class);
    private static final int BATCH_SIZE = 5;
    private static final int MIN_SCORE_TO_INCLUDE = 40;

    // ── Groq config (compatible OpenAI) ──
    @Value("${openai.base-url:https://api.groq.com/openai}")
    private String groqBaseUrl;

    @Value("${openai.model:llama3-8b-8192}")
    private String groqModel;

    @Value("${openai.api-key}")
    private String groqApiKey;

    private final InvestorRepository investorRepository;
    private final EconomicPartnerRepository economicPartnerRepository;
    private final InternationalCompanyRepository internationalCompanyRepository;
    private final InvestmentServiceRepository investmentServiceRepository;
    private final CollaborationServiceRepository collaborationServiceRepository;
    private final TouristRepository touristRepository;
    private final TouristServiceRepository touristServiceRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ExecutorService batchExecutor = Executors.newFixedThreadPool(4);

    public AiRecommendationService(
            InvestorRepository investorRepository,
            EconomicPartnerRepository economicPartnerRepository,
            InternationalCompanyRepository internationalCompanyRepository,
            InvestmentServiceRepository investmentServiceRepository,
            CollaborationServiceRepository collaborationServiceRepository,
            TouristRepository touristRepository,
            TouristServiceRepository touristServiceRepository) {
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

        // 1. Construire le profil complet
        UserProfile profile = buildUserProfile(userEmail, userRole);
        log.info("👤 Profil → role={} sector={} country={} company={}",
                profile.role, profile.activitySector, profile.originCountry, profile.company);

        // 2. Charger les services selon le rôle
        List<ServiceSummary> allServices = loadServicesByRole(userRole);
        log.info("📦 {} services chargés avant pré-filtrage", allServices.size());

        if (allServices.isEmpty()) {
            return emptyResponse("Aucun service disponible pour le moment.");
        }

        // 3. Pré-filtrage par profil
        List<ServiceSummary> preFiltered = preFilterByProfile(allServices, profile);
        log.info("🔎 {} services après pré-filtrage profil (éliminé: {})",
                preFiltered.size(), allServices.size() - preFiltered.size());

        List<ServiceSummary> candidates = preFiltered.size() >= 3 ? preFiltered : allServices;

        // 4. Score par batch Groq
        Map<Long, String> idToTypeMap = candidates.stream()
                .collect(Collectors.toMap(ServiceSummary::getId,
                        ServiceSummary::getServiceType, (a, b) -> a));

        List<ScoredService> allScored = scoreBatchedParallel(profile, candidates, idToTypeMap);

        // 5. Filtre final
        List<ScoredService> matched = allScored.stream()
                .filter(s -> s.getScore() >= MIN_SCORE_TO_INCLUDE)
                .sorted((a, b) -> Integer.compare(b.getScore(), a.getScore()))
                .limit(10)
                .collect(Collectors.toList());

        log.info("✅ {} services matchent le profil (score >= {})", matched.size(), MIN_SCORE_TO_INCLUDE);

        if (matched.isEmpty()) {
            return emptyResponse(
                    "No services closely match your profile at this time. " +
                            "Try expanding your preferences or check back later.");
        }

        // 6. Explication globale
        String globalExplanation = generateGlobalExplanation(profile, matched, candidates);

        AiRecommendationResponse result = new AiRecommendationResponse();
        result.setRankedServices(matched);
        result.setGlobalExplanation(globalExplanation);
        return result;
    }

    // ════════════════════════════════════════════════════════════
    // PRÉ-FILTRAGE PAR PROFIL
    // ════════════════════════════════════════════════════════════

    private List<ServiceSummary> preFilterByProfile(List<ServiceSummary> services, UserProfile profile) {
        if (profile.activitySector == null) {
            log.info("⚠️ Pas de secteur → pas de pré-filtrage");
            return services;
        }

        List<ServiceSummary> filtered = services.stream()
                .filter(s -> s.getActivityDomain() == null ||
                        sectorMatches(profile.activitySector, s.getActivityDomain()))
                .collect(Collectors.toList());

        if (filtered.size() < 5) {
            log.info("⚠️ Pré-filtrage trop restrictif ({}) → on garde tout", filtered.size());
            return services;
        }

        return filtered;
    }

    private boolean sectorMatches(String profileSector, String serviceDomain) {
        if (profileSector == null || serviceDomain == null) return false;

        String ps = profileSector.toUpperCase().replace("_", " ").replace("-", " ");
        String sd = serviceDomain.toUpperCase().replace("_", " ").replace("-", " ");

        if (ps.equals(sd)) return true;
        if (ps.contains(sd) || sd.contains(ps)) return true;

        Map<String, List<String>> sectorAliases = buildSectorAliases();
        for (Map.Entry<String, List<String>> entry : sectorAliases.entrySet()) {
            String canonical = entry.getKey();
            List<String> aliases = entry.getValue();
            boolean psMatches = ps.contains(canonical) || aliases.stream().anyMatch(ps::contains);
            boolean sdMatches = sd.contains(canonical) || aliases.stream().anyMatch(sd::contains);
            if (psMatches && sdMatches) return true;
        }

        return false;
    }

    private Map<String, List<String>> buildSectorAliases() {
        Map<String, List<String>> m = new HashMap<>();
        m.put("TECHNOLOGY",   List.of("TECH", "IT", "INFORMAT", "DIGIT", "LOGIC", "SOFTWARE", "NUMERIC"));
        m.put("AGRICULTURE",  List.of("AGRI", "AGRO", "ALIMENTAIRE", "FOOD", "FARM", "AGRI FOOD"));
        m.put("TOURISM",      List.of("TOUR", "HOTEL", "HOSPITALITY", "HOSPIT", "TRAVEL"));
        m.put("INDUSTRY",     List.of("INDUS", "MANUFACTUR", "FABRI", "PRODUCTION"));
        m.put("ENERGY",       List.of("ENERG", "SOLAR", "SOLAIRE", "RENOUV", "RENEWABLE", "GREEN"));
        m.put("FINANCE",      List.of("FINANC", "BANQUE", "BANK", "ASSUR", "INSURANCE", "INVEST"));
        m.put("HEALTH",       List.of("SANT", "HEALTH", "MEDICAL", "PHARMA", "MEDIC"));
        m.put("EDUCATION",    List.of("EDUC", "FORM", "TRAINING", "SCHOOL", "LEARN"));
        m.put("CONSTRUCTION", List.of("CONSTRU", "IMMOB", "BTP", "REAL ESTATE", "BUILDING"));
        m.put("TEXTILE",      List.of("TEXTILE", "HABILLEMENT", "FASHION", "CLOTH"));
        m.put("TRADE",        List.of("COMMERC", "SERVICE", "TRADE", "RETAIL", "COMMERCE"));
        m.put("TRANSPORT",    List.of("TRANSPORT", "LOGISTIC", "LOGISTIQ", "SHIPPING"));
        m.put("ENVIRONMENT",  List.of("ENVIRON", "ECOLOGY", "ECOLOG", "SUSTAIN"));
        return m;
    }

    // ════════════════════════════════════════════════════════════
    // CHARGEMENT DES SERVICES
    // ════════════════════════════════════════════════════════════

    private List<ServiceSummary> loadServicesByRole(String userRole) {
        List<ServiceSummary> result = new ArrayList<>();
        switch (userRole.toUpperCase()) {
            case "INVESTOR" ->
                    investmentServiceRepository.findByStatus(ServiceStatus.APPROVED)
                            .forEach(s -> result.add(toInvestmentSummary(s)));
            case "ECONOMIC_PARTNER" ->
                    collaborationServiceRepository.findByStatus(ServiceStatus.APPROVED)
                            .forEach(s -> result.add(toCollaborationSummary(s)));
            case "INTERNATIONAL_COMPANY" -> {
                investmentServiceRepository.findByStatus(ServiceStatus.APPROVED)
                        .forEach(s -> result.add(toInvestmentSummary(s)));
                collaborationServiceRepository.findByStatus(ServiceStatus.APPROVED)
                        .forEach(s -> result.add(toCollaborationSummary(s)));
            }
            case "TOURIST" ->
                    touristServiceRepository.findByStatus(ServiceStatus.APPROVED)
                            .forEach(s -> result.add(toTouristSummary(s)));
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════
    // SCORING PARALLÈLE PAR BATCH
    // ════════════════════════════════════════════════════════════

    private List<ScoredService> scoreBatchedParallel(
            UserProfile profile,
            List<ServiceSummary> services,
            Map<Long, String> idToTypeMap) {

        List<List<ServiceSummary>> batches = new ArrayList<>();
        for (int i = 0; i < services.size(); i += BATCH_SIZE) {
            batches.add(services.subList(i, Math.min(i + BATCH_SIZE, services.size())));
        }

        log.info("🔀 {} services → {} batch(es) PARALLÈLE", services.size(), batches.size());

        List<CompletableFuture<List<ScoredService>>> futures = new ArrayList<>();
        for (List<ServiceSummary> batch : batches) {
            futures.add(CompletableFuture.supplyAsync(
                    () -> callGroqForBatch(profile, batch), batchExecutor));
        }

        Map<String, ScoredService> allScores = new LinkedHashMap<>();
        for (CompletableFuture<List<ScoredService>> future : futures) {
            try {
                future.get(60, TimeUnit.SECONDS)
                        .forEach(ss -> allScores.put(ss.getServiceType() + "_" + ss.getServiceId(), ss));
            } catch (Exception e) {
                log.error("❌ Batch error: {}", e.getMessage());
            }
        }

        // Fallback pour les services non scorés
        for (ServiceSummary s : services) {
            String key = s.getServiceType() + "_" + s.getId();
            if (!allScores.containsKey(key)) {
                ScoredService ss = new ScoredService();
                ss.setServiceId(s.getId());
                ss.setServiceType(s.getServiceType());
                ss.setScore(getFallbackScore(s.getAvailability()));
                ss.setReason("Score de disponibilité (IA indisponible).");
                allScores.put(key, ss);
            }
        }

        return new ArrayList<>(allScores.values());
    }

    // ════════════════════════════════════════════════════════════
    // APPEL GROQ — UN BATCH  (endpoint /v1/chat/completions)
    // ════════════════════════════════════════════════════════════

    private List<ScoredService> callGroqForBatch(UserProfile profile, List<ServiceSummary> batch) {
        try {
            String userPrompt = buildPreciseBatchPrompt(profile, batch);

            // Construire le corps de la requête au format OpenAI chat/completions
            Map<String, Object> systemMsg = Map.of(
                    "role", "system",
                    "content", "You are a precise service-matching engine. " +
                            "Always respond ONLY with valid JSON, no markdown, no explanation outside JSON."
            );
            Map<String, Object> userMsg = Map.of("role", "user", "content", userPrompt);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", groqModel);
            requestBody.put("messages", List.of(systemMsg, userMsg));
            requestBody.put("temperature", 0.2);
            requestBody.put("max_tokens", 1024);
            // Groq supporte response_format json_object
            requestBody.put("response_format", Map.of("type", "json_object"));

            log.info("📤 Envoi batch de {} services à Groq (model={})", batch.size(), groqModel);
            long start = System.currentTimeMillis();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(groqBaseUrl + "/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + groqApiKey)
                    .timeout(Duration.ofSeconds(55))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("📥 Groq répondu en {}ms — status={}", System.currentTimeMillis() - start, response.statusCode());

            if (response.statusCode() != 200) {
                log.error("Groq error {}: {}", response.statusCode(), response.body());
                return buildFallbackBatch(batch);
            }

            String rawBody = response.body();
            log.debug("🔍 Réponse brute Groq: {}", rawBody.substring(0, Math.min(500, rawBody.length())));

            return parseGroqBatchResponse(rawBody, batch);

        } catch (Exception e) {
            log.error("❌ Erreur appel Groq: {}", e.getMessage());
            return buildFallbackBatch(batch);
        }
    }

    // ════════════════════════════════════════════════════════════
    // PROMPT PRÉCIS
    // ════════════════════════════════════════════════════════════

    private String buildPreciseBatchPrompt(UserProfile profile, List<ServiceSummary> batch) {

        String requiredIds = batch.stream()
                .map(s -> String.valueOf(s.getId()))
                .collect(Collectors.joining(", "));

        StringBuilder sb = new StringBuilder();
        sb.append("Score how well each service matches THIS specific user profile.\n");
        sb.append("Respond ONLY with valid JSON. No text outside the JSON.\n\n");

        sb.append("=== USER PROFILE ===\n");
        sb.append("Role: ").append(profile.role).append("\n");
        if (profile.firstName != null)
            sb.append("Name: ").append(profile.firstName).append(" ").append(profile.lastName).append("\n");
        if (profile.activitySector != null)
            sb.append("Activity sector: ").append(profile.activitySector).append("\n");
        if (profile.originCountry != null)
            sb.append("Country: ").append(profile.originCountry).append("\n");
        if (profile.company != null)
            sb.append("Company: ").append(profile.company).append("\n");
        sb.append("\n");

        sb.append("=== SERVICES TO SCORE ===\n");
        for (ServiceSummary s : batch) {
            sb.append("SERVICE id:").append(s.getId())
                    .append(" | type:").append(s.getServiceType())
                    .append(" | name:\"").append(s.getName()).append("\"");
            if (s.getActivityDomain() != null)
                sb.append(" | domain:").append(s.getActivityDomain());
            if (s.getRegion() != null)
                sb.append(" | region:").append(s.getRegion());
            if (s.getAvailability() != null)
                sb.append(" | availability:").append(s.getAvailability());
            if (s.getCollaborationType() != null)
                sb.append(" | collabType:").append(s.getCollaborationType());
            if (s.getCategory() != null)
                sb.append(" | category:").append(s.getCategory());
            if (s.getAvailableLanguages() != null && !s.getAvailableLanguages().isEmpty())
                sb.append(" | languages:").append(String.join(",", s.getAvailableLanguages()));
            if (s.getDescription() != null && !s.getDescription().isEmpty())
                sb.append(" | desc:\"")
                        .append(s.getDescription(), 0, Math.min(80, s.getDescription().length()))
                        .append("\"");
            sb.append("\n");
        }

        sb.append("\n=== SCORING RULES (score 0-100) ===\n");
        sb.append("Use the FULL range:\n");
        sb.append("- 80-100: Excellent match (sector identical, high availability)\n");
        sb.append("- 60-79 : Good match (sector close, or good availability)\n");
        sb.append("- 40-59 : Partial match (one criterion matches)\n");
        sb.append("- 20-39 : Weak match (different sector, low relevance)\n");
        sb.append("- 0-19  : No match (completely unrelated)\n\n");

        switch (profile.role) {
            case "INVESTOR" -> {
                sb.append("For INVESTOR:\n");
                sb.append("- Sector match '").append(profile.activitySector).append("' vs domain = +50pts\n");
                sb.append("- IMMEDIATE availability = +25pts, ON_DEMAND = +15pts\n");
                sb.append("- Sector mismatch = max 30pts total\n");
            }
            case "ECONOMIC_PARTNER" -> {
                sb.append("For ECONOMIC_PARTNER:\n");
                sb.append("- Sector match '").append(profile.activitySector).append("' vs domain = +45pts\n");
                sb.append("- Collaboration type relevance = +25pts\n");
                sb.append("- IMMEDIATE availability = +20pts\n");
                sb.append("- Sector mismatch = max 25pts total\n");
            }
            case "INTERNATIONAL_COMPANY" -> {
                sb.append("For INTERNATIONAL_COMPANY:\n");
                sb.append("- Sector match '").append(profile.activitySector).append("' vs domain = +50pts\n");
                sb.append("- IMMEDIATE availability = +25pts\n");
                sb.append("- International relevance = +15pts\n");
                sb.append("- Sector mismatch = max 30pts total\n");
            }
            case "TOURIST" -> {
                sb.append("For TOURIST:\n");
                if (profile.originCountry != null)
                    sb.append("- Language match with '").append(profile.originCountry).append("' = +40pts\n");
                sb.append("- IMMEDIATE availability = +30pts\n");
                sb.append("- Category relevance = +20pts\n");
                sb.append("- Region = +10pts\n");
            }
        }

        sb.append("\nIMPORTANT: Score honestly. Sector mismatch → score < 30.\n");
        sb.append("MANDATORY: Score ALL these IDs: ").append(requiredIds).append("\n\n");

        sb.append("REQUIRED JSON FORMAT (return exactly this structure):\n");
        sb.append("{\"scores\":[\n");
        for (int i = 0; i < batch.size(); i++) {
            ServiceSummary s = batch.get(i);
            sb.append("  {\"serviceId\":").append(s.getId())
                    .append(",\"score\":65,\"reason\":\"specific reason mentioning sector/availability\"}");
            if (i < batch.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]}");

        return sb.toString();
    }

    // ════════════════════════════════════════════════════════════
    // PARSING RÉPONSE GROQ
    // ════════════════════════════════════════════════════════════

    private List<ScoredService> parseGroqBatchResponse(String responseBody, List<ServiceSummary> batch) {
        try {
            // Structure OpenAI: choices[0].message.content
            JsonNode root = objectMapper.readTree(responseBody);
            String content = root
                    .path("choices").get(0)
                    .path("message")
                    .path("content")
                    .asText("")
                    .trim()
                    .replaceAll("(?s)^```json\\s*", "")
                    .replaceAll("(?s)^```\\s*", "")
                    .replaceAll("(?s)\\s*```$", "");

            JsonNode parsed = objectMapper.readTree(content);
            JsonNode scoresNode = parsed.path("scores");

            List<ScoredService> result = new ArrayList<>();
            Set<String> parsedKeys = new HashSet<>();

            if (scoresNode.isArray()) {
                for (JsonNode node : scoresNode) {
                    long serviceId = node.path("serviceId").asLong();
                    String serviceType = findTypeInBatch(serviceId, batch);
                    if (serviceType == null) continue;

                    String key = serviceType + "_" + serviceId;
                    if (parsedKeys.contains(key)) continue;

                    ScoredService ss = new ScoredService();
                    ss.setServiceId(serviceId);
                    ss.setServiceType(serviceType);
                    ss.setScore(Math.min(100, Math.max(0, node.path("score").asInt(0))));
                    ss.setReason(node.path("reason").asText(""));
                    result.add(ss);
                    parsedKeys.add(key);
                }
            }

            // Fallback pour les oubliés
            for (ServiceSummary s : batch) {
                String key = s.getServiceType() + "_" + s.getId();
                if (!parsedKeys.contains(key)) {
                    ScoredService ss = new ScoredService();
                    ss.setServiceId(s.getId());
                    ss.setServiceType(s.getServiceType());
                    ss.setScore(getFallbackScore(s.getAvailability()));
                    ss.setReason("Score de disponibilité.");
                    result.add(ss);
                }
            }

            log.info("    ✅ {}/{} scorés par Groq", parsedKeys.size(), batch.size());
            return result;

        } catch (Exception e) {
            log.error("Erreur parsing réponse Groq: {}", e.getMessage());
            return buildFallbackBatch(batch);
        }
    }

    private String findTypeInBatch(long id, List<ServiceSummary> batch) {
        return batch.stream().filter(s -> s.getId() == id)
                .map(ServiceSummary::getServiceType).findFirst().orElse(null);
    }

    // ════════════════════════════════════════════════════════════
    // PROFIL UTILISATEUR
    // ════════════════════════════════════════════════════════════

    private UserProfile buildUserProfile(String email, String role) {
        UserProfile p = new UserProfile();
        p.role = role;
        p.email = email;

        switch (role.toUpperCase()) {
            case "INVESTOR" -> investorRepository.findByEmail(email).ifPresent(inv -> {
                p.firstName = inv.getFirstName();
                p.lastName = inv.getLastName();
                p.activitySector = inv.getActivitySector() != null ? inv.getActivitySector().name() : null;
                p.originCountry = inv.getOriginCountry();
                p.company = inv.getCompany();
            });
            case "ECONOMIC_PARTNER" -> economicPartnerRepository.findByEmail(email).ifPresent(ep -> {
                p.firstName = ep.getFirstName();
                p.lastName = ep.getLastName();
                p.activitySector = ep.getBusinessSector() != null ? ep.getBusinessSector().name() : null;
                p.originCountry = ep.getCountryOfOrigin();
            });
            case "INTERNATIONAL_COMPANY" -> internationalCompanyRepository.findByEmail(email).ifPresent(ic -> {
                p.firstName = ic.getContactFirstName();
                p.lastName = ic.getContactLastName();
                p.company = ic.getCompanyName();
                p.activitySector = ic.getActivitySector() != null ? ic.getActivitySector().name() : null;
                p.originCountry = ic.getOriginCountry();
            });
            case "TOURIST" -> touristRepository.findByEmail(email).ifPresent(t -> {
                p.firstName = t.getFirstName();
                p.lastName = t.getLastName();
                p.originCountry = t.getNationality();
            });
        }
        return p;
    }

    // ════════════════════════════════════════════════════════════
    // CONVERSIONS ENTITÉS → ServiceSummary
    // ════════════════════════════════════════════════════════════

    private ServiceSummary toInvestmentSummary(InvestmentService s) {
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

    private ServiceSummary toCollaborationSummary(CollaborationService s) {
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
    // EXPLICATION GLOBALE (via Groq)
    // ════════════════════════════════════════════════════════════

    private String generateGlobalExplanation(UserProfile profile,
                                             List<ScoredService> ranked,
                                             List<ServiceSummary> allServices) {
        try {
            Map<String, String> names = allServices.stream().collect(Collectors.toMap(
                    s -> s.getServiceType() + "_" + s.getId(),
                    ServiceSummary::getName, (a, b) -> a));

            String top3 = ranked.stream().limit(3).map(s -> {
                String key = s.getServiceType() + "_" + s.getServiceId();
                String name = names.getOrDefault(key, "service");
                String domain = allServices.stream()
                        .filter(sv -> sv.getServiceType().equals(s.getServiceType())
                                && sv.getId().equals(s.getServiceId()))
                        .map(ServiceSummary::getActivityDomain)
                        .findFirst().orElse("");
                return "\"" + name + "\" (domain=" + domain + ", score=" + s.getScore() + ")";
            }).collect(Collectors.joining("; "));

            String userPrompt =
                    "User profile: role=" + profile.role +
                            (profile.activitySector != null ? ", sector=" + profile.activitySector : "") +
                            (profile.originCountry != null ? ", country=" + profile.originCountry : "") + "\n" +
                            "Top 3 matched services: " + top3 + "\n\n" +
                            "Write exactly 2 sentences in English:\n" +
                            "1. Mention the user's role and sector specifically.\n" +
                            "2. Mention the top service name and WHY it matches (use the domain field).\n" +
                            "Respond ONLY with JSON: {\"globalExplanation\":\"sentence1. sentence2.\"}";

            Map<String, Object> systemMsg = Map.of(
                    "role", "system",
                    "content", "You are a helpful recommendation assistant. Always respond with valid JSON only."
            );
            Map<String, Object> userMsg = Map.of("role", "user", "content", userPrompt);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", groqModel);
            body.put("messages", List.of(systemMsg, userMsg));
            body.put("temperature", 0.3);
            body.put("max_tokens", 256);
            body.put("response_format", Map.of("type", "json_object"));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(groqBaseUrl + "/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + groqApiKey)
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(resp.body());
                String content = root
                        .path("choices").get(0)
                        .path("message")
                        .path("content")
                        .asText("").trim()
                        .replaceAll("(?s)^```json\\s*", "")
                        .replaceAll("(?s)\\s*```$", "");
                JsonNode parsed = objectMapper.readTree(content);
                String expl = parsed.path("globalExplanation").asText("").trim();
                if (!expl.isBlank() && expl.length() > 20) return expl;
            }

        } catch (Exception e) {
            log.warn("⚠️ globalExplanation fallback: {}", e.getMessage());
        }

        return buildFallbackExplanation(profile, ranked, allServices);
    }

    private String buildFallbackExplanation(UserProfile profile,
                                            List<ScoredService> ranked,
                                            List<ServiceSummary> allServices) {
        Map<String, String> names = allServices.stream().collect(Collectors.toMap(
                s -> s.getServiceType() + "_" + s.getId(),
                ServiceSummary::getName, (a, b) -> a));

        String firstName = profile.firstName != null ? profile.firstName : "";
        String sector = profile.activitySector != null
                ? profile.activitySector.replace("_", " ").toLowerCase() : "your sector";
        String topName = ranked.isEmpty() ? "services" :
                names.getOrDefault(
                        ranked.get(0).getServiceType() + "_" + ranked.get(0).getServiceId(),
                        "top service");

        String intro = firstName.isBlank() ? "Based on your profile" : "Hello " + firstName;
        return switch (profile.role.toUpperCase()) {
            case "INVESTOR" -> intro + ", we found investment opportunities that align with your " +
                    sector + " background. \"" + topName + "\" is your strongest match.";
            case "ECONOMIC_PARTNER" -> intro + ", here are collaboration services matching your " +
                    sector + " expertise. \"" + topName + "\" is the best fit for your profile.";
            case "INTERNATIONAL_COMPANY" -> intro + ", as a company in the " + sector +
                    " sector, these services are your top opportunities in Tunisia. \"" + topName + "\" leads the ranking.";
            case "TOURIST" -> intro + ", welcome! Based on your nationality, \"" + topName +
                    "\" is your top recommended experience.";
            default -> intro + ", \"" + topName + "\" is your top recommendation.";
        };
    }

    // ════════════════════════════════════════════════════════════
    // UTILITAIRES
    // ════════════════════════════════════════════════════════════

    private List<ScoredService> buildFallbackBatch(List<ServiceSummary> batch) {
        return batch.stream().map(s -> {
            ScoredService ss = new ScoredService();
            ss.setServiceId(s.getId());
            ss.setServiceType(s.getServiceType());
            ss.setScore(getFallbackScore(s.getAvailability()));
            ss.setReason("Score de disponibilité (IA indisponible).");
            return ss;
        }).collect(Collectors.toList());
    }

    private int getFallbackScore(String availability) {
        if (availability == null) return 25;
        return switch (availability) {
            case "IMMEDIATE" -> 60;
            case "ON_DEMAND" -> 45;
            default -> 25;
        };
    }

    private AiRecommendationResponse emptyResponse(String message) {
        AiRecommendationResponse r = new AiRecommendationResponse();
        r.setRankedServices(Collections.emptyList());
        r.setGlobalExplanation(message);
        return r;
    }

    private static class UserProfile {
        String role, email, firstName, lastName, company, activitySector, originCountry;
    }
}