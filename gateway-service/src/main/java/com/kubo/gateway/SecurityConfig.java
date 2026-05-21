package com.kubo.gateway;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity severHttpSecurity) {
        severHttpSecurity
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchange -> exchange
                        // Endpoints públicos — no requieren JWT
                        .pathMatchers("/eureka/**").permitAll()
                        .pathMatchers("/api/products/**").permitAll()  // <-- AGREGA ESTA LÍNEA
                        .pathMatchers("/api/purchase/**").permitAll()
                        .anyExchange()
                        .authenticated()
                )
                // Configurar como Resource Server que valida JWT
                // Las URLs de Keycloak vienen del application.yaml
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return severHttpSecurity.build();
    }
}
