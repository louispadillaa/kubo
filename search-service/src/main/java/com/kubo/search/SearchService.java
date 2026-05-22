package com.kubo.search;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kubo.search.dto.*;
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
import java.util.Collections;
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
    private final ProductClient productClient;

    private final String scrapingServiceUrl = "http://localhost:8000";

    public List<ProductSuggestResponse> suggest(String query) {
        String cacheKey = "suggest:" + query.trim().toLowerCase();

        // 1. PASO A: INTENTAR LEER DESDE REDIS CACHÉ
        try {
            Object cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                log.info("Redis Caché HIT para sugerencia: '{}'", query);
                return objectMapper.convertValue(cachedData, new TypeReference<List<ProductSuggestResponse>>() {});
            }
        } catch (Exception e) {
            log.error("Error al leer desde la caché de Redis: {}", e.getMessage());
        }

        log.info("🔍 Redis Caché MISS. Consultando catálogo local en product-service para: '{}'", query);

        // 2. PASO B: CONSULTAR SI PRODUCT-SERVICE TIENE HISTORIAL EN SU BASE DE DATOS
        List<ProductSuggestResponse> localSuggestions = Collections.emptyList();
        try {
            localSuggestions = productClient.getProcessedSuggestions(query);
        } catch (Exception e) {
            log.error("Fallo crítico de comunicación Feign con product-service: {}", e.getMessage());
        }

        if (localSuggestions != null && !localSuggestions.isEmpty()) {
            log.info("Datos recuperados desde PostgreSQL (catálogo). Poblando caché de Redis.");
            saveInRedisCache(cacheKey, localSuggestions);
            return localSuggestions;
        }

        // 3. PASO C: FALLBACK GENERAL - DISPARAR SCRAPING CONCURRENTE EN FASTAPI (PLAYWRIGHT)
        log.warn("🌐 Sin registros en BD. Invocando motores de Playwright en FastAPI de forma síncrona para: '{}'", query);
        List<PythonScrapingResponse> pythonOffers = Collections.emptyList();
        try {
            pythonOffers = webClientBuilder.build()
                    .get()
                    .uri(scrapingServiceUrl + "/api/v1/scraping/search?q={q}", query)
                    .retrieve()
                    .bodyToFlux(PythonScrapingResponse.class)
                    .collectList()
                    .block(Duration.ofSeconds(30)); // Timeout controlado para esperar Playwright
        } catch (Exception e) {
            log.error("Error de conexión o timeout con el servicio Python FastAPI: {}", e.getMessage());
        }

        if (pythonOffers == null || pythonOffers.isEmpty()) {
            log.warn("El scraper de FastAPI no encontró ofertas en internet para: '{}'", query);
            return Collections.emptyList();
        }

        // 4. PASO D: MAPEAR AL COMANDO BULK Y ENVIARLO A PRODUCT-SERVICE PARA PROCESAMIENTO Y PERSISTENCIA
        List<ProductSnapshotBulkCommand> bulkCommands = pythonOffers.stream()
                .map(offer -> new ProductSnapshotBulkCommand(
                        offer.nameRaw(),
                        offer.store(),
                        offer.price(),
                        offer.url(),
                        offer.imageUrl(),
                        query // Envía el término original para relacionar/crear el producto base
                )).toList();

        try {
            log.info("Enviando lote de {} snapshots a product-service vía Feign para persistir...", bulkCommands.size());
            productClient.saveBulkSnapshots(bulkCommands);
        } catch (Exception e) {
            log.error("Error al persistir el lote en la base de datos de product-service: {}", e.getMessage());
        }

        // 5. PASO E: RECUPERAR LOS DATOS MADUROS YA PROCESADOS POR TU CATÁLOGO
        log.info("Re-consultando sugerencias maduras del catálogo procesado para: '{}'", query);
        List<ProductSuggestResponse> finalProcessedResponse = Collections.emptyList();
        try {
            finalProcessedResponse = productClient.getProcessedSuggestions(query);
        } catch (Exception e) {
            log.error("Error al recuperar los datos post-persistencia de product-service: {}", e.getMessage());
        }

        // 6. PASO F: COMPARTIR EN REDIS PARA FUTURAS CONSULTAS RÁPIDAS
        if (finalProcessedResponse != null && !finalProcessedResponse.isEmpty()) {
            saveInRedisCache(cacheKey, finalProcessedResponse);
        }

        return finalProcessedResponse;
    }

    /**
     * Helper centralizado para persistir en Redis con expiración temporal (TTL).
     */
    private void saveInRedisCache(String key, List<ProductSuggestResponse> data) {
        try {
            // Guardamos en caché por 2 horas para evitar spammear/bloquear los e-commerce constantemente
            redisTemplate.opsForValue().set(key, data, Duration.ofHours(2));
            log.info("Caché de Redis actualizada exitosamente para la llave: {}", key);
        } catch (Exception e) {
            log.error("Error al escribir en la caché de Redis: {}", e.getMessage());
        }
    }

    // =====================================================================
    // MÉTODOS ASÍNCRONOS EXISTENTES (COMPARE / PUB-SUB WEBSOCKETS) INTACTOS
    // =====================================================================

    public CompareInitiated compare(CompareRequest request, String userId) {
        UUID jobId = UUID.randomUUID();

        // CORRECCIÓN: Si request.stores() es nulo o vacío, pasamos una lista vacía
        // o manejas la lógica que tenías originalmente antes de que yo interviniera.
        List<String> stores = (request.stores() != null) ? request.stores() : Collections.emptyList();

        log.info("Iniciando job jobId={} userId={} productos={} tiendas={}",
                jobId, userId, request.productNames().size(), stores.size());

        redisTemplate.opsForValue().set(
                "job:" + jobId,
                Map.of("status", "PENDING", "userId", userId),
                Duration.ofMinutes(30)
        );

        // ... El resto del método compare se queda exactamente igual ...

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

        subscribeToJobResults(jobId.toString(), userId);

        return new CompareInitiated(jobId, "Búsqueda iniciada. Los resultados llegarán por WebSocket.", stores.size());
    }

    private void subscribeToJobResults(String jobId, String userId) {
        String channel = "results:" + jobId;

        MessageListener listener = new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                try {
                    Map<?, ?> event = objectMapper.readValue(new String(message.getBody()), Map.class);
                    String status = (String) event.get("status");

                    log.debug("Evento jobId={} status={}", jobId, status);
                    wsTemplate.convertAndSendToUser(userId, "/queue/results/" + jobId, event);

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