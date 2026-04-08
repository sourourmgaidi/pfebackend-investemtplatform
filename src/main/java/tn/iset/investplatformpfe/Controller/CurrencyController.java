package tn.iset.investplatformpfe.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api/currency")
@CrossOrigin(origins = "*")
public class CurrencyController {

    @GetMapping("/convert")
    public ResponseEntity<?> convert(
            @RequestParam double amount,
            @RequestParam String from,
            @RequestParam String to) {
        try {
            RestTemplate rt = new RestTemplate();

            // ✅ API gratuite sans clé — taux basés sur USD
            String url = "https://open.er-api.com/v6/latest/" + from;
            Map result = rt.getForObject(url, Map.class);

            if (result == null || !result.containsKey("rates")) {
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "Could not fetch exchange rates"));
            }

            Map<String, Object> rates = (Map<String, Object>) result.get("rates");
            Object rateObj = rates.get(to);

            if (rateObj == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Currency not supported: " + to));
            }

            double rate = ((Number) rateObj).doubleValue();
            double converted = amount * rate;
            double rounded = Math.round(converted * 100.0) / 100.0;

            return ResponseEntity.ok(Map.of(
                    "from", from,
                    "to", to,
                    "originalAmount", amount,
                    "convertedAmount", rounded,
                    "rate", Math.round(rate * 10000.0) / 10000.0
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/rates")
    public ResponseEntity<?> getRates(@RequestParam(defaultValue = "TND") String base) {
        try {
            RestTemplate rt = new RestTemplate();
            String url = "https://open.er-api.com/v6/latest/" + base;
            Map result = rt.getForObject(url, Map.class);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}