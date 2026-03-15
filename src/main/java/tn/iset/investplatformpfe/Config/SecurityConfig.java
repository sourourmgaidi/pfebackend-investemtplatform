package tn.iset.investplatformpfe.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    // ========================================
    // 1. FILTRE POUR LES RESSOURCES STATIQUES (UPLOADS) - AVEC CORS
    // ========================================
    @Bean
    @Order(1)
    public SecurityFilterChain staticResourcesFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/uploads/**")
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // ✅ CORS AJOUTÉ
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    // ========================================
    // 2. FILTRE POUR LES ENDPOINTS PUBLICS D'AUTH
    // ========================================
    @Bean
    @Order(2)
    public SecurityFilterChain publicAuthFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/api/auth/refresh",
                        "/api/auth/logout",
                        "/api/auth/forgot-password",
                        "/api/auth/reset-password",
                        "/api/public/**",
                        "/api/partenaires-locaux/register",
                        "/api/partenaires-locaux/login",
                        "/api/partenaires-locaux/refresh",
                        "/api/partenaires-locaux/logout",
                        "/api/partenaires-locaux/forgot-password",
                        "/api/partenaires-locaux/reset-password",
                        "/api/touristes/register",
                        "/api/touristes/login",
                        "/api/touristes/refresh",
                        "/api/touristes/logout",
                        "/api/touristes/forgot-password",
                        "/api/touristes/reset-password",
                        "/api/partenaires-economiques/register",
                        "/api/partenaires-economiques/login",
                        "/api/partenaires-economiques/refresh",
                        "/api/partenaires-economiques/logout",
                        "/api/partenaires-economiques/forgot-password",
                        "/api/partenaires-economiques/reset-password",
                        "/api/admin/register",
                        "/api/admin/login",
                        "/api/admin/refresh",
                        "/api/admin/logout",
                        "/api/admin/forgot-password",
                        "/api/admin/reset-password",
                        "/api/international-companies/register",
                        "/api/international-companies/login",
                        "/api/international-companies/refresh",
                        "/api/international-companies/logout",
                        "/api/international-companies/forgot-password",
                        "/api/international-companies/reset-password"
                )
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    // ========================================
    // 3. FILTRE POUR LES ENDPOINTS PROTÉGÉS (AVEC JWT)
    // ========================================
    @Bean
    @Order(3)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authz -> authz
                        // L'upload nécessite une authentification
                        .requestMatchers("/api/upload/**").authenticated()

                        // Les profils nécessitent une authentification
                        .requestMatchers("/api/*/profile").authenticated()
                        .requestMatchers("/api/*/update").authenticated()

                        // ✅ SUPPRESSION DE COMPTE - UTILISATEUR (son propre compte)
                        .requestMatchers("/api/auth/delete-account").authenticated()
                        .requestMatchers("/api/partenaires-economiques/delete-account").authenticated()
                        .requestMatchers("/api/partenaires-locaux/delete-account").authenticated()
                        .requestMatchers("/api/touristes/delete-account").authenticated()
                        .requestMatchers("/api/international-companies/delete-account").authenticated()
                        .requestMatchers("/api/admin/delete-account").authenticated()

                        // ✅ SUPPRESSION DE COMPTE - ADMIN (supprime les autres utilisateurs)
                        .requestMatchers("/api/auth/admin/delete-account/**").hasRole("ADMIN")
                        .requestMatchers("/api/partenaires-economiques/admin/delete-account/**").hasRole("ADMIN")
                        .requestMatchers("/api/partenaires-locaux/admin/delete-account/**").hasRole("ADMIN")
                        .requestMatchers("/api/touristes/admin/delete-account/**").hasRole("ADMIN")
                        .requestMatchers("/api/international-companies/admin/delete-account/**").hasRole("ADMIN")

                        // ✅ NOUVEAUX ENDPOINTS POUR L'ADMIN DANS /api/admin
                        .requestMatchers("/api/admin/delete-user/**").hasRole("ADMIN")

                        // Tout le reste dans /api nécessite une authentification
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );
        return http.build();
    }

    // ========================================
    // 4. FILTRE PAR DÉFAUT (POUR TOUT LE RESTE)
    // ========================================
    @Bean
    @Order(4)
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().permitAll()
                );
        return http.build();
    }

    // ✅ CONFIGURATION CORS COMPLÈTE
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Origines autorisées (votre frontend Angular)
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:4200",
                "http://127.0.0.1:4200",
                "http://localhost:4201"
        ));

        // Méthodes HTTP autorisées
        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));

        // Headers autorisés
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers",
                "Cache-Control"
        ));

        // Headers exposés au frontend
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Disposition"
        ));

        // Autoriser les cookies/credentials
        configuration.setAllowCredentials(true);

        // Durée de vie du cache CORS (1 heure)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        converter.setPrincipalClaimName("preferred_username");
        return converter;
    }
}

class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

        if (realmAccess == null || realmAccess.isEmpty()) {
            return List.of();
        }

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.get("roles");

        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());
    }
}