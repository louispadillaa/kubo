package com.kubo.purchase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kubo.purchase.dto.PurchaseInitiated;
import com.kubo.purchase.dto.StartPurchaseRequest;
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
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PurchaseService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisMessageListenerContainer listenerContainer;
    private final SimpMessagingTemplate wsTemplate;
    private final ObjectMapper objectMapper;
    private final WebClient.Builder webClientBuilder;

    //@Value("${kubo.scraping-service.url}")
    //private String scrapingServiceUrl;

    /**
     * Inicia el flujo de compra automatizada con Playwright.
     *
     * RESTRICCIÓN LEGAL: el proceso SIEMPRE se detiene antes del pago.
     * Playwright agrega al carrito y lleva al usuario al checkout.
     * El usuario paga directamente en el sitio de la tienda.
     */
    public PurchaseInitiated startPurchase(StartPurchaseRequest request, String userId) {
        UUID sessionId = UUID.randomUUID();
        log.info("Iniciando sesión de compra sessionId={} store={}", sessionId, request.store());

        // Estado en Redis con TTL de 60 minutos
        redisTemplate.opsForValue().set(
                "purchase:session:" + sessionId,
                Map.of("status", "INITIATED", "userId", userId, "store", request.store()),
                Duration.ofMinutes(60)
        );

        // Enviar job al Playwright agent en el scraping-service
        var body = Map.of(
                "session_id", sessionId.toString(),
                "store",      request.store(),
                "items",      request.items(),
                "user_id",    userId
        );

        webClientBuilder.build()
                .post()
                //.uri(scrapingServiceUrl + "/purchase")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(5))
                .doOnError(e -> log.error("Error enviando job de compra: {}", e.getMessage()))
                .subscribe();

        // Suscribir a eventos del Playwright agent
        subscribeToSessionEvents(sessionId.toString(), userId);

        return new PurchaseInitiated(
                sessionId,
                request.store(),
                "Playwright está preparando tu carrito. Los eventos llegarán en tiempo real.",
                "Kubo NO automatiza pagos. Serás dirigido al checkout para completar tu compra directamente."
        );
    }

    private void subscribeToSessionEvents(String sessionId, String userId) {
        String channel = "purchase:events:" + sessionId;

        MessageListener listener = new MessageListener() {
            @Override
            public void onMessage(Message message, byte[] pattern) {
                try {
                    Map<?, ?> event = objectMapper.readValue(new String(message.getBody()), Map.class);
                    String type = (String) event.get("type");

                    wsTemplate.convertAndSendToUser(userId, "/queue/purchase/" + sessionId, event);

                    if ("CART_READY".equals(type) || "FAILED".equals(type)) {
                        listenerContainer.removeMessageListener(this, new ChannelTopic(channel));
                        log.info("Sesión de compra {} finalizada: {}", sessionId, type);
                    }
                } catch (Exception e) {
                    log.error("Error en evento de compra {}: {}", sessionId, e.getMessage());
                }
            }
        };

        listenerContainer.addMessageListener(listener, new ChannelTopic(channel));
    }
}
