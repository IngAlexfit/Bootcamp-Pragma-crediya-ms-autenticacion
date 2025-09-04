package co.crediya.msautenticacion.api;

import co.crediya.msautenticacion.api.dto.UsuarioRequest;
import co.crediya.msautenticacion.api.dto.UsuarioResponse;
import co.crediya.msautenticacion.api.mapper.UsuarioDTOMapper;
import co.crediya.msautenticacion.model.usuario.Usuario;
import co.crediya.msautenticacion.usecase.usuario.registrarusuario.interfaces.IRegistrarUsuarioUseCase;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo; // <- para loguear nombre de la prueba
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.test.web.reactive.server.HttpHandlerConnector;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.hasSize; 

/**
 * Pruebas unitarias del Router/Handler de usuarios (WebFlux).
 *
 * Cobertura:
 * - Flujo exitoso de registro (201/200 OK).
 * - Errores de Bean Validation (una y múltiples violaciones).
 * - Errores de decodificación del cuerpo (JSON mal formado).
 * - Errores de binding (tipos/formato de fecha).
 * - Body vacío.
 * - Regla de negocio (email duplicado).
 *
 * Notas:
 * - Se usa WebTestClient contra un HttpHandler in-memory.
 * - Se imprime con System.out para traza rápida durante la ejecución de tests.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioRouterHandlerTest {

    private WebTestClient webTestClient;
    private IRegistrarUsuarioUseCase registrarUsuarioUseCase;
    private Validator validator;
    private UsuarioDTOMapper usuarioDTOMapper;
    @Mock
    private ServerWebExchange exchange;

    @InjectMocks
    private Handler handler;

    private static void tlog(String msg) {
        System.out.println("[TEST] " + msg);
    }

    /**
     * Construye un objeto UsuarioRequest de prueba.
     *
     * @return UsuarioRequest con datos de ejemplo
     */
    private UsuarioRequest buildRequest() {
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
     *
     * @param req UsuarioRequest
     * @return Usuario
     */
    private Usuario buildModelFromReq(UsuarioRequest req) {
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
     * Inicializa WebTestClient y mocks antes de cada prueba.
     * Además, imprime el nombre de la prueba actual.
     */
    @BeforeEach
    void setup(TestInfo testInfo) {
        tlog("Iniciando prueba: " + testInfo.getDisplayName());

        registrarUsuarioUseCase = mock(IRegistrarUsuarioUseCase.class);
        validator = mock(Validator.class);
        usuarioDTOMapper = mock(UsuarioDTOMapper.class);

        Handler handler = new Handler(registrarUsuarioUseCase, validator, usuarioDTOMapper);
        RouterRest routerRest = new RouterRest();
        RouterFunction<ServerResponse> router = routerRest.routerFunction(handler);

        var webHandler = RouterFunctions.toWebHandler(router);
        HttpHandler httpHandler = WebHttpHandlerBuilder.webHandler(webHandler).build();

        this.webTestClient = WebTestClient.bindToServer(new HttpHandlerConnector(httpHandler)).build();
    }

    /**
     * Caso exitoso: se registra un usuario correctamente.
     */
    @Test
    @DisplayName("POST /api/v1/usuarios - éxito")
    void registrarUsuario_ok() {
        UsuarioRequest req = buildRequest();
        tlog("Payload OK -> email=" + req.getEmail());

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
    }

    /**
     * Error de validación con una única violación.
     */
    @Test
    @DisplayName("POST /api/v1/usuarios - error de validación (1 violación)")
    void registrarUsuario_validationError_single() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<UsuarioRequest> violation = Mockito.mock(ConstraintViolation.class);
        Path path = Mockito.mock(Path.class);
        when(path.toString()).thenReturn("nombre");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("El nombre es obligatorio");
        when(validator.validate(any(UsuarioRequest.class))).thenReturn(Set.of(violation));

        UsuarioRequest req = buildRequest();
        req.setNombre("");
        tlog("Payload inválido (1 violación) -> nombre vacío");

        webTestClient.post()
                .uri("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").isEqualTo("Bad Request")
                .jsonPath("$.message").isEqualTo("Validation failed")
                .jsonPath("$.errors[0].field").isEqualTo("nombre")
                .jsonPath("$.errors[0].message").isEqualTo("El nombre es obligatorio");
    }

    /**
     * Error de validación con múltiples violaciones.
     */
    @Test
    @DisplayName("POST /api/v1/usuarios - error de validación (múltiples violaciones)")
    void registrarUsuario_validationError_multiple() {
        @SuppressWarnings("unchecked")
        ConstraintViolation<UsuarioRequest> v1 = Mockito.mock(ConstraintViolation.class);
        Path p1 = Mockito.mock(Path.class);
        when(p1.toString()).thenReturn("email");
        when(v1.getPropertyPath()).thenReturn(p1);
        when(v1.getMessage()).thenReturn("El correo electrónico debe tener un formato válido");

        @SuppressWarnings("unchecked")
        ConstraintViolation<UsuarioRequest> v2 = Mockito.mock(ConstraintViolation.class);
        Path p2 = Mockito.mock(Path.class);
        when(p2.toString()).thenReturn("salarioBase");
        when(v2.getPropertyPath()).thenReturn(p2);
        when(v2.getMessage()).thenReturn("El salario base debe ser menor o igual a 15.000.000");

        when(validator.validate(any(UsuarioRequest.class))).thenReturn(Set.of(v1, v2));

        UsuarioRequest req = buildRequest();
        req.setEmail("invalido");
        req.setSalarioBase(new BigDecimal("20000000"));
        tlog("Payload inválido (múltiples violaciones) -> email y salarioBase");

        webTestClient.post()
                .uri("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors").value(hasSize(2)) // total
                .jsonPath("$.errors[?(@.field=='email')]").value(hasSize(1))
                .jsonPath("$.errors[?(@.field=='salarioBase')]").value(hasSize(1));
    }



    /**
     * JSON mal formado: debe provocar Bad Request con mensaje de decodificación.
     */
    @Test
    @DisplayName("POST /api/v1/usuarios - JSON mal formado (DecodingException)")
    void registrarUsuario_invalidJson_decodingError() {
        String invalidJson = "{ \"nombre\": \"Juan\", "; // truncado -> JSON inválido
        tlog("Enviando JSON mal formado");

        webTestClient.post()
                .uri("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(invalidJson)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").value(containsString("No se pudo leer el cuerpo"));
    }


    /**
     * Cuerpo vacío: debe devolver 400 con mensaje claro.
     */
    @Test
    @DisplayName("POST /api/v1/usuarios - body vacío")
    void registrarUsuario_emptyBody_error() {
        tlog("Enviando body vacío");

        webTestClient.post()
                .uri("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("El body no puede estar vacío");
    }

    /**
     * Regla de negocio: correo ya registrado.
     */
    @Test
    @DisplayName("POST /api/v1/usuarios - correo ya existe (regla de negocio)")
    void registrarUsuario_emailExists() {
        UsuarioRequest req = buildRequest();
        Usuario toSave = buildModelFromReq(req);
        when(usuarioDTOMapper.toModel(any(UsuarioRequest.class))).thenReturn(toSave);
        when(registrarUsuarioUseCase.registrar(any(Usuario.class)))
                .thenReturn(
                        Mono.error(new IllegalArgumentException("El correo electrónico ya se encuentra registrado.")));

        tlog("Simulando correo duplicado -> " + req.getEmail());

        webTestClient.post()
                .uri("/api/v1/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isEqualTo("El correo electrónico ya se encuentra registrado.");
    }

    /**
     * Prueba de integración que verifica el correcto manejo de excepciones DecodingException.
     *
     * Esta prueba configura un router con un filtro que captura errores de decodificación
     * y verifica que el handler los transforma en respuestas HTTP 400 con el mensaje adecuado.
     *
     * @see Handler#manejarError(Throwable)
     * @see DecodingException
     */
    @Test
    @DisplayName("Debe manejar correctamente errores DecodingException - Integración")
    void testManejarErrorDecodingExceptionIntegracion() {
        // Configurar router con handler
        RouterFunction<ServerResponse> router = RouterFunctions.route()
                .POST("/test", request -> {
                    throw new DecodingException("JSON mal formado");
                })
                .filter((request, next) -> {
                    try {
                        return next.handle(request);
                    } catch (DecodingException ex) {
                        return handler.manejarError(ex);
                    }
                })
                .build();

        // Crear WebTestClient
        WebTestClient client = WebTestClient.bindToRouterFunction(router).build();

        // Ejecutar prueba
        client.post().uri("/test")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.error").isEqualTo("No se pudo leer el cuerpo de la solicitud (JSON inválido o tipos incorrectos).");
    }

    /**
     * Prueba unitaria que verifica que el método manejarError procesa correctamente
     * las excepciones de tipo DecodingException.
     *
     * Verifica que la respuesta tenga el código de estado 400 (Bad Request) y el tipo
     * de contenido correcto, sin validar el contenido específico del cuerpo.
     *
     * @see Handler#manejarError(Throwable)
     * @see DecodingException
     */
    @Test
    @DisplayName("Debe manejar correctamente errores DecodingException - Unitario")
    void testManejarErrorDecodingException() {
        // Arrange
        DecodingException exception = new DecodingException("JSON mal formado");

        // Act
        var response = handler.manejarError(exception);

        // Assert
        StepVerifier.create(response)
                .assertNext(serverResponse -> {
                    assertEquals(400, serverResponse.statusCode().value());
                    assertEquals(MediaType.APPLICATION_JSON, serverResponse.headers().getContentType());


                })
                .verifyComplete();
    }

    /**
     * Prueba unitaria para el manejo de excepciones genéricas no específicamente tratadas.
     *
     * Verifica que cualquier excepción no manejada explícitamente se convierte en una
     * respuesta HTTP 500 (Internal Server Error) con el tipo de contenido adecuado.
     *
     * @see Handler#manejarError(Throwable)
     */
    @Test
    @DisplayName("Debe manejar correctamente otros errores como error interno")
    void testManejarErrorGenerico() {
        // Arrange
        RuntimeException exception = new RuntimeException("Error inesperado");

        // Act
        var response = handler.manejarError(exception);

        // Assert
        StepVerifier.create(response)
                .assertNext(serverResponse -> {
                    assertEquals(500, serverResponse.statusCode().value());
                    assertEquals(MediaType.APPLICATION_JSON, serverResponse.headers().getContentType());


                })
                .verifyComplete();
    }

    /**
     * Prueba unitaria que verifica el manejo específico de ServerWebInputException sin mensaje.
     *
     * Este caso particular debe ser manejado correctamente generando una respuesta
     * HTTP 400 (Bad Request) con un mensaje predeterminado cuando el mensaje de
     * la excepción está vacío.
     *
     * @see Handler#manejarError(Throwable)
     * @see ServerWebInputException
     */
    @Test
    @DisplayName("Debe manejar ServerWebInputException con mensaje vacío")
    void testManejarErrorServerWebInputExceptionSinMensaje() {
        // Arrange
        ServerWebInputException exception = new ServerWebInputException("");

        // Act
        var response = handler.manejarError(exception);

        // Assert
        StepVerifier.create(response)
                .assertNext(serverResponse -> {
                    assertEquals(400, serverResponse.statusCode().value());
                    assertEquals(MediaType.APPLICATION_JSON, serverResponse.headers().getContentType());


                })
                .verifyComplete();
    }
}