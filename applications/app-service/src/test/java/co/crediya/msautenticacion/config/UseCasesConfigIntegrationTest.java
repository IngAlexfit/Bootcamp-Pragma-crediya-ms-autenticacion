package co.crediya.msautenticacion.config;

import co.crediya.msautenticacion.model.usuario.gateways.UsuarioRepository;
import co.crediya.msautenticacion.usecase.usuario.registrarusuario.interfaces.IRegistrarUsuarioUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Pruebas de integración para la configuración de casos de uso.
 *
 * Esta clase verifica que los beans esenciales estén correctamente
 * configurados y disponibles en el contexto de Spring cuando la
 * aplicación se inicia completamente.
 *
 * @see UseCasesConfig
 */
@SpringBootTest
class UseCasesConfigIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * Verifica que los beans principales de repositorio y casos de uso
     * estén correctamente registrados en el contexto de la aplicación.
     *
     * <p>Esta prueba asegura que:
     * <ul>
     *   <li>El repositorio de usuario está disponible</li>
     *   <li>El caso de uso de registro de usuario está disponible</li>
     * </ul>
     */
    @Test
    void testConfigurationBeansExist() {
        assertNotNull(applicationContext.getBean(UsuarioRepository.class));
        assertNotNull(applicationContext.getBean(IRegistrarUsuarioUseCase.class));
    }
}