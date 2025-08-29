package co.crediya.msautenticacion.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;
import reactor.util.context.ContextView;

import java.util.HashMap;
import java.util.Map;

/**
 * Puente Reactor Context -> SLF4J MDC para aplicaciones WebFlux.
 *
 * Por qué es necesaria:
 * - SLF4J MDC usa ThreadLocal, pero en programación reactiva un flujo puede
 *   ejecutarse en múltiples hilos (reactor-http-nio-N, reactor-tcp-nio-N).
 * - Sin este puente, las claves de MDC (correlationId, email, etc.) no
 *   aparecen en logs emitidos fuera del hilo inicial (por ejemplo, los del
 *   driver R2DBC), resultando en "[cid:na] [email:na]".
 *
 * Cómo funciona:
 * - Registra un hook global (Hooks.onEachOperator) que envuelve cada operador
 *   con un suscriptor que copia el Reactor Context actual al MDC justo antes de
 *   propagar onNext/onError/onComplete, y lo limpia después.
 * - Tú solo debes colocar tus valores (p. ej. "correlationId", "email") en
 *   Reactor Context con .contextWrite(ctx -> ctx.put("clave", valor)).
 *   Este hook se encarga de que estén disponibles en MDC para el patrón de log.
 *
 * Cuándo mantenerla:
 * - cuando se quiere que correlationId/email salgan en logs de cualquier hilo (driver,
 *   operadores reactivos, filtros), déjala.
 *
 * Cuándo podrías omitirla:
 * - Si no se usa MDC en el patrón de log o no te importa ver "na" en logs fuera
 *   del Handler/WebFilter.
 *
 * Notas de rendimiento:
 * - Copiar todo el Context a MDC en cada señal tiene un coste pequeño.
 *   Si te preocupa, filtra solo las claves necesarias (ver comentario en copyToMdc).
 */
@Configuration
public class ReactiveMdcConfig {

    /**
     * Clave única para registrar y luego limpiar el hook.
     */
    private static final String HOOK_KEY = "mdcContextHook";

    /**
     * Registra el hook global al inicializar el contexto Spring.
     */
    @PostConstruct
    public void setupHook() {
        // Operators.lift nos permite envolver el suscriptor aguas abajo
        Hooks.onEachOperator(HOOK_KEY, Operators.lift((sc, sub) -> new MdcContextLifter<>(sub)));
    }

    /**
     * Limpia el hook y el MDC al cerrar el contexto Spring.
     */
    @PreDestroy
    public void cleanupHook() {
        Hooks.resetOnEachOperator(HOOK_KEY);
        MDC.clear();
    }

    /**
     * Suscriptor que, en cada señal (onNext/onError/onComplete),
     * copia el Reactor Context -> MDC justo antes de delegar,
     * y limpia el MDC al terminar para evitar fugas entre peticiones.
     */
    static final class MdcContextLifter<T> implements CoreSubscriber<T> {
        final CoreSubscriber<T> actual;

        MdcContextLifter(CoreSubscriber<T> actual) { this.actual = actual; }

        @Override public void onSubscribe(Subscription s) { actual.onSubscribe(s); }

        @Override public void onNext(T t) {
            copyToMdc(actual.currentContext());
            try { actual.onNext(t); }
            finally { MDC.clear(); } // importante: no contaminar otros hilos/peticiones
        }

        @Override public void onError(Throwable t) {
            copyToMdc(actual.currentContext());
            try { actual.onError(t); }
            finally { MDC.clear(); }
        }

        @Override public void onComplete() {
            copyToMdc(actual.currentContext());
            try { actual.onComplete(); }
            finally { MDC.clear(); }
        }

        @Override public Context currentContext() { return actual.currentContext(); }

        /**
         * Copia todas las entradas del Reactor Context al MDC.
         *
         * Optimización opcional:
         * - ejemplo Si solo interesan "correlationId" y "email", se podría filtrar:
         *     Object cid = ctx.getOrDefault("correlationId", null);
         *     Object mail = ctx.getOrDefault("email", null);
         *     if (cid != null) MDC.put("correlationId", String.valueOf(cid));
         *     if (mail != null) MDC.put("email", String.valueOf(mail));
         *   Esto reduce el tamaño del mapa y el coste por señal.
         */
        private void copyToMdc(ContextView ctx) {
            if (ctx == null || ctx.isEmpty()) return;

            Map<String, String> map = new HashMap<>();
            // Convertimos todas las entradas a String para MDC
            ctx.stream().forEach(e -> map.put(String.valueOf(e.getKey()), String.valueOf(e.getValue())));

            // Reemplazamos el mapa completo de MDC (más seguro que put parciales).
            MDC.setContextMap(map);
        }
    }
}