package com.kubo.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchanges -> exchanges
                        // Endpoints públicos — no requieren JWT
                        .pathMatchers("/api/products/suggest").permitAll()
                        .pathMatchers("/actuator/health").permitAll()
                        .pathMatchers("/ws/**").permitAll()          // WebSocket handshake
                        .pathMatchers("/fallback/**").permitAll()

                        // Todo lo demás requiere autenticación
                        .anyExchange().authenticated()
                )
                // Configurar como Resource Server que valida JWT
                // Las URLs de Keycloak vienen del application.yaml
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> {}) // La config viene del yaml (issuer-uri + jwk-set-uri)
                );

        return http.build();
    }
}
