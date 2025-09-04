package co.crediya.msautenticacion.api.mapper;

import co.crediya.msautenticacion.api.dto.UsuarioRequest;
import co.crediya.msautenticacion.api.dto.UsuarioResponse;
import co.crediya.msautenticacion.model.usuario.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para {@link UsuarioDTOMapper}.
 *
 * Estas pruebas verifican el correcto mapeo entre objetos DTO y modelos de dominio,
 * asegurando que:
 * - Los campos se mapean correctamente en ambas direcciones
 * - Se respetan las reglas específicas (como ignorar userId)
 * - Se manejan adecuadamente los casos de valores nulos
 */
class UsuarioDTOMapperTest {


    /**
     * Instancia del mapper a probar, creada con MapStruct.
     */
    private final UsuarioDTOMapper mapper = Mappers.getMapper(UsuarioDTOMapper.class);

    /**
     * Verifica que un {@link UsuarioRequest} se mapea correctamente a un modelo {@link Usuario}.
     *
     * @see UsuarioDTOMapper#toModel(UsuarioRequest)
     */
    @Test
    @DisplayName("Debe mapear correctamente de UsuarioRequest a modelo Usuario")
    void testToModel() {
        // Arrange
        UsuarioRequest request = new UsuarioRequest();
        request.setNombre("Juan");
        request.setApellido("Pérez");
        request.setEmail("juan@example.com");
        request.setTelefono("123456789");
        request.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        request.setSalarioBase(new BigDecimal("1000.00"));

        // Act
        Usuario usuario = mapper.toModel(request);

        // Assert
        assertNotNull(usuario);
        assertNull(usuario.getUserId()); // El userId debe ser ignorado
        assertEquals("Juan", usuario.getNombre());
        assertEquals("Pérez", usuario.getApellido());
        assertEquals("juan@example.com", usuario.getEmail());
        assertEquals("123456789", usuario.getTelefono());
        assertEquals(LocalDate.of(1990, 1, 1), usuario.getFechaNacimiento());
        assertEquals(new BigDecimal("1000.00"), usuario.getSalarioBase());
    }

    /**
     * Verifica que un modelo {@link Usuario} se mapea correctamente a un {@link UsuarioResponse}.
     *
     * @see UsuarioDTOMapper#toResponse(Usuario)
     */
    @Test
    @DisplayName("Debe mapear correctamente de modelo Usuario a UsuarioResponse")
    void testToResponse() {
        // Arrange
        UUID userId = UUID.randomUUID();
        Usuario usuario = Usuario.builder()
                .userId(userId)
                .nombre("Juan")
                .apellido("Pérez")
                .email("juan@example.com")
                .telefono("123456789")
                .fechaNacimiento(LocalDate.of(1990, 1, 1))
                .salarioBase(new BigDecimal("1000.00"))
                .build();

        // Act
        UsuarioResponse response = mapper.toResponse(usuario);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals("Juan", response.getNombre());
        assertEquals("Pérez", response.getApellido());
        assertEquals("juan@example.com", response.getEmail());
        assertEquals("123456789", response.getTelefono());
        assertEquals(LocalDate.of(1990, 1, 1), response.getFechaNacimiento());
        assertEquals(new BigDecimal("1000.00"), response.getSalarioBase());
    }

    /**
     * Verifica que el mapper maneja correctamente los valores nulos en ambas direcciones.
     *
     * @see UsuarioDTOMapper#toModel(UsuarioRequest)
     * @see UsuarioDTOMapper#toResponse(Usuario)
     */
    @Test
    @DisplayName("Debe manejar correctamente valores nulos")
    void testNullValues() {
        // Arrange
        UsuarioRequest requestNull = null;
        Usuario usuarioNull = null;

        // Act & Assert
        assertNull(mapper.toModel(requestNull));
        assertNull(mapper.toResponse(usuarioNull));
    }
}