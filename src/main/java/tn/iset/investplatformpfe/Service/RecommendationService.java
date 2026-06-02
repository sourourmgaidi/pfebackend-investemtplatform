package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Dto.RecommendationRequestDTO;
import tn.iset.investplatformpfe.Dto.RecommendationResponseDTO;
import tn.iset.investplatformpfe.Repository.CollaborationServiceRepository;
import tn.iset.investplatformpfe.Repository.InvestmentServiceRepository;
import tn.iset.investplatformpfe.Repository.TouristServiceRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.iset.investplatformpfe.Entity.ServiceStatus;
import tn.iset.investplatformpfe.Entity.TouristService;
import tn.iset.investplatformpfe.Entity.InvestmentService;
import tn.iset.investplatformpfe.Entity.CollaborationService;

@Service
public class RecommendationService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationService.class);

    @Autowired private TouristServiceRepository       touristRepo;
    @Autowired private InvestmentServiceRepository    investmentRepo;
    @Autowired private CollaborationServiceRepository collaborationRepo;
    @Autowired private AIEngine                       aiEngine;

    // ================================================================
    //  POINT D'ENTRÉE PRINCIPAL
    //  Priorité stricte :
    //   1. Domaine     → obligatoire si fourni, sinon retourne vide
    //   2. Région      → affine le niveau 1
    //   3. Budget      → affine le niveau 2
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
        log.info("{} candidats chargés", allCandidates.size());

        if (allCandidates.isEmpty()) return List.of();

        // 2. Filtrage en cascade strict
        List<Object> candidates = applyCascadeFilter(allCandidates, dto);

        if (candidates.isEmpty()) {
            log.warn("Aucun service ne correspond aux critères — retour liste vide");
            return List.of(); // Frontend affichera "Aucun service disponible"
        }

        log.info("{} candidats retenus après filtrage", candidates.size());

        // 3. Scorer et trier
        List<RecommendationResponseDTO> scored = candidates.stream()
                .map(service -> {
                    try {
                        AIEngine.AIScoreResult result = aiEngine.computeScoreWithAI(service, dto);
                        int score = Math.max(0, Math.min(10, result.totalScore));
                        return new RecommendationResponseDTO(service, score, result.explanation, result.isAIScored);
                    } catch (Exception e) {
                        log.warn("Erreur scoring : {}", e.getMessage());
                        int ruleScore = aiEngine.computeRuleScore(service, dto);
                        return new RecommendationResponseDTO(service, ruleScore, "Score par règles métier.", false);
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
            log.info("  Score {} | {}", r.getScore(), name);
        });

        return scored;
    }

    // ================================================================
    //  FILTRAGE EN CASCADE STRICT
    //
    //  Étape 1 — Domaine
    //    - Si domaine fourni et aucun service ne correspond → retour VIDE
    //    - Si domaine non fourni → on prend tout
    //
    //  Étape 2 — Région (sur le résultat de l'étape 1)
    //    - Si région fournie et des services correspondent → on affine
    //    - Si région fournie mais aucun service → on garde l'étape 1
    //
    //  Étape 3 — Budget (sur le résultat de l'étape 2)
    //    - Si budget fourni et des services correspondent → on affine
    //    - Si budget fourni mais aucun service → on garde l'étape 2
    // ================================================================
    private List<Object> applyCascadeFilter(List<Object> all, RecommendationRequestDTO dto) {

        // ── ÉTAPE 1 : DOMAINE (priorité maximale) ────────────────────
        List<Object> byDomain;
        if (dto.getActivityDomain() != null) {
            byDomain = filterByDomain(all, dto);
            if (byDomain.isEmpty()) {
                log.warn("Aucun service pour domaine={} → retour vide", dto.getActivityDomain());
                return List.of(); // Domaine fourni, aucun match → on ne recommande rien
            }
        } else {
            byDomain = new ArrayList<>(all); // Pas de domaine → pas de filtre domaine
        }
        log.info("Étape 1 (domaine) : {} services", byDomain.size());

        // ── ÉTAPE 2 : RÉGION ─────────────────────────────────────────
        List<Object> byRegion = byDomain;
        if (dto.getRegionId() != null) {
            List<Object> filtered = filterByRegion(byDomain, dto);
            if (!filtered.isEmpty()) {
                byRegion = filtered;
                log.info("Étape 2 (domaine+région) : {} services", byRegion.size());
            } else {
                log.info("Étape 2 (région) vide → on garde les {} services du domaine", byDomain.size());
                // On garde byDomain — le scoring pénalisera les hors-région
            }
        }

        // ── ÉTAPE 3 : BUDGET ─────────────────────────────────────────
        List<Object> byBudget = byRegion;
        if (dto.getBudget() != null) {
            List<Object> filtered = filterByBudget(byRegion, dto);
            if (!filtered.isEmpty()) {
                byBudget = filtered;
                log.info("Étape 3 (domaine+région+budget) : {} services", byBudget.size());
            } else {
                log.info("Étape 3 (budget) vide → on garde les {} services domaine+région", byRegion.size());
                // Budget trop restrictif → on l'ignore, le scoring dégradéra les hors-budget
            }
        }

        return byBudget;
    }

    // ──────────────────────────────────────────────────────────────────
    //  FILTRE PAR DOMAINE D'ACTIVITÉ
    // ──────────────────────────────────────────────────────────────────
    private List<Object> filterByDomain(List<Object> candidates, RecommendationRequestDTO dto) {
        if (dto.getActivityDomain() == null) return new ArrayList<>(candidates);
        String domain = dto.getActivityDomain().name().toUpperCase();

        return candidates.stream()
                .filter(svc -> matchesDomain(svc, domain))
                .collect(Collectors.toList());
    }

    private boolean matchesDomain(Object svc, String domain) {

        if (svc instanceof TouristService ts) {
            // Catégorie directe
            if (ts.getCategory() != null &&
                    ts.getCategory().name().equalsIgnoreCase(domain)) return true;
            // Si le domaine est un domaine touristique général → tous les services touristiques passent
            return isTouristDomain(domain);
        }

        if (svc instanceof InvestmentService is) {
            if (is.getEconomicSector() == null) return false;
            String sectorName = is.getEconomicSector().getName().toUpperCase();
            return sectorName.contains(domain) || domain.contains(sectorName)
                    || aiEngine.sectorDomainMatchPublic(sectorName, domain);
        }

        if (svc instanceof CollaborationService cs) {
            if (cs.getActivityDomain() == null) return false;
            return cs.getActivityDomain().name().equalsIgnoreCase(domain);
        }

        return false;
    }

    private boolean isTouristDomain(String domain) {
        return switch (domain) {
            case "TOURISM", "HOTEL", "GUEST_HOUSE", "TOUR_GUIDE",
                 "TRANSPORT", "RESTAURANT", "CRAFTS", "TRAVEL_AGENCY" -> true;
            default -> false;
        };
    }

    // ──────────────────────────────────────────────────────────────────
    //  FILTRE PAR RÉGION
    // ──────────────────────────────────────────────────────────────────
    private List<Object> filterByRegion(List<Object> candidates, RecommendationRequestDTO dto) {
        if (dto.getRegionId() == null) return new ArrayList<>(candidates);

        return candidates.stream()
                .filter(svc -> {
                    if (svc instanceof TouristService ts)
                        return ts.getRegion() != null && ts.getRegion().getId().equals(dto.getRegionId());
                    if (svc instanceof InvestmentService is)
                        return is.getRegion() != null && is.getRegion().getId().equals(dto.getRegionId());
                    if (svc instanceof CollaborationService cs)
                        return cs.getRegion() != null && cs.getRegion().getId().equals(dto.getRegionId());
                    return false;
                })
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────────
    //  FILTRE PAR BUDGET
    // ──────────────────────────────────────────────────────────────────
    private List<Object> filterByBudget(List<Object> candidates, RecommendationRequestDTO dto) {
        if (dto.getBudget() == null) return new ArrayList<>(candidates);
        double budget = dto.getBudget().doubleValue();

        return candidates.stream()
                .filter(svc -> {
                    if (svc instanceof TouristService ts) {
                        if (ts.getPrice() == null) return true;
                        return ts.getPrice().doubleValue() <= budget * 1.10; // tolérance 10%
                    }
                    if (svc instanceof InvestmentService is) {
                        java.math.BigDecimal minAmt = is.getMinimumAmount() != null
                                ? is.getMinimumAmount() : is.getTotalAmount();
                        if (minAmt == null) return true;
                        return minAmt.doubleValue() <= budget * 1.50; // tolérance 50%
                    }
                    if (svc instanceof CollaborationService cs) {
                        if (cs.getRequestedBudget() == null) return true;
                        return cs.getRequestedBudget().doubleValue() <= budget * 1.30; // tolérance 30%
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────────
    //  FILTRE CAPACITÉ GROUPE
    // ──────────────────────────────────────────────────────────────────
    private List<Object> filterByGroupSize(List<Object> candidates, RecommendationRequestDTO dto) {
        if (dto.getGroupSize() == null) return new ArrayList<>(candidates);
        return candidates.stream()
                .filter(svc -> {
                    if (svc instanceof TouristService ts) {
                        if (ts.getMaxCapacity() == null) return true;
                        return ts.getMaxCapacity() >= dto.getGroupSize();
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    // ================================================================
    //  CHARGEMENT DES CANDIDATS (APPROVED + non expirés)
    // ================================================================
    private List<Object> fetchCandidates(RecommendationRequestDTO dto) {
        return switch (dto.getUserType()) {

            case TOURIST -> {
                List<Object> list = touristRepo.findAll().stream()
                        .filter(s -> s.getStatus() == ServiceStatus.APPROVED)
                        .collect(Collectors.toCollection(ArrayList::new));
                yield filterByGroupSize(list, dto);
            }

            case INVESTOR -> investmentRepo.findAll().stream()
                    .filter(s -> s.getStatus() == ServiceStatus.APPROVED)
                    .filter(s -> s.getDeadlineDate() == null ||
                            !s.getDeadlineDate().isBefore(java.time.LocalDate.now()))
                    .collect(Collectors.toCollection(ArrayList::new));

            case PARTNER -> collaborationRepo.findAll().stream()
                    .filter(s -> s.getStatus() == ServiceStatus.APPROVED)
                    .collect(Collectors.toCollection(ArrayList::new));

            case INTERNATIONAL_COMPANY -> {
                String filter = dto.getServiceTypeFilter();
                if ("INVESTMENT".equalsIgnoreCase(filter)) {
                    yield investmentRepo.findAll().stream()
                            .filter(s -> s.getStatus() == ServiceStatus.APPROVED)
                            .filter(s -> s.getDeadlineDate() == null ||
                                    !s.getDeadlineDate().isBefore(java.time.LocalDate.now()))
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
                            .filter(s -> s.getDeadlineDate() == null ||
                                    !s.getDeadlineDate().isBefore(java.time.LocalDate.now()))
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
}