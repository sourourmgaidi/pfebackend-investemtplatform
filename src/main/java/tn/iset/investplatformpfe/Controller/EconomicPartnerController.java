package tn.iset.investplatformpfe.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.iset.investplatformpfe.Entity.EconomicPartner;  // Import corrigé
import tn.iset.investplatformpfe.Service.EconomicPartnerService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/economic-partners")  // Mapping en anglais
public class EconomicPartnerController {  // Renommé

    @Autowired
    private EconomicPartnerService service;

    // CREATE
    @PostMapping
    public ResponseEntity<?> createPartner(@RequestBody EconomicPartner partner) {  // Renommé
        String result = service.validateAndSave(partner);  // Appel avec partner
        if (!result.equals("OK")) return ResponseEntity.badRequest().body(result);
        return ResponseEntity.ok(partner);
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<?> updatePartner(@PathVariable Long id, @RequestBody EconomicPartner partner) {  // Renommé
        partner.setId(id); // set ID pour validation
        String result = service.validateAndSave(partner);  // Appel avec partner
        if (!result.equals("OK")) return ResponseEntity.badRequest().body(result);
        return ResponseEntity.ok(partner);
    }

    // READ ALL
    @GetMapping
    public List<EconomicPartner> getAllPartners() {  // Renommé
        return service.getAllPartners();  // Appel corrigé
    }

    // READ BY ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getPartnerById(@PathVariable Long id) {  // Renommé
        Optional<EconomicPartner> partner = service.getPartnerById(id);  // Appel corrigé
        return partner.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePartner(@PathVariable Long id) {  // Renommé
        boolean deleted = service.deletePartner(id);  // Appel corrigé
        if (deleted) return ResponseEntity.noContent().build();
        else return ResponseEntity.notFound().build();
    }
}