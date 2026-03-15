package tn.iset.investplatformpfe.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        // Endpoints publics
                        .requestMatchers("/api/auth/register").permitAll()
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/api/auth/refresh").permitAll()
                        .requestMatchers("/api/auth/logout").permitAll()
                        .requestMatchers("/api/auth/forgot-password").permitAll()  // AJOUTÉ
                        .requestMatchers("/api/auth/reset-password").permitAll()   //  AJOUTÉ
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers("/api/partenaires-locaux/register").permitAll()
                        .requestMatchers("/api/partenaires-locaux/login").permitAll()
                        .requestMatchers("/api/partenaires-locaux/refresh").permitAll()
                        .requestMatchers("/api/partenaires-locaux/logout").permitAll()
                        .requestMatchers("/api/partenaires-locaux/forgot-password").permitAll()
                        .requestMatchers("/api/partenaires-locaux/reset-password").permitAll()
                        .requestMatchers("/api/touristes/register").permitAll()
                        .requestMatchers("/api/touristes/login").permitAll()
                        .requestMatchers("/api/touristes/refresh").permitAll()
                        .requestMatchers("/api/touristes/logout").permitAll()
                        .requestMatchers("/api/touristes/forgot-password").permitAll()
                        .requestMatchers("/api/touristes/reset-password").permitAll()
                        .requestMatchers("/api/partenaires-economiques/register").permitAll()
                        .requestMatchers("/api/partenaires-economiques/login").permitAll()
                        .requestMatchers("/api/partenaires-economiques/refresh").permitAll()
                        .requestMatchers("/api/partenaires-economiques/logout").permitAll()
                        .requestMatchers("/api/partenaires-economiques/forgot-password").permitAll()
                        .requestMatchers("/api/partenaires-economiques/reset-password").permitAll()
                        .requestMatchers("/api/admin/register").permitAll()
                        .requestMatchers("/api/admin/login").permitAll()
                        .requestMatchers("/api/admin/refresh").permitAll()
                        .requestMatchers("/api/admin/logout").permitAll()
                        .requestMatchers("/api/admin/forgot-password").permitAll()
                        .requestMatchers("/api/admin/reset-password").permitAll()
                        .requestMatchers("/api/international-companies/register").permitAll()
                        .requestMatchers("/api/international-companies/login").permitAll()
                        .requestMatchers("/api/international-companies/refresh").permitAll()
                        .requestMatchers("/api/international-companies/logout").permitAll()
                        .requestMatchers("/api/international-companies/forgot-password").permitAll()
                        .requestMatchers("/api/international-companies/reset-password").permitAll()
                        .requestMatchers("/api/collaboration-services/**").permitAll()

                        // Toute autre requête nécessite une authentification
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
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