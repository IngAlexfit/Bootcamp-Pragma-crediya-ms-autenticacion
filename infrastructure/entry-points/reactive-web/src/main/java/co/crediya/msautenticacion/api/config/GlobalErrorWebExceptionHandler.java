package co.crediya.msautenticacion.api.config;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.UnexpectedTypeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
@Component
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {
    @Override
    public @NonNull Mono<Void> handle(@NonNull ServerWebExchange exchange, @NonNull Throwable ex) {
        String path = exchange.getRequest().getPath().value();
        HttpStatus status;
        String message;

        log.debug("Handling exception at {} -> {}: {}", path, ex.getClass().getName(), ex.getMessage(), ex);

        if (ex instanceof IllegalArgumentException iae) {
            status = HttpStatus.BAD_REQUEST;
            message = iae.getMessage();
            log.warn("400 at {} -> {}", path, message);
        } else if (ex instanceof ConstraintViolationException cve) {
            status = HttpStatus.BAD_REQUEST;
            message = cve.getConstraintViolations().stream()
                .map(v -> (v.getPropertyPath() != null ? v.getPropertyPath().toString() + ": " : "") + v.getMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Solicitud inválida");
            log.warn("400 (validation) at {} -> {}", path, message);
        } else if (ex instanceof UnexpectedTypeException ute) {
            status = HttpStatus.BAD_REQUEST;
            message = "Validación inválida en el DTO: " + ute.getMessage();
            log.warn("400 (unexpected type) at {} -> {}", path, message);
        } else if (ex instanceof ServerWebInputException swe) {
            status = HttpStatus.BAD_REQUEST;
            message = "Cuerpo de solicitud inválido: " + swe.getReason();
            log.warn("400 (input) at {} -> {}", path, message);
        } else if (ex instanceof DecodingException) {
            status = HttpStatus.BAD_REQUEST;
            message = "No se pudo leer el cuerpo de la solicitud (JSON inválido o tipos incorrectos).";
            log.warn("400 (decode) at {} -> {}", path, message);
        } else if (ex instanceof UnsupportedMediaTypeStatusException) {
            status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            message = "Content-Type no soportado. Usa application/json.";
            log.warn("415 at {} -> {}", path, message);
        } else if (ex instanceof NotAcceptableStatusException) {
            status = HttpStatus.NOT_ACCEPTABLE;
            message = "Accept no soportado por el servidor.";
            log.warn("406 at {} -> {}", path, message);
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = "Error interno del servidor";
            log.error("500 at {} -> {}: {}", path, ex.getClass().getName(), ex.getMessage(), ex);
        }

        var response = exchange.getResponse();
        if (response.isCommitted()) {
            log.warn("Response already committed for path {}. Propagating original exception.", path);
            return Mono.error(ex);
        }
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String json = """
            {"timestamp":"%s","status":%d,"error":"%s","path":"%s"}
            """.formatted(Instant.now(), status.value(), escape(message), path);

        DataBufferFactory bufferFactory = response.bufferFactory();
        var buffer = bufferFactory.wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}