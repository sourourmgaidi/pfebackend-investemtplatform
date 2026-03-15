package tn.iset.investplatformpfe.Controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.LocalPartner;
import tn.iset.investplatformpfe.Service.LocalPartnerService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/partenaires-locaux")  // Keep the original mapping
public class LocalPartnerController {

    @Autowired
    private LocalPartnerService service;

    // CREATE
    @PostMapping
    public ResponseEntity<?> createPartner(@RequestBody LocalPartner partner) {
        String result = service.validateAndSave(partner);
        if (!result.equals("OK")) return ResponseEntity.badRequest().body(result);
        return ResponseEntity.ok(partner);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePartner(@PathVariable Long id, @RequestBody LocalPartner partner) {
        partner.setId(id);
        String result = service.validateAndSave(partner);
        if (!result.equals("OK")) return ResponseEntity.badRequest().body(result);
        return ResponseEntity.ok(partner);
    }

    // READ ALL
    @GetMapping
    public List<LocalPartner> getAllPartners() {
        return service.getAllPartners();
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getPartnerById(@PathVariable Long id) {
        Optional<LocalPartner> partner = service.getPartnerById(id);
        return partner.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePartner(@PathVariable Long id) {
        boolean deleted = service.deletePartner(id);
        if (deleted) return ResponseEntity.noContent().build();
        else return ResponseEntity.notFound().build();
    }
}
