package com.kubo.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

@Component
@Slf4j
public class JwtAuthFilter extends AbstractGatewayFilterFactory<Object> {

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) ->
                ReactiveSecurityContextHolder.getContext()
                        .map(ctx -> ctx.getAuthentication())
                        .cast(JwtAuthenticationToken.class)
                        .flatMap(auth -> {
                            Jwt jwt = auth.getToken();

                            // El 'sub' es el ID único e inmutable del usuario en Keycloak
                            String userId = jwt.getSubject();
                            String email = jwt.getClaimAsString("email");

                            log.debug("JWT procesado en Gateway: userId={}, email={}", userId, email);

                            // Propagar los datos esenciales hacia los microservicios aguas abajo
                            ServerHttpRequest mutatedRequest = exchange.getRequest()
                                    .mutate()
                                    .header("X-User-Id", userId)
                                    .header("X-User-Email", email != null ? email : "")
                                    .build();

                            return chain.filter(exchange.mutate().request(mutatedRequest).build());
                        })
                        .switchIfEmpty(chain.filter(exchange));
    }

}
