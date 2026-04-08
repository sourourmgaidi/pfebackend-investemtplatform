package tn.iset.investplatformpfe.Security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;
import tn.iset.investplatformpfe.Service.UserSessionService;

import java.io.IOException;
import java.util.Base64;

public class TokenExpirationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TokenExpirationFilter.class);

    private final JwtDecoder jwtDecoder;
    private final UserSessionService userSessionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public TokenExpirationFilter(JwtDecoder jwtDecoder, UserSessionService userSessionService) {
        this.jwtDecoder = jwtDecoder;
        this.userSessionService = userSessionService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                // Vérifier si le token est valide
                Jwt jwt = jwtDecoder.decode(token);

                // Token valide, continuer
                filterChain.doFilter(request, response);

            } catch (JwtException e) {
                // Token expiré ou invalide
                log.info("⚠️ Token invalide ou expiré détecté: {}", e.getMessage());

                // Extraire l'email du token expiré (si possible)
                String email = extractEmailFromExpiredToken(token);

                if (email != null) {
                    log.info("🔍 Tentative de fermeture de session pour: {}", email);

                    // Terminer la session de manière asynchrone
                    new Thread(() -> {
                        try {
                            userSessionService.endSession(email);
                            log.info("✅ Session fermée automatiquement pour token expiré: {}", email);
                        } catch (Exception ex) {
                            log.error("❌ Erreur fermeture session pour {}: {}", email, ex.getMessage());
                        }
                    }).start();
                }

                // Envoyer une réponse 401 Unauthorized
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Token expired\", \"message\": \"Votre session a expiré\"}");
                return;
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private String extractEmailFromExpiredToken(String token) {
        try {
            // Décoder le payload du token JWT (même expiré, on peut le décoder)
            String[] parts = token.split("\\.");
            if (parts.length == 3) {
                String payload = new String(Base64.getDecoder().decode(parts[1]));

                // Utiliser Jackson pour parser le JSON
                JsonNode jsonNode = objectMapper.readTree(payload);

                // Extraire l'email
                if (jsonNode.has("email")) {
                    return jsonNode.get("email").asText();
                } else if (jsonNode.has("sub")) {
                    return jsonNode.get("sub").asText();
                }
            }
        } catch (Exception e) {
            log.error("Erreur extraction email du token expiré: {}", e.getMessage());
        }
        return null;
    }
}