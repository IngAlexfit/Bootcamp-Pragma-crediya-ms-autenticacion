package co.crediya.msautenticacion.api;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.util.Set;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import co.crediya.msautenticacion.api.dto.UsuarioRequest;
import co.crediya.msautenticacion.api.mapper.UsuarioDTOMapper;
import co.crediya.msautenticacion.model.usuario.Usuario;
import co.crediya.msautenticacion.usecase.usuario.registrarusuario.interfaces.IRegistrarUsuarioUseCase;
import jakarta.validation.ConstraintViolation;
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
    /**
     * Procesa la petición de registro de usuario.
     * Valida los datos y responde con el usuario creado o error.
     *
     * @param request Petición HTTP
     * @return Mono<ServerResponse> respuesta HTTP
     */
    public Mono<ServerResponse> registrarUsuario(ServerRequest request) {
        return request.bodyToMono(UsuarioRequest.class)
                .flatMap(this::validarUsuarioRequest)
                .flatMap(this::registrarYResponder)
                .switchIfEmpty(ServerResponse.badRequest().bodyValue(Map.of(ERROR_KEY, "El body no puede estar vacío")))
                .onErrorResume(this::manejarError);
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
            String errorMsg = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Datos inválidos");
            log.warn("Validación fallida: {}", errorMsg);
            return Mono.error(new IllegalArgumentException(errorMsg));
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
     * Maneja los errores ocurridos durante el procesamiento de la petición.
     *
     * @param e Throwable excepción lanzada
     * @return Mono<ServerResponse> con el mensaje de error en formato JSON
     */
    private Mono<ServerResponse> manejarError(Throwable e) {
        if (e instanceof IllegalArgumentException) {
            log.warn("Error procesando la petición: {}", e.getMessage());
            return ServerResponse.badRequest().bodyValue(Map.of(ERROR_KEY, e.getMessage()));
        }
        log.error("Error procesando la petición: {}", e.getMessage(), e);
        return ServerResponse.badRequest().bodyValue(Map.of(ERROR_KEY,
                "Ocurrió un error procesando la solicitud. Verifique los datos e intente nuevamente o contacte a soporte."));
    }

}
