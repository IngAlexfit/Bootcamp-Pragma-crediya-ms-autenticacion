package co.crediya.msautenticacion.config;

import co.crediya.msautenticacion.model.usuario.gateways.UsuarioRepository;
import co.crediya.msautenticacion.usecase.usuario.registrarusuario.RegistrarUsuarioUseCase;
import co.crediya.msautenticacion.usecase.usuario.registrarusuario.interfaces.IRegistrarUsuarioUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class UseCasesConfigTest {



    @Configuration
    public static class TestConfig {
        @Bean
        @Primary
        public UsuarioRepository usuarioRepository() {
            return Mockito.mock(UsuarioRepository.class);
        }

        @Bean
        @Primary
        public IRegistrarUsuarioUseCase registrarUsuarioUseCase(UsuarioRepository usuarioRepository) {
            return new RegistrarUsuarioUseCase(usuarioRepository);
        }

        @Bean
        public MyUseCase myUseCase() {
            return new MyUseCase();
        }
    }

    @Test
    void testUseCaseBeansExist() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            String[] beanNames = context.getBeanDefinitionNames();

            boolean useCaseBeanFound = false;
            for (String beanName : beanNames) {
                if (beanName.endsWith("UseCase")) {
                    useCaseBeanFound = true;
                    break;
                }
            }

            assertTrue(useCaseBeanFound, "No beans ending with 'UseCase' were found");
        }
    }

    static class MyUseCase {
        public String execute() {
            return "MyUseCase Test";
        }
    }
}