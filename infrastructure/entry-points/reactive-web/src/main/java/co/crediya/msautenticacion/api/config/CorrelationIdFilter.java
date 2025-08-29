package co.crediya.msautenticacion.api.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.lang.NonNull;

import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * WebFilter responsable de gestionar el Correlation ID por petición HTTP.
 *
 * Objetivos:
 * - Generar o reutilizar un identificador de correlación (X-Correlation-Id).
 * - Exponerlo en la respuesta HTTP para trazabilidad entre sistemas.
 * - Propagarlo en el Reactor Context para que esté disponible a lo largo del pipeline reactivo.
 * - Ponerlo también en el MDC (SLF4J) para que aparezca en el patrón de log (%X{correlationId}).
 *
 * Notas:
 * - Está anotado con @Order(HIGHEST_PRECEDENCE) para que se ejecute antes que otros filtros.
 * - MDC es ThreadLocal; para que viaje entre hilos de Reactor/Netty se recomienda mantener
 *   el valor en Reactor Context (contextWrite). Si además usas un hook tipo ReactiveMdcConfig,
 *   éste copiará el Context al MDC en cada hilo automáticamente.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter implements WebFilter {

    /** Clave usada en MDC y Reactor Context. Debe coincidir con el patrón de log (%X{correlationId}). */
    public static final String CORRELATION_ID_KEY = "correlationId";
    /** Nombre del header HTTP para recibir/devolver el correlation id. */
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    /**
     * Aplica el filtro a cada request:
     * - Lee el X-Correlation-Id entrante o genera uno nuevo (UUID).
     * - Lo escribe como header de respuesta.
     * - Lo propaga en Reactor Context y lo coloca en MDC durante el procesamiento.
     */
    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        // 1) Obtener el correlation id del request o generar uno nuevo.
        String cid = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (cid == null || cid.isBlank()) {
            cid = UUID.randomUUID().toString();
        }

        // 2) Devolver el correlation id al cliente (útil para soporte y trazabilidad extremo a extremo).
        exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, cid);
        final String correlationId = cid;

        // 3) Continuar la cadena:
        //    - contextWrite: guarda el correlationId en Reactor Context para todo el pipeline.
        //    - doFirst: pone el correlationId en MDC antes de que se ejecute el resto (para logs locales).
        //    - doFinally: limpia el MDC al terminar, evitando fugas entre peticiones.
        return chain.filter(exchange)
                .contextWrite(ctx -> ctx.put(CORRELATION_ID_KEY, correlationId))
                .doFirst(() -> MDC.put(CORRELATION_ID_KEY, correlationId))
                .doFinally(sig -> MDC.remove(CORRELATION_ID_KEY));
    }
}