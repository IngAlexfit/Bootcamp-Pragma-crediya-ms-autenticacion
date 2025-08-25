package co.crediya.msautenticacion.api;

import co.crediya.msautenticacion.api.config.GlobalErrorWebExceptionHandler;
import co.crediya.msautenticacion.api.dto.UsuarioRequest;
import co.crediya.msautenticacion.api.dto.UsuarioResponse;
import co.crediya.msautenticacion.api.mapper.UsuarioDTOMapper;
import co.crediya.msautenticacion.model.usuario.Usuario;
import co.crediya.msautenticacion.usecase.usuario.registrarusuario.interfaces.IRegistrarUsuarioUseCase;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.springframework.test.web.reactive.server.HttpHandlerConnector;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Clase de pruebas para UsuarioRouterHandler.
 * Realiza pruebas unitarias sobre los endpoints de usuario.
 * Utiliza WebTestClient para simular peticiones HTTP y Mockito para mocks.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioRouterHandlerTest {
    /**
     * Cliente de pruebas para realizar peticiones HTTP.
     */
    private WebTestClient webTestClient;

    /**
     * Caso de uso para registrar usuario (mock).
     */
    private IRegistrarUsuarioUseCase registrarUsuarioUseCase;
    /**
     * Validador de datos (mock).
     */
    private Validator validator;
    /**
     * Mapper de DTO de usuario (mock).
     */
    private UsuarioDTOMapper usuarioDTOMapper;

    /**
     * Construye un objeto UsuarioRequest de prueba.
     * @return UsuarioRequest con datos de ejemplo
     */
    private UsuarioRequest buildRequest() {
        System.out.println("Ejecutando buildRequest()");
        UsuarioRequest req = new UsuarioRequest();
        req.setNombre("Juan");
        req.setApellido("Pérez");
        req.setFechaNacimiento(LocalDate.now());
        req.setTelefono("3001234567");
        req.setEmail("juan.perez@example.com");
        req.setSalarioBase(new BigDecimal("1200000"));
        return req;
    }

    /**
     * Construye un modelo Usuario a partir de un UsuarioRequest.
     * @param req UsuarioRequest
     * @return Usuario
     */
    private Usuario buildModelFromReq(UsuarioRequest req) {
        System.out.println("Ejecutando buildModelFromReq()");
        return Usuario.builder()
                .userId(null)
                .nombre(req.getNombre())
                .apellido(req.getApellido())
                .fechaNacimiento(req.getFechaNacimiento())
                .telefono(req.getTelefono())
                .email(req.getEmail())
                .salarioBase(req.getSalarioBase())
                .build();
    }

    /**
     * Configura los mocks y el WebTestClient antes de cada prueba.
     */
    @BeforeEach
    void setup() {
        System.out.println("Ejecutando setup()");
        registrarUsuarioUseCase = mock(IRegistrarUsuarioUseCase.class);
        validator = mock(Validator.class);
        usuarioDTOMapper = mock(UsuarioDTOMapper.class);

        Handler handler = new Handler(registrarUsuarioUseCase, validator, usuarioDTOMapper);
        RouterRest routerRest = new RouterRest();
        RouterFunction<ServerResponse> router = routerRest.routerFunction(handler);

        var webHandler = RouterFunctions.toWebHandler(router);
        HttpHandler httpHandler = WebHttpHandlerBuilder.webHandler(webHandler)
                .exceptionHandler(new GlobalErrorWebExceptionHandler())
                .build();

        this.webTestClient = WebTestClient.bindToServer(new HttpHandlerConnector(httpHandler)).build();
    }

    /**
     * Prueba el endpoint POST /api/v1/usuarios para registro exitoso.
     */
    @Test
    @DisplayName("POST /api/v1/usuarios - éxito")
    void registrarUsuario_ok() {
        System.out.println("Ejecutando registrarUsuario_ok()");
        // Arrange
        UsuarioRequest req = buildRequest();
        Usuario toSave = buildModelFromReq(req);
        Usuario saved = toSave.toBuilder().userId(UUID.randomUUID()).build();
        UsuarioResponse response = new UsuarioResponse();
        response.setUserId(saved.getUserId());
        response.setNombre(saved.getNombre());
        response.setApellido(saved.getApellido());
        response.setFechaNacimiento(saved.getFechaNacimiento());
        response.setTelefono(saved.getTelefono());
        response.setEmail(saved.getEmail());
        response.setSalarioBase(saved.getSalarioBase());

        when(usuarioDTOMapper.toModel(any(UsuarioRequest.class))).thenReturn(toSave);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class))).thenReturn(Mono.just(saved));
        when(usuarioDTOMapper.toResponse(any(Usuario.class))).thenReturn(response);

        // Act + Assert
        webTestClient.post()
                .uri("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.userId").isNotEmpty()
                .jsonPath("$.email").isEqualTo(req.getEmail());
        System.out.println("FIN registrarUsuario_ok()");

    }

     /**
     * Prueba el endpoint POST /api/v1/usuarios para error de validación.
     */
    @Test
    @DisplayName("POST /api/v1/usuarios - error de validación")
    void registrarUsuario_validationError() {
        System.out.println("Ejecutando registrarUsuario_validationError()");

        @SuppressWarnings("unchecked")
        ConstraintViolation<UsuarioRequest> violation = Mockito.mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("El nombre es obligatorio");
        when(validator.validate(any(UsuarioRequest.class))).thenReturn(Set.of(violation));
   
        UsuarioRequest req = buildRequest();
        req.setNombre("");

        // Act + Assert
        webTestClient.post()
                .uri("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isNotEmpty();
        System.out.println("FIN registrarUsuario_validationError()");

    }

    /**
     * Prueba el endpoint POST /api/v1/usuarios cuando el correo ya existe.
     */
    @Test
    @DisplayName("POST /api/v1/usuarios - correo ya existe")
    void registrarUsuario_emailExists() {
        System.out.println("Ejecutando registrarUsuario_emailExists()");
        // Arrange
        UsuarioRequest req = buildRequest();
        Usuario toSave = buildModelFromReq(req);
        when(usuarioDTOMapper.toModel(any(UsuarioRequest.class))).thenReturn(toSave);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class)))
                .thenReturn(Mono.error(new IllegalArgumentException("El correo electrónico ya se encuentra registrado.")));

        // Act + Assert
        webTestClient.post()
                .uri("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("El correo electrónico ya se encuentra registrado.");
        System.out.println("FIN registrarUsuario_emailExists()");

    }
}
