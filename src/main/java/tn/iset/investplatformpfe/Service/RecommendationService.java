package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Dto.RecommendationRequestDTO;
import tn.iset.investplatformpfe.Dto.RecommendationResponseDTO;
import tn.iset.investplatformpfe.Repository.CollaborationServiceRepository;
import tn.iset.investplatformpfe.Repository.InvestmentServiceRepository;
import tn.iset.investplatformpfe.Repository.TouristServiceRepository;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.iset.investplatformpfe.Entity.ServiceStatus;
import tn.iset.investplatformpfe.Entity.TouristService;
import tn.iset.investplatformpfe.Entity.InvestmentService;
import tn.iset.investplatformpfe.Entity.CollaborationService;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);
    private static final int MAX_CANDIDATES_TO_AI = 40;

    @Autowired private TouristServiceRepository       touristRepo;
    @Autowired private InvestmentServiceRepository    investmentRepo;
    @Autowired private CollaborationServiceRepository collaborationRepo;
    @Autowired private AIEngine                       aiEngine;
    @Autowired private GroqAiClient                  groqAiClient;

    private final ObjectMapper mapper = new ObjectMapper();

    // ================================================================
    //  RÉSULTAT ENRICHI : service + flag "isSuggested" + raison
    // ================================================================
    public static class FilterResult {
        public final List<Object> services;
        public final boolean      isSuggested;   // true = domaine proche, pas exact
        public final String       suggestionNote; // message affiché au frontend

        public FilterResult(List<Object> services, boolean isSuggested, String suggestionNote) {
            this.services       = services;
            this.isSuggested    = isSuggested;
            this.suggestionNote = suggestionNote;
        }
    }

    // ================================================================
    //  POINT D'ENTRÉE PRINCIPAL
    // ================================================================
    public List<RecommendationResponseDTO> recommend(RecommendationRequestDTO dto) {

        if (dto.getUserType() == null) {
            log.warn("userType null dans la requête de recommandation");
            return List.of();
        }

        log.info("Recommandation — userType={}, region={}, budget={}, domain={}",
                dto.getUserType(), dto.getRegionId(), dto.getBudget(), dto.getActivityDomain());

        // 1. Charger tous les candidats APPROVED
        List<Object> allCandidates = fetchCandidates(dto);
        log.info("{} candidats APPROVED chargés", allCandidates.size());

        if (allCandidates.isEmpty()) return List.of();

        // 2. Filtrage IA avec fallback domaine proche
        FilterResult filterResult = filterWithAI(allCandidates, dto);

        if (filterResult.services.isEmpty()) {
            log.warn("Aucun service retenu même après recherche domaines proches → liste vide");
            return List.of();
        }

        log.info("{} services retenus (isSuggested={})", filterResult.services.size(), filterResult.isSuggested);

        // 3. Scorer et trier
        List<RecommendationResponseDTO> scored = filterResult.services.stream()
                .map(service -> {
                    try {
                        AIEngine.AIScoreResult result = aiEngine.computeScoreWithAI(service, dto);
                        int score = Math.max(0, Math.min(10, result.totalScore));

                        // Si suggestion de domaine proche → ajouter note dans l'explication
                        String explanation = result.explanation;
                        if (filterResult.isSuggested && filterResult.suggestionNote != null) {
                            explanation = "💡 " + filterResult.suggestionNote + " — " + explanation;
                        }

                        return new RecommendationResponseDTO(service, score, explanation, result.isAIScored);
                    } catch (Exception e) {
                        log.warn("Erreur scoring : {}", e.getMessage());
                        int ruleScore = aiEngine.computeRuleScore(service, dto);
                        String expl = filterResult.isSuggested
                                ? "💡 " + filterResult.suggestionNote
                                : "Score par règles métier.";
                        return new RecommendationResponseDTO(service, ruleScore, expl, false);
                    }
                })
                .sorted(Comparator.comparingInt(RecommendationResponseDTO::getScore).reversed())
                .limit(10)
                .collect(Collectors.toList());

        scored.stream().limit(3).forEach(r -> {
            Object svc = r.getService();
            String name = svc instanceof TouristService ts ? ts.getName()
                    : svc instanceof InvestmentService is ? (is.getTitle() != null ? is.getTitle() : is.getName())
                    : svc instanceof CollaborationService cs ? cs.getName() : "?";
            log.info("  Score {} | {} {}", r.getScore(), name, filterResult.isSuggested ? "[SUGGÉRÉ]" : "");
        });

        return scored;
    }

    // ================================================================
    //  FILTRAGE IA EN 2 PASSES
    //
    //  Passe 1 — Domaine exact
    //    → Si Groq trouve des services : retourner avec isSuggested=false
    //
    //  Passe 2 — Domaines proches/similaires
    //    → Si Groq ne trouve rien en passe 1, on lui demande
    //      de chercher dans des domaines proches du domaine demandé
    //    → Si Groq trouve : retourner avec isSuggested=true + note explicative
    //
    //  Fallback cascade — Si Groq échoue complètement
    // ================================================================
    private FilterResult filterWithAI(List<Object> candidates, RecommendationRequestDTO dto) {

        List<Object> pool = candidates.size() > MAX_CANDIDATES_TO_AI
                ? candidates.subList(0, MAX_CANDIDATES_TO_AI)
                : candidates;

        // ── PASSE 1 : Recherche domaine exact ────────────────────────
        try {
            String systemPrompt = buildFilterSystemPrompt(false);
            String userMessage  = buildFilterUserMessage(pool, dto, false);

            log.info("PASSE 1 — Envoi de {} services à Groq (domaine exact)...", pool.size());
            long start = System.currentTimeMillis();

            String rawResponse = groqAiClient.chat(systemPrompt, userMessage);
            log.info("Groq a répondu en {}ms", System.currentTimeMillis() - start);

            GroqFilterResponse response = parseGroqFilterResponse(rawResponse, pool);

            if (!response.selectedIds.isEmpty()) {
                List<Object> selected = mapIdsToServices(response.selectedIds, pool);
                if (selected.size() >= 2) {
                    log.info("PASSE 1 réussie : {} services retenus", selected.size());
                    return new FilterResult(selected, false, null);
                }
            }

            log.info("PASSE 1 : aucun résultat exact → démarrage PASSE 2 (domaines proches)...");

        } catch (Exception e) {
            log.error("Erreur PASSE 1: {} → tentative PASSE 2", e.getMessage());
        }

        // ── PASSE 2 : Recherche domaines proches ─────────────────────
        // Seulement si un domaine était spécifié (sinon pas de sens)
        if (dto.getActivityDomain() != null) {
            try {
                String systemPrompt = buildFilterSystemPrompt(true);
                String userMessage  = buildFilterUserMessage(pool, dto, true);

                log.info("PASSE 2 — Recherche domaines proches de {}...", dto.getActivityDomain().name());
                long start = System.currentTimeMillis();

                String rawResponse = groqAiClient.chat(systemPrompt, userMessage);
                log.info("Groq passe 2 a répondu en {}ms", System.currentTimeMillis() - start);

                GroqFilterResponse response = parseGroqFilterResponse(rawResponse, pool);

                if (!response.selectedIds.isEmpty()) {
                    List<Object> selected = mapIdsToServices(response.selectedIds, pool);
                    if (selected.size() >= 2) {
                        String note = buildSuggestionNote(dto.getActivityDomain().name(), response.reasoning);
                        log.info("PASSE 2 réussie : {} services de domaines proches", selected.size());
                        return new FilterResult(selected, true, note);
                    }
                }

                log.info("PASSE 2 : toujours aucun résultat → fallback cascade");

            } catch (Exception e) {
                log.error("Erreur PASSE 2: {}", e.getMessage());
            }
        }

        // ── FALLBACK CASCADE ─────────────────────────────────────────
        log.info("Fallback filtre cascade manuel activé");
        List<Object> fallback = fallbackCascadeFilter(candidates, dto);
        return new FilterResult(fallback, false, null);
    }

    // ================================================================
    //  PROMPT SYSTÈME
    //  isSimilarSearch=false → cherche le domaine exact
    //  isSimilarSearch=true  → cherche des domaines proches/similaires
    // ================================================================
    private String buildFilterSystemPrompt(boolean isSimilarSearch) {
        if (!isSimilarSearch) {
            return """
                    Tu es un moteur de sélection de services d'investissement et tourisme pour la Tunisie.
                    Sélectionne les services qui correspondent EXACTEMENT au profil utilisateur.
                    
                    RÈGLES :
                    - Sélectionne entre 5 et 15 services (les plus pertinents)
                    - Priorité absolue : adéquation exacte secteur/domaine avec le profil
                    - Si domaine précisé → sélectionne UNIQUEMENT les services de ce domaine exact
                    - Tiens compte du budget et de la région si fournis
                    
                    RÉPONSE : JSON valide uniquement, sans markdown :
                    {"selectedIds": [id1, id2, ...], "reasoning": "explication courte"}
                    Si aucun service ne correspond exactement : {"selectedIds": [], "reasoning": "raison"}
                    """;
        } else {
            return """
                    Tu es un moteur de recommandation de services pour la Tunisie.
                    Le domaine exact demandé n'a pas de services disponibles.
                    Tu dois maintenant chercher des services dans des domaines PROCHES ou SIMILAIRES.
                    
                    RÈGLES POUR DOMAINES PROCHES :
                    - Cherche des domaines qui ont une forte relation sémantique ou économique avec le domaine demandé
                    - Exemples de proximité :
                      * MANUFACTURING ↔ INDUSTRY, TEXTILE, CONSTRUCTION
                      * IT ↔ TECHNOLOGY, FINANCE, EDUCATION
                      * AGRICULTURE ↔ AGRI_FOOD, ENERGY (bioénergie)
                      * TOURISM ↔ HOTEL, TRANSPORT, RESTAURANT, CRAFTS
                      * ENERGY ↔ RENEWABLE_ENERGY, INDUSTRY, CONSTRUCTION
                      * HEALTH ↔ EDUCATION, TECHNOLOGY
                      * FINANCE ↔ TRADE, SERVICES, TECHNOLOGY
                    - Sélectionne 5 à 10 services des domaines les plus proches
                    - Explique clairement dans "reasoning" quels domaines proches tu as choisis et pourquoi
                    
                    RÉPONSE : JSON valide uniquement, sans markdown :
                    {"selectedIds": [id1, id2, ...], "reasoning": "explication des domaines proches sélectionnés"}
                    Si vraiment aucun domaine proche : {"selectedIds": [], "reasoning": "raison"}
                    """;
        }
    }

    // ================================================================
    //  MESSAGE UTILISATEUR
    // ================================================================
    private String buildFilterUserMessage(List<Object> candidates, RecommendationRequestDTO dto, boolean isSimilarSearch) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== PROFIL UTILISATEUR ===\n");
        sb.append("Rôle: ").append(dto.getUserType().name()).append("\n");
        if (dto.getRegionId() != null)
            sb.append("Région ID: ").append(dto.getRegionId()).append("\n");
        if (dto.getBudget() != null)
            sb.append("Budget max: ").append(dto.getBudget()).append(" DT\n");
        if (dto.getActivityDomain() != null) {
            if (isSimilarSearch) {
                sb.append("Domaine demandé (AUCUN service disponible): ").append(dto.getActivityDomain().name()).append("\n");
                sb.append("→ Cherche des domaines PROCHES et SIMILAIRES\n");
            } else {
                sb.append("Domaine d'activité: ").append(dto.getActivityDomain().name()).append("\n");
            }
        }
        if (dto.getPreferredSector() != null && !dto.getPreferredSector().isBlank())
            sb.append("Secteur préféré: ").append(dto.getPreferredSector()).append("\n");
        if (dto.getAvailability() != null && !dto.getAvailability().isBlank())
            sb.append("Disponibilité: ").append(dto.getAvailability()).append("\n");
        if (dto.getCollaborationType() != null && !dto.getCollaborationType().isBlank())
            sb.append("Type collaboration: ").append(dto.getCollaborationType()).append("\n");
        if (dto.getInvestmentHorizon() != null && !dto.getInvestmentHorizon().isBlank())
            sb.append("Horizon: ").append(dto.getInvestmentHorizon()).append("\n");
        if (dto.getOfferedSkills() != null && !dto.getOfferedSkills().isEmpty())
            sb.append("Compétences: ").append(String.join(", ", dto.getOfferedSkills())).append("\n");
        if (dto.getServiceTypeFilter() != null && !dto.getServiceTypeFilter().isBlank())
            sb.append("Filtre type: ").append(dto.getServiceTypeFilter()).append("\n");
        if (dto.getOriginCountry() != null && !dto.getOriginCountry().isBlank())
            sb.append("Pays: ").append(dto.getOriginCountry()).append("\n");
        if (dto.getGroupSize() != null)
            sb.append("Groupe: ").append(dto.getGroupSize()).append("\n");
        if (dto.getTargetAudience() != null)
            sb.append("Audience: ").append(dto.getTargetAudience().name()).append("\n");
        if (dto.getProjectDescription() != null && !dto.getProjectDescription().isBlank())
            sb.append("Projet: ").append(truncate(dto.getProjectDescription(), 150)).append("\n");

        sb.append("\n=== SERVICES DISPONIBLES ===\n");
        for (Object svc : candidates) {
            sb.append(buildServiceLine(svc)).append("\n");
        }

        sb.append("\nRéponds UNIQUEMENT avec: {\"selectedIds\": [...], \"reasoning\": \"...\"}\n");
        return sb.toString();
    }

    // ================================================================
    //  CONSTRUIT LA NOTE DE SUGGESTION pour le frontend
    // ================================================================
    private String buildSuggestionNote(String requestedDomain, String groqReasoning) {
        String domainLabel = formatDomainLabel(requestedDomain);

        // Essayer d'extraire les domaines suggérés depuis le reasoning de Groq
        String note = "Aucun service disponible pour \"" + domainLabel + "\". ";
        note += "Voici des services de domaines similaires qui pourraient vous intéresser.";

        return note;
    }

    private String formatDomainLabel(String domain) {
        return switch (domain) {
            case "TECHNOLOGY"       -> "Technologie";
            case "IT"               -> "Informatique";
            case "AGRICULTURE"      -> "Agriculture";
            case "AGRI_FOOD"        -> "Agroalimentaire";
            case "TOURISM"          -> "Tourisme";
            case "HOTEL"            -> "Hôtellerie";
            case "MANUFACTURING"    -> "Fabrication";
            case "INDUSTRY"         -> "Industrie";
            case "ENERGY"           -> "Énergie";
            case "RENEWABLE_ENERGY" -> "Énergie renouvelable";
            case "FINANCE"          -> "Finance";
            case "HEALTH"           -> "Santé";
            case "EDUCATION"        -> "Éducation";
            case "CONSTRUCTION"     -> "Construction";
            case "REAL_ESTATE"      -> "Immobilier";
            case "TEXTILE"          -> "Textile";
            case "TRADE"            -> "Commerce";
            case "SERVICES"         -> "Services";
            case "TRANSPORT"        -> "Transport";
            case "RESTAURANT"       -> "Restauration";
            default -> domain;
        };
    }

    // ================================================================
    //  PARSING RÉPONSE GROQ
    // ================================================================
    private static class GroqFilterResponse {
        final List<Long> selectedIds;
        final String     reasoning;

        GroqFilterResponse(List<Long> selectedIds, String reasoning) {
            this.selectedIds = selectedIds;
            this.reasoning   = reasoning;
        }
    }

    private GroqFilterResponse parseGroqFilterResponse(String rawResponse, List<Object> pool) {
        if (rawResponse == null || rawResponse.isBlank())
            return new GroqFilterResponse(List.of(), "Réponse vide");

        try {
            String cleaned = rawResponse
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            int start = cleaned.indexOf('{');
            int end   = cleaned.lastIndexOf('}');
            if (start < 0 || end <= start)
                return new GroqFilterResponse(List.of(), "JSON introuvable");

            JsonNode root    = mapper.readTree(cleaned.substring(start, end + 1));
            String reasoning = root.path("reasoning").asText("");
            JsonNode idsNode = root.path("selectedIds");

            log.info("Groq reasoning: {}", reasoning);

            if (!idsNode.isArray())
                return new GroqFilterResponse(List.of(), reasoning);

            List<Long> ids = new ArrayList<>();
            for (JsonNode n : idsNode) {
                if (n.isNumber()) ids.add(n.asLong());
            }

            log.info("Groq a sélectionné {} IDs: {}", ids.size(), ids);
            return new GroqFilterResponse(ids, reasoning);

        } catch (Exception e) {
            log.error("Erreur parsing Groq: {}", e.getMessage());
            return new GroqFilterResponse(List.of(), "Erreur parsing");
        }
    }

    private List<Object> mapIdsToServices(List<Long> ids, List<Object> pool) {
        return pool.stream()
                .filter(svc -> ids.contains(getServiceId(svc)))
                .collect(Collectors.toList());
    }

    // ================================================================
    //  LIGNE COMPACTE PAR SERVICE
    // ================================================================
    private String buildServiceLine(Object svc) {
        if (svc instanceof TouristService ts) {
            return String.format(
                    "ID:%d|TOURIST|\"%s\"|cat:%s|regionId:%s|prix:%sDT|dispo:%s|cap:%s|audience:%s",
                    ts.getId(), safe(ts.getName()),
                    ts.getCategory() != null ? ts.getCategory().name() : "?",
                    ts.getRegion() != null ? ts.getRegion().getId() : "?",
                    ts.getPrice() != null ? ts.getPrice().longValue() : "?",
                    ts.getAvailability() != null ? ts.getAvailability().name() : "?",
                    ts.getMaxCapacity() != null ? ts.getMaxCapacity() : "?",
                    ts.getTargetAudience() != null ? ts.getTargetAudience().name() : "?"
            );
        }
        if (svc instanceof InvestmentService is) {
            return String.format(
                    "ID:%d|INVESTMENT|\"%s\"|secteur:%s|regionId:%s|minMt:%sDT|dispo:%s|duree:%s",
                    is.getId(), safe(is.getTitle() != null ? is.getTitle() : is.getName()),
                    is.getEconomicSector() != null ? is.getEconomicSector().getName() : "?",
                    is.getRegion() != null ? is.getRegion().getId() : "?",
                    is.getMinimumAmount() != null ? is.getMinimumAmount().longValue() : "?",
                    is.getAvailability() != null ? is.getAvailability().name() : "?",
                    safe(is.getProjectDuration())
            );
        }
        if (svc instanceof CollaborationService cs) {
            String skills = cs.getRequiredSkills() != null
                    ? cs.getRequiredSkills().stream().limit(3).collect(Collectors.joining(","))
                    : "?";
            return String.format(
                    "ID:%d|COLLABORATION|\"%s\"|domaine:%s|type:%s|regionId:%s|budget:%sDT|dispo:%s|skills:[%s]",
                    cs.getId(), safe(cs.getName()),
                    cs.getActivityDomain() != null ? cs.getActivityDomain().name() : "?",
                    cs.getCollaborationType() != null ? cs.getCollaborationType().name() : "?",
                    cs.getRegion() != null ? cs.getRegion().getId() : "?",
                    cs.getRequestedBudget() != null ? cs.getRequestedBudget().longValue() : "?",
                    cs.getAvailability() != null ? cs.getAvailability().name() : "?",
                    skills
            );
        }
        return "ID:?|UNKNOWN";
    }

    // ================================================================
    //  FALLBACK CASCADE MANUEL
    // ================================================================
    private List<Object> fallbackCascadeFilter(List<Object> all, RecommendationRequestDTO dto) {
        log.info("Fallback filtre cascade manuel activé");

        List<Object> byDomain;
        if (dto.getActivityDomain() != null) {
            String domain = dto.getActivityDomain().name().toUpperCase();
            byDomain = all.stream()
                    .filter(svc -> matchesDomainFallback(svc, domain))
                    .collect(Collectors.toList());
            if (byDomain.isEmpty()) {
                // Fallback cascade : chercher domaines proches manuellement
                byDomain = all.stream()
                        .filter(svc -> matchesSimilarDomain(svc, domain))
                        .collect(Collectors.toList());
                if (byDomain.isEmpty()) return List.of();
            }
        } else {
            byDomain = new ArrayList<>(all);
        }

        List<Object> byRegion = byDomain;
        if (dto.getRegionId() != null) {
            List<Object> filtered = byDomain.stream()
                    .filter(svc -> matchesRegion(svc, dto.getRegionId()))
                    .collect(Collectors.toList());
            if (!filtered.isEmpty()) byRegion = filtered;
        }

        List<Object> result = byRegion;
        if (dto.getBudget() != null) {
            List<Object> filtered = byRegion.stream()
                    .filter(svc -> matchesBudget(svc, dto.getBudget().doubleValue()))
                    .collect(Collectors.toList());
            if (!filtered.isEmpty()) result = filtered;
        }

        return result;
    }

    // ── Vérifie si un service correspond exactement au domaine ───────
    private boolean matchesDomainFallback(Object svc, String domain) {
        if (svc instanceof TouristService ts)
            return (ts.getCategory() != null && ts.getCategory().name().equalsIgnoreCase(domain))
                    || isTouristDomain(domain);
        if (svc instanceof InvestmentService is) {
            if (is.getEconomicSector() == null) return false;
            String s = is.getEconomicSector().getName().toUpperCase();
            return s.contains(domain) || domain.contains(s) || aiEngine.sectorDomainMatchPublic(s, domain);
        }
        if (svc instanceof CollaborationService cs)
            return cs.getActivityDomain() != null && cs.getActivityDomain().name().equalsIgnoreCase(domain);
        return false;
    }

    // ── Vérifie si un service correspond à un domaine proche ────────
    private boolean matchesSimilarDomain(Object svc, String requestedDomain) {
        List<String> similarDomains = getSimilarDomains(requestedDomain);
        for (String similar : similarDomains) {
            if (matchesDomainFallback(svc, similar)) return true;
        }
        return false;
    }

    // ── Retourne les domaines proches d'un domaine donné ────────────
    private List<String> getSimilarDomains(String domain) {
        return switch (domain) {
            case "MANUFACTURING"    -> List.of("INDUSTRY", "TEXTILE", "CONSTRUCTION", "ENERGY");
            case "INDUSTRY"         -> List.of("MANUFACTURING", "ENERGY", "CONSTRUCTION", "TRADE");
            case "IT"               -> List.of("TECHNOLOGY", "FINANCE", "EDUCATION", "SERVICES");
            case "TECHNOLOGY"       -> List.of("IT", "EDUCATION", "FINANCE", "SERVICES");
            case "AGRICULTURE"      -> List.of("AGRI_FOOD", "ENERGY", "CONSTRUCTION");
            case "AGRI_FOOD"        -> List.of("AGRICULTURE", "TRADE", "HEALTH");
            case "TOURISM"          -> List.of("HOTEL", "TRANSPORT", "RESTAURANT", "CRAFTS", "TRAVEL_AGENCY");
            case "HOTEL"            -> List.of("TOURISM", "RESTAURANT", "TRANSPORT");
            case "ENERGY"           -> List.of("RENEWABLE_ENERGY", "INDUSTRY", "CONSTRUCTION");
            case "RENEWABLE_ENERGY" -> List.of("ENERGY", "AGRICULTURE", "CONSTRUCTION");
            case "FINANCE"          -> List.of("TRADE", "SERVICES", "TECHNOLOGY", "IT");
            case "HEALTH"           -> List.of("EDUCATION", "TECHNOLOGY", "SERVICES");
            case "EDUCATION"        -> List.of("HEALTH", "SERVICES", "TECHNOLOGY");
            case "CONSTRUCTION"     -> List.of("REAL_ESTATE", "INDUSTRY", "ENERGY");
            case "REAL_ESTATE"      -> List.of("CONSTRUCTION", "TRADE", "SERVICES");
            case "TEXTILE"          -> List.of("MANUFACTURING", "TRADE", "INDUSTRY");
            case "TRADE"            -> List.of("SERVICES", "FINANCE", "TRANSPORT");
            case "SERVICES"         -> List.of("TRADE", "FINANCE", "TECHNOLOGY");
            case "TRANSPORT"        -> List.of("TRADE", "TOURISM", "CONSTRUCTION");
            default                 -> List.of("TRADE", "SERVICES", "INDUSTRY");
        };
    }

    private boolean isTouristDomain(String domain) {
        return switch (domain) {
            case "TOURISM", "HOTEL", "GUEST_HOUSE", "TOUR_GUIDE",
                 "TRANSPORT", "RESTAURANT", "CRAFTS", "TRAVEL_AGENCY" -> true;
            default -> false;
        };
    }

    private boolean matchesRegion(Object svc, Long regionId) {
        if (svc instanceof TouristService ts)
            return ts.getRegion() != null && ts.getRegion().getId().equals(regionId);
        if (svc instanceof InvestmentService is)
            return is.getRegion() != null && is.getRegion().getId().equals(regionId);
        if (svc instanceof CollaborationService cs)
            return cs.getRegion() != null && cs.getRegion().getId().equals(regionId);
        return false;
    }

    private boolean matchesBudget(Object svc, double budget) {
        if (svc instanceof TouristService ts) {
            if (ts.getPrice() == null) return true;
            return ts.getPrice().doubleValue() <= budget * 1.30;
        }
        if (svc instanceof InvestmentService is) {
            java.math.BigDecimal minAmt = is.getMinimumAmount() != null ? is.getMinimumAmount() : is.getTotalAmount();
            if (minAmt == null) return true;
            return minAmt.doubleValue() <= budget * 1.50;
        }
        if (svc instanceof CollaborationService cs) {
            if (cs.getRequestedBudget() == null) return true;
            return cs.getRequestedBudget().doubleValue() <= budget * 1.30;
        }
        return true;
    }

    // ================================================================
    //  CHARGEMENT DES CANDIDATS
    // ================================================================
    private List<Object> fetchCandidates(RecommendationRequestDTO dto) {
        LocalDate today = LocalDate.now();

        return switch (dto.getUserType()) {

            case TOURIST -> {
                List<Object> list = touristRepo.findAll().stream()
                        .filter(s -> s.getStatus() == ServiceStatus.APPROVED)
                        .filter(s -> dto.getGroupSize() == null
                                || s.getMaxCapacity() == null
                                || s.getMaxCapacity() >= dto.getGroupSize())
                        .collect(Collectors.toCollection(ArrayList::new));
                yield list;
            }

            case INVESTOR -> investmentRepo.findAll().stream()
                    .filter(s -> s.getStatus() == ServiceStatus.APPROVED)
                    .filter(s -> s.getDeadlineDate() == null || !s.getDeadlineDate().isBefore(today))
                    .collect(Collectors.toCollection(ArrayList::new));

            case PARTNER -> collaborationRepo.findAll().stream()
                    .filter(s -> s.getStatus() == ServiceStatus.APPROVED)
                    .collect(Collectors.toCollection(ArrayList::new));

            case INTERNATIONAL_COMPANY -> {
                String filter = dto.getServiceTypeFilter();
                if ("INVESTMENT".equalsIgnoreCase(filter)) {
                    yield investmentRepo.findAll().stream()
                            .filter(s -> s.getStatus() == ServiceStatus.APPROVED)
                            .filter(s -> s.getDeadlineDate() == null || !s.getDeadlineDate().isBefore(today))
                            .collect(Collectors.toCollection(ArrayList::new));
                } else if ("COLLABORATION".equalsIgnoreCase(filter)) {
                    yield collaborationRepo.findAll().stream()
                            .filter(s -> s.getStatus() == ServiceStatus.APPROVED)
                            .collect(Collectors.toCollection(ArrayList::new));
                } else {
                    List<Object> combined = new ArrayList<>();
                    collaborationRepo.findAll().stream()
                            .filter(s -> s.getStatus() == ServiceStatus.APPROVED)
                            .forEach(combined::add);
                    investmentRepo.findAll().stream()
                            .filter(s -> s.getStatus() == ServiceStatus.APPROVED)
                            .filter(s -> s.getDeadlineDate() == null || !s.getDeadlineDate().isBefore(today))
                            .forEach(combined::add);
                    yield combined;
                }
            }

            default -> {
                log.warn("UserType non géré : {}", dto.getUserType());
                yield List.of();
            }
        };
    }

    // ================================================================
    //  UTILITAIRES
    // ================================================================
    private Long getServiceId(Object svc) {
        if (svc instanceof TouristService ts)       return ts.getId();
        if (svc instanceof InvestmentService is)    return is.getId();
        if (svc instanceof CollaborationService cs) return cs.getId();
        return -1L;
    }

    private String safe(String s) {
        return s != null ? s.replaceAll("\"", "'") : "?";
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}