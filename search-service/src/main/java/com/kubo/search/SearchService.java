package com.kubo.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kubo.search.dto.CompareInitiated;
import com.kubo.search.dto.CompareRequest;
import com.kubo.search.dto.ScrapingJobRequest;
import com.kubo.search.dto.ScrapingJobResponse;
import io.lettuce.core.dynamic.annotation.Value;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;
    private final SimpMessagingTemplate wsTemplate;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    //@Value("${kubo.scraping-service.url}")
    private String scrapingServiceUrl;

    private static final List<String> ALL_STORES =
            List.of("MERCADOLIBRE", "EXITO", "ALKOSTO", "OLIMPICA", "FALABELLA", "D1", "ARA");

    /**
     * Inicia una comparación de precios:
     * 1. Genera jobId y guarda estado en Redis (TTL 30 min)
     * 2. Envía el job al scraping-service Python vía WebClient (no bloqueante)
     * 3. Suscribe al canal Redis Pub/Sub donde el scraper publicará resultados
     * 4. Retorna el jobId al frontend INMEDIATAMENTE — los resultados llegan por WS
     */
    public CompareInitiated compare(CompareRequest request, String userId) {
        UUID jobId = UUID.randomUUID();
        List<String> stores = (request.stores() != null && !request.stores().isEmpty())
                ? request.stores() : ALL_STORES;

        log.info("Iniciando job jobId={} userId={} productos={} tiendas={}",
                jobId, userId, request.productNames().size(), stores.size());

        // Estado en Redis con TTL de 30 minutos
        redisTemplate.opsForValue().set(
                "job:" + jobId,
                Map.of("status", "PENDING", "userId", userId),
                Duration.ofMinutes(30)
        );

        // Llamar al scraping-service Python (non-blocking)
        var payload = new ScrapingJobRequest(jobId.toString(), userId, request.productNames(), stores);

        webClientBuilder.build()
                .post()
                .uri(scrapingServiceUrl + "/scrape")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(ScrapingJobResponse.class)
                .timeout(Duration.ofSeconds(5))
                .doOnError(e -> log.error("Error llamando al scraping-service: {}", e.getMessage()))
                .subscribe(r -> log.info("Scraping service confirmó jobId={} status={}", jobId, r.status()));

        // Suscribir al canal de resultados de este job
        subscribeToJobResults(jobId.toString(), userId);

        return new CompareInitiated(
                jobId,
                "Búsqueda iniciada. Los resultados llegarán por WebSocket.",
                stores.size()
        );
    }

    /**
     * Suscripción a Redis Pub/Sub.
     * El scraper publica en 'results:{jobId}' — nosotros escuchamos y reenviamos al WS del usuario.
     */
    private void subscribeToJobResults(String jobId, String userId) {
        String channel = "results:" + jobId;

        MessageListener listener = new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                try {
                    Map<?, ?> event = objectMapper.readValue(new String(message.getBody()), Map.class);
                    String status = (String) event.get("status");

                    log.debug("Evento jobId={} status={}", jobId, status);

                    // Reenviar al topic privado del usuario
                    wsTemplate.convertAndSendToUser(userId, "/queue/results/" + jobId, event);

                    // Job terminado → limpiar suscripción
                    if ("COMPLETED".equals(status) || "FAILED".equals(status)) {
                        listenerContainer.removeMessageListener(this, new ChannelTopic(channel));
                        redisTemplate.delete("job:" + jobId);
                        log.info("Job {} finalizado con status={}", jobId, status);
                    }
                } catch (Exception e) {
                    log.error("Error procesando evento del job {}: {}", jobId, e.getMessage());
                }
            }
        };

        listenerContainer.addMessageListener(listener, new ChannelTopic(channel));
    }
}
