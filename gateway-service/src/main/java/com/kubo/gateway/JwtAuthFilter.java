package com.kubo.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.server.mvc.common.AbstractGatewayDiscoverer;
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

                            // Extraer userId (sub del JWT)
                            String userId = jwt.getSubject();

                            // Extraer roles del claim realm_access.roles de Keycloak
                            String userPlan = extractPlan(jwt);

                            log.debug("JWT válido: userId={} plan={}", userId, userPlan);

                            // Propagar como headers hacia el servicio destino
                            ServerHttpRequest mutatedRequest = exchange.getRequest()
                                    .mutate()
                                    .header("X-User-Id",   userId)
                                    .header("X-User-Plan", userPlan)
                                    .header("X-User-Email", jwt.getClaimAsString("email"))
                                    .build();

                            return chain.filter(exchange.mutate().request(mutatedRequest).build());
                        })
                        // Si no hay contexto de seguridad (no debería pasar por la config),
                        // continuar sin headers (el servicio verá userId null)
                        .switchIfEmpty(chain.filter(exchange));
    }

    /**
     * Determina el plan del usuario según sus roles en Keycloak.
     * ROLE_PREMIUM → "PREMIUM"
     * Cualquier otro → "FREE"
     */
    private String extractPlan(Jwt jwt) {
        try {
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess == null) return "FREE";

            @SuppressWarnings("unchecked")
            Collection<String> roles = (Collection<String>) realmAccess.get("roles");
            if (roles == null) return "FREE";

            return roles.contains("ROLE_PREMIUM") ? "PREMIUM" : "FREE";
        } catch (Exception e) {
            return "FREE";
        }
    }
}
