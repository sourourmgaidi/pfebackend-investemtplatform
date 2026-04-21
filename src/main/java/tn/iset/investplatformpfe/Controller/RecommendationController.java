package tn.iset.investplatformpfe.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Dto.RecommendationRequestDTO;
import tn.iset.investplatformpfe.Service.RecommendationService;

@RestController
@RequestMapping("/api/recommendations")
@CrossOrigin(origins = "*")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @PostMapping
    public ResponseEntity<?> getRecommendations(@RequestBody RecommendationRequestDTO dto) {

        return ResponseEntity.ok(recommendationService.recommend(dto));
    }
}
