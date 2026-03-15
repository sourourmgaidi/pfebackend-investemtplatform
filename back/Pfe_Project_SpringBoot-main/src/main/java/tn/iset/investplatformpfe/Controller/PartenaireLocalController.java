package tn.iset.investplatformpfe.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/partenaires-locaux")
public class PartenaireLocalController {

    @Autowired
    private PartenaireLocalService service;

    // CREATE
    @PostMapping
    public ResponseEntity<?> createPartenaire(@RequestBody PartenaireLocal partenaire) {
        String result = service.validateAndSave(partenaire);
        if (!result.equals("OK")) return ResponseEntity.badRequest().body(result);
        return ResponseEntity.ok(partenaire);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePartenaire(@PathVariable Long id, @RequestBody PartenaireLocal partenaire) {
        partenaire.setId(id);
        String result = service.validateAndSave(partenaire);
        if (!result.equals("OK")) return ResponseEntity.badRequest().body(result);
        return ResponseEntity.ok(partenaire);
    }

    // READ ALL
    @GetMapping
    public List<PartenaireLocal> getAllPartenaires() {
        return service.getAllPartenaires();
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getPartenaireById(@PathVariable Long id) {
        Optional<PartenaireLocal> partenaire = service.getPartenaireById(id);
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

