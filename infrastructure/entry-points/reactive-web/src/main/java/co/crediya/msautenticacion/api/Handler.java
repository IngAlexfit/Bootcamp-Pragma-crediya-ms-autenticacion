package co.crediya.msautenticacion.api;

import lombok.RequiredArgsConstructor;

import java.util.Set;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

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
                .flatMap(this::validarUsuarioRequest)
                .flatMap(this::registrarYResponder);
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
            throw new ConstraintViolationException(violations);
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

   

}
