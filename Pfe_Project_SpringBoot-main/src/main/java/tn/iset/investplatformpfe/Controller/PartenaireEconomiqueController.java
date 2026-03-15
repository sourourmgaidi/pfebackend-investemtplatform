package tn.iset.investplatformpfe.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.PartenaireEconomique;
import tn.iset.investplatformpfe.Service.PartenaireEconomiqueService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/partenaires-economiques")
public class PartenaireEconomiqueController {

    @Autowired
    private PartenaireEconomiqueService service;

    // CREATE
    @PostMapping
    public ResponseEntity<?> createPartenaire(@RequestBody PartenaireEconomique partenaire) {
        String result = service.validateAndSave(partenaire);
        if (!result.equals("OK")) return ResponseEntity.badRequest().body(result);
        return ResponseEntity.ok(partenaire);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePartenaire(@PathVariable Long id, @RequestBody PartenaireEconomique partenaire) {
        partenaire.setId(id); // set ID pour validation
        String result = service.validateAndSave(partenaire);
        if (!result.equals("OK")) return ResponseEntity.badRequest().body(result);
        return ResponseEntity.ok(partenaire);
    }

    // READ ALL
    @GetMapping
    public List<PartenaireEconomique> getAllPartenaires() {
        return service.getAllPartenaires();
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getPartenaireById(@PathVariable Long id) {
        Optional<PartenaireEconomique> partenaire = service.getPartenaireById(id);
        return partenaire.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePartenaire(@PathVariable Long id) {
        boolean deleted = service.deletePartenaire(id);
        if (deleted) return ResponseEntity.noContent().build();
        else return ResponseEntity.notFound().build();
    }
}
