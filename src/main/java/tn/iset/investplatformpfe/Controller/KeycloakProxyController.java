package tn.iset.investplatformpfe.Controller;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;

@RestController
public class KeycloakProxyController {

    private final RestTemplate restTemplate = new RestTemplate();

    // ✅ URL locale de Keycloak
    private static final String KEYCLOAK_BASE_URL = "http://localhost:8080";

    // ✅ Proxy GET — pour le discovery document et les redirections
    @RequestMapping(
            value = "/realms/**",
            method = {RequestMethod.GET, RequestMethod.POST}
    )
    public ResponseEntity<String> proxyKeycloak(
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {

        String path = request.getRequestURI();
        String queryString = request.getQueryString();
        String targetUrl = KEYCLOAK_BASE_URL + path +
                (queryString != null ? "?" + queryString : "");

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!headerName.equalsIgnoreCase("host")) {
                headers.set(headerName, request.getHeader(headerName));
            }
        }

        HttpMethod method = HttpMethod.valueOf(request.getMethod());
        HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    URI.create(targetUrl),
                    method,
                    entity,
                    String.class
            );

            // ✅ Remplacer localhost:8080 par l'URL ngrok dans la réponse
            String responseBody = response.getBody();
            if (responseBody != null) {
                responseBody = responseBody.replace(
                        "http://localhost:8080",
                        "https://rising-docile-unshackle.ngrok-free.dev"
                );
            }

            return ResponseEntity
                    .status(response.getStatusCode())
                    .headers(response.getHeaders())
                    .body(responseBody);

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body("Proxy error: " + e.getMessage());
        }
    }
}
