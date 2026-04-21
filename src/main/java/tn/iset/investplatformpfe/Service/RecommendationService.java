package tn.iset.investplatformpfe.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Dto.RecommendationRequestDTO;
import tn.iset.investplatformpfe.Dto.RecommendationResponseDTO;
import tn.iset.investplatformpfe.Repository.CollaborationServiceRepository;
import tn.iset.investplatformpfe.Repository.InvestmentServiceRepository;
import tn.iset.investplatformpfe.Repository.TouristServiceRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tn.iset.investplatformpfe.Entity.ServiceStatus;
import java.util.Comparator;
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

    // ═════════════════════════════════════════════════════════════════
    //  POINT D'ENTRÉE PRINCIPAL
    // ═════════════════════════════════════════════════════════════════
    public List<RecommendationResponseDTO> recommend(RecommendationRequestDTO dto) {

        if (dto.getUserType() == null) {
            log.warn("⚠️ userType null dans la requête de recommandation");
            return List.of();
        }

        log.info("🔍 Recommandation — userType={}, region={}, budget={}, domain={}, availability={}",
                dto.getUserType(), dto.getRegionId(), dto.getBudget(),
                dto.getActivityDomain(), dto.getAvailability());

        // ── 1. Charger tous les candidats ───────────────────────────
        List<Object> allCandidates = fetchCandidates(dto);
        log.info("📦 {} candidats chargés", allCandidates.size());

        if (allCandidates.isEmpty()) {
            log.warn("⚠️ Aucun candidat disponible pour userType={}", dto.getUserType());
            return List.of();
        }

        // ── 2. Pré-filtrer pour éliminer les services totalement hors
        //        critères obligatoires (si budget fourni, région fournie)
        List<Object> filtered = preFilter(allCandidates, dto);
        log.info("🔎 {} candidats après pré-filtrage", filtered.size());

        // ── 3. Si le pré-filtre est trop restrictif, revenir à tous
        //        les candidats (on préfère scorer plutôt que retourner vide)
        List<Object> candidates = filtered.isEmpty() ? allCandidates : filtered;

        // ── 4. Scorer chaque candidat ────────────────────────────────
        List<RecommendationResponseDTO> scored = candidates.stream()
                .map(service -> {
                    try {
                        AIEngine.AIScoreResult result = aiEngine.computeScoreWithAI(service, dto);
                        int score = Math.max(0, Math.min(10, result.totalScore));
                        return new RecommendationResponseDTO(service, score, result.explanation, result.isAIScored);
                    } catch (Exception e) {
                        log.warn("⚠️ Erreur scoring service : {}", e.getMessage());
                        int ruleScore = aiEngine.computeRuleScore(service, dto);
                        return new RecommendationResponseDTO(service, ruleScore, "Score par règles métier.", false);
                    }
                })
                .sorted(Comparator.comparingInt(RecommendationResponseDTO::getScore).reversed())
                .limit(10)
                .collect(Collectors.toList());

        // ── 5. Log des meilleurs résultats ───────────────────────────
        scored.stream().limit(3).forEach(r -> {
            Object svc = r.getService();
            String name = svc instanceof TouristService ts ? ts.getName()
                    : svc instanceof InvestmentService is ? (is.getTitle() != null ? is.getTitle() : is.getName())
                    : svc instanceof CollaborationService cs ? cs.getName()
                    : "?";
            log.info("  → Score {} | {}", r.getScore(), name);
        });

        return scored;
    }

    // ═════════════════════════════════════════════════════════════════
    //  PRÉ-FILTRE — élimine les services manifestement incompatibles
    //  (uniquement sur les critères durs, pour ne pas retourner vide)
    // ═════════════════════════════════════════════════════════════════
    private List<Object> preFilter(List<Object> candidates, RecommendationRequestDTO dto) {
        return candidates.stream()
                .filter(svc -> passesHardFilters(svc, dto))
                .collect(Collectors.toList());
    }

    private boolean passesHardFilters(Object svc, RecommendationRequestDTO dto) {

        // ── Touriste ─────────────────────────────────────────────────
        if (svc instanceof TouristService ts) {
            // Filtre région (si fournie)
            if (dto.getRegionId() != null && ts.getRegion() != null) {
                if (!ts.getRegion().getId().equals(dto.getRegionId())) return false;
            }
            // Filtre budget (marge de 30% tolérée)
            if (dto.getBudget() != null && ts.getPrice() != null) {
                double maxAllowed = dto.getBudget().doubleValue() * 1.30;
                if (ts.getPrice().doubleValue() > maxAllowed) return false;
            }
            // Filtre audience
            if (dto.getTargetAudience() != null && ts.getTargetAudience() != null) {
                if (!ts.getTargetAudience().name().equalsIgnoreCase(dto.getTargetAudience().name())) return false;
            }
            return true;
        }

        // ── Investissement ───────────────────────────────────────────
        if (svc instanceof InvestmentService is) {
            // Filtre région
            if (dto.getRegionId() != null && is.getRegion() != null) {
                if (!is.getRegion().getId().equals(dto.getRegionId())) return false;
            }
            // Filtre budget (marge de 50% tolérée — les montants d'investissement varient beaucoup)
            if (dto.getBudget() != null) {
                java.math.BigDecimal minAmt = is.getMinimumAmount() != null ? is.getMinimumAmount() : is.getTotalAmount();
                if (minAmt != null) {
                    double maxAllowed = dto.getBudget().doubleValue() * 1.50;
                    if (minAmt.doubleValue() > maxAllowed) return false;
                }
            }
            return true;
        }

        // ── Collaboration ────────────────────────────────────────────
        if (svc instanceof CollaborationService cs) {
            // Filtre région
            if (dto.getRegionId() != null && cs.getRegion() != null) {
                if (!cs.getRegion().getId().equals(dto.getRegionId())) return false;
            }
            // Filtre domaine d'activité (strict si fourni)
            if (dto.getActivityDomain() != null && cs.getActivityDomain() != null) {
                if (!cs.getActivityDomain().name().equalsIgnoreCase(dto.getActivityDomain().name())) return false;
            }
            return true;
        }

        return true;
    }

    // ═════════════════════════════════════════════════════════════════
    //  CHARGEMENT DES CANDIDATS (services APPROVED uniquement)
    // ═════════════════════════════════════════════════════════════════
    private List<Object> fetchCandidates(RecommendationRequestDTO dto) {
        return switch (dto.getUserType()) {

            case TOURIST -> {
                yield touristRepo.findAll().stream()          // ✅ filtrer comme les autres
                        .filter(s -> s.getStatus() == ServiceStatus.APPROVED)
                        .collect(Collectors.toCollection(ArrayList::new));
            }

            case INVESTOR -> {
                yield investmentRepo.findAll().stream()
                        .filter(s -> s.getStatus() == ServiceStatus.APPROVED)
                        .collect(Collectors.toCollection(ArrayList::new));
            }

            case PARTNER -> {
                yield collaborationRepo.findAll().stream()
                        .filter(s -> s.getStatus() == ServiceStatus.APPROVED)
                        .collect(Collectors.toCollection(ArrayList::new));
            }

            case INTERNATIONAL_COMPANY -> {
                String filter = dto.getServiceTypeFilter();

                if ("INVESTMENT".equalsIgnoreCase(filter)) {
                    yield investmentRepo.findAll().stream()
                            .filter(s -> s.getStatus() == ServiceStatus.APPROVED)
                            .collect(Collectors.toCollection(ArrayList::new));

                } else if ("COLLABORATION".equalsIgnoreCase(filter)) {
                    yield collaborationRepo.findAll().stream()
                            .filter(s -> s.getStatus() == ServiceStatus.APPROVED)
                            .collect(Collectors.toCollection(ArrayList::new));

                } else {
                    // Les deux types
                    List<Object> combined = new ArrayList<>();
                    collaborationRepo.findAll().stream()
                            .filter(s -> s.getStatus() == ServiceStatus.APPROVED)
                            .forEach(combined::add);
                    investmentRepo.findAll().stream()
                            .filter(s -> s.getStatus() == ServiceStatus.APPROVED)
                            .forEach(combined::add);
                    yield combined;
                }
            }

            default -> {
                log.warn("⚠️ UserType non géré : {}", dto.getUserType());
                yield List.of();
            }
        };
    }
}
