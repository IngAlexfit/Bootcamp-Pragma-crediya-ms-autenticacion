package co.crediya.msautenticacion.api;

import lombok.RequiredArgsConstructor;

import java.util.Set;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;

import co.crediya.msautenticacion.api.dto.UsuarioRequest;
import co.crediya.msautenticacion.api.mapper.UsuarioDTOMapper;
import co.crediya.msautenticacion.model.usuario.Usuario;
import co.crediya.msautenticacion.usecase.usuario.registrarusuario.interfaces.IRegistrarUsuarioUseCase;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler para las peticiones HTTP relacionadas con usuarios.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Handler {

        private final IRegistrarUsuarioUseCase registrarUsuarioUseCase;
        private final Validator validator;
        private final UsuarioDTOMapper usuarioDTOMapper;
        private static final String ERROR_KEY = "error";

        // Helper: convierte null a "", para evitar NPE al construir el body
        private static String s(Object o) {
                return o == null ? "" : String.valueOf(o);
        }

        /**
         * Procesa la petición de registro de usuario.
         * Valida los datos y responde con el usuario creado o error.
         *
         * @param request Petición HTTP
         * @return Mono<ServerResponse> respuesta HTTP
         */
        public Mono<ServerResponse> registrarUsuario(ServerRequest request) {
                return request.bodyToMono(UsuarioRequest.class)
                                .switchIfEmpty(Mono.error(new IllegalArgumentException("El body no puede estar vacío")))
                                .flatMap(dto -> Mono.defer(() -> validarUsuarioRequest(dto)
                                                .flatMap(this::registrarYResponder)
                                                .onErrorResume(this::manejarError) // errores dentro del flujo
                                ).contextWrite(ctx -> dto.getEmail() == null ? ctx : ctx.put("email", dto.getEmail())))
                                .onErrorResume(this::manejarError); // errores antes del flatMap (decoding, body vacío)
        }

        /**
         * Valida los datos recibidos en el DTO de usuario.
         *
         * @param dto UsuarioRequest con los datos a validar
         * @return Mono<UsuarioRequest> con el DTO válido o error si hay violaciones
         */
        private Mono<UsuarioRequest> validarUsuarioRequest(UsuarioRequest dto) {
                Set<ConstraintViolation<UsuarioRequest>> violations = validator.validate(dto);
                if (!violations.isEmpty()) {
                        return Mono.error(new jakarta.validation.ConstraintViolationException(violations));
                }
                return Mono.just(dto);
        }

        /**
         * Registra el usuario y construye la respuesta HTTP.
         *
         * @param dto UsuarioRequest con los datos ya validados
         * @return Mono<ServerResponse> con el usuario creado en formato JSON
         */
        private Mono<ServerResponse> registrarYResponder(UsuarioRequest dto) {
                Usuario usuario = usuarioDTOMapper.toModel(dto);
                return registrarUsuarioUseCase.registrar(usuario)
                                .doOnSuccess(u -> log.info("Usuario registrado exitosamente: {}", u))
                                .map(usuarioDTOMapper::toResponse)
                                .flatMap(u -> ServerResponse.ok().contentType(MediaType.APPLICATION_JSON).bodyValue(u));
        }

        /**
         * Mapea excepciones conocidas a respuestas HTTP sin usar un ExceptionHandler
         * global.
         * Regla: todo error atribuible al request del cliente → 400; errores
         * imprevistos del servidor → 500.
         * Nota: el orden de los if importa; se manejan primero los casos más
         * específicos de 400.
         */
        Mono<ServerResponse> manejarError(Throwable e) {
                // 1) Validación Bean Validation -> objeto estructurado con lista de errores
                if (e instanceof jakarta.validation.ConstraintViolationException cve) {
                        var errors = new ArrayList<Map<String, Object>>();
                        cve.getConstraintViolations().forEach(v -> {
                                Map<String, Object> item = new LinkedHashMap<>();
                                item.put("field", s(v.getPropertyPath()));
                                item.put("message", s(v.getMessage()));
                                errors.add(item);
                        });
                        Map<String, Object> body = new LinkedHashMap<>();
                        body.put("status", 400);
                        body.put("error", "Bad Request");
                        body.put("message", "Validation failed");
                        body.put("errors", errors);
                        log.warn("400 (validation) -> {} error(es)", errors.size());
                        return ServerResponse.badRequest()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(body);
                }

                // 2) Body inválido por binding/formato -> tus tests esperan $.error conteniendo
                // el texto
                if (e instanceof org.springframework.web.server.ServerWebInputException swe) {
                        // Si la causa es DecodingException, devolver el mensaje esperado por el test 2
                        if (swe.getCause() instanceof org.springframework.core.codec.DecodingException) {
                                Map<String, Object> body = new LinkedHashMap<>();
                                body.put("error",
                                                "No se pudo leer el cuerpo de la solicitud (JSON inválido o tipos incorrectos).");
                                log.warn("400 (decode) -> JSON inválido");
                                return ServerResponse.badRequest()
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .bodyValue(body);
                        }
                        String reason = s(swe.getReason());
                        String msg = "Cuerpo de solicitud inválido" + (reason.isBlank() ? "" : (": " + reason));
                        Map<String, Object> body = new LinkedHashMap<>();
                        body.put("error", msg);
                        log.warn("400 (input) -> {}", msg);
                        return ServerResponse.badRequest()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(body);
                }

                // 3) JSON mal formado explícito (por si llega directo)
                if (e instanceof org.springframework.core.codec.DecodingException) {
                        Map<String, Object> body = new LinkedHashMap<>();
                        body.put("error",
                                        "No se pudo leer el cuerpo de la solicitud (JSON inválido o tipos incorrectos).");
                        log.warn("400 (decode) -> JSON inválido");
                        return ServerResponse.badRequest()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(body);
                }

                // 4) Reglas de negocio (IllegalArgumentException) -> tests esperan $.error con
                // el mensaje
                if (e instanceof IllegalArgumentException iae) {
                        String msg = s(iae.getMessage());
                        Map<String, Object> body = new LinkedHashMap<>();
                        body.put("error", msg.isBlank() ? "Solicitud inválida." : msg);
                        // si quieres mapear duplicado a 409, puedes hacerlo aquí; por ahora 400 para
                        // alinear con tests
                        log.warn("400 (business) -> {}", body.get("error"));
                        return ServerResponse.badRequest()
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .bodyValue(body);
                }

                // 5) Fallback 500
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("error", "Error interno del servidor");
                log.error("500 -> {}", e.getMessage(), e);
                return ServerResponse.status(500)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(body);
        }

}
