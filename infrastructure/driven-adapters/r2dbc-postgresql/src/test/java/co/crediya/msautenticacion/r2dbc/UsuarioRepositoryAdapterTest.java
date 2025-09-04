package co.crediya.msautenticacion.r2dbc;

import co.crediya.msautenticacion.model.usuario.Usuario;
import co.crediya.msautenticacion.r2dbc.entity.UsuarioEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.reactivecommons.utils.ObjectMapper;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;

/**
 * Pruebas unitarias para {@link UsuarioRepositoryAdapter}.
 *
 * Estas pruebas verifican la correcta implementación del adaptador de repositorio,
 * asegurando que:
 * - La conversión entre entidades y modelos de dominio funciona adecuadamente
 * - Las operaciones de guardado se propagan correctamente al repositorio subyacente
 * - Las consultas por correo electrónico retornan los resultados esperados
 * - El flujo reactivo se mantiene a lo largo de las operaciones
 */
class UsuarioRepositoryAdapterTest {

    @Mock
    private UsuarioReactiveRepository repository;

    @Mock
    private ObjectMapper mapper;

    @InjectMocks
    private UsuarioRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Verifica que el método save() convierte correctamente el modelo de dominio a entidad,
     * guarda la entidad y convierte el resultado de nuevo al modelo de dominio.
     */
    @Test
    void testSaveUsuario() {
        // Preparar datos
        UUID userId = UUID.randomUUID();
        Usuario usuario = Usuario.builder()
                .nombre("Juan")
                .apellido("Pérez")
                .email("juan@example.com")
                .telefono("123456789")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .salarioBase(new BigDecimal("1000.00"))
                .build();

        UsuarioEntity usuarioEntity = UsuarioEntity.builder()
                .userId(userId)
                .nombre("Juan")
                .apellido("Pérez")
                .email("juan@example.com")
                .telefono("123456789")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .salarioBase(new BigDecimal("1000.00"))
                .build();

        // Configurar mocks
        Mockito.when(mapper.map(usuario, UsuarioEntity.class)).thenReturn(usuarioEntity);
        Mockito.when(repository.save(any(UsuarioEntity.class))).thenReturn(Mono.just(usuarioEntity));
        Mockito.when(mapper.map(usuarioEntity, Usuario.class)).thenReturn(usuario);

        // Ejecutar y verificar
        StepVerifier.create(adapter.save(usuario))
                .expectNextMatches(result ->
                        result.getNombre().equals("Juan") &&
                                result.getApellido().equals("Pérez") &&
                                result.getEmail().equals("juan@example.com")
                )
                .verifyComplete();
    }

    /**
     * Verifica que el método findByEmail() realiza la consulta correctamente,
     * transforma la entidad resultante a modelo de dominio y mantiene
     * la integridad de los datos durante el proceso.
     */
    @Test
    void testFindByEmail() {
        // Preparar datos
        String email = "juan@example.com";
        UUID userId = UUID.randomUUID();

        UsuarioEntity usuarioEntity = UsuarioEntity.builder()
                .userId(userId)
                .nombre("Juan")
                .apellido("Pérez")
                .email(email)
                .telefono("123456789")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .salarioBase(new BigDecimal("1000.00"))
                .build();

        Usuario usuario = Usuario.builder()
                .userId(userId)
                .nombre("Juan")
                .apellido("Pérez")
                .email(email)
                .telefono("123456789")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .salarioBase(new BigDecimal("1000.00"))
                .build();

        // Configurar mocks
        Mockito.when(repository.findByEmail(email)).thenReturn(Mono.just(usuarioEntity));
        Mockito.when(mapper.map(usuarioEntity, Usuario.class)).thenReturn(usuario);

        // Ejecutar y verificar
        StepVerifier.create(adapter.findByEmail(email))
                .expectNextMatches(result ->
                        result.getUserId().equals(userId) &&
                                result.getNombre().equals("Juan") &&
                                result.getApellido().equals("Pérez") &&
                                result.getEmail().equals(email)
                )
                .verifyComplete();
    }
}