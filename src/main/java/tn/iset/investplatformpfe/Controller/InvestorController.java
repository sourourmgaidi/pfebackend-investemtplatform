package tn.iset.investplatformpfe.Controller;


import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.Investor;
import tn.iset.investplatformpfe.Service.InvestorService;

import java.util.List;

@RestController
@RequestMapping("/api/investors")
@CrossOrigin(origins = "*")
public class InvestorController {
    private final InvestorService investorService;
    @Autowired
    public InvestorController(InvestorService investorService) {
        this.investorService = investorService;
    }

    //Ajouter @Valid pour activer la validation
    @PostMapping
    public ResponseEntity<Investor> createInvestor(@Valid @RequestBody Investor investor) {
        Investor createdInvestor = investorService.createInvestor(investor);
        return new ResponseEntity<>(createdInvestor, HttpStatus.CREATED);
    }

    // ========================================
// UPDATE - PUT /api/investors/{id}
// ========================================

    @PutMapping("/{id}")
    public ResponseEntity<Investor> updateInvestor(
            @PathVariable Long id,
            @Valid @RequestBody Investor investor) {

        Investor updatedInvestor = investorService.updateInvestor(id, investor);
        return ResponseEntity.ok(updatedInvestor);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvestor(@PathVariable Long id) {
        investorService.deleteInvestor(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Investor> getInvestorById(@PathVariable Long id) {
        Investor investor = investorService.getInvestorById(id);
        return ResponseEntity.ok(investor);
    }
    @GetMapping
    public ResponseEntity<List<Investor>> getAllInvestors() {
        List<Investor> investors = investorService.getAllInvestors();
        return ResponseEntity.ok(investors);
    }
}
