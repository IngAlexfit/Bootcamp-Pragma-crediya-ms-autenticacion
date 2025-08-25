package co.crediya.msautenticacion.api.config;

import co.crediya.msautenticacion.api.Handler;
import co.crediya.msautenticacion.api.RouterRest;
import co.crediya.msautenticacion.api.mapper.UsuarioDTOMapper;
import co.crediya.msautenticacion.usecase.usuario.registrarusuario.interfaces.IRegistrarUsuarioUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import jakarta.validation.Validator;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import static org.mockito.Mockito.mock;

@ContextConfiguration(classes = {RouterRest.class, Handler.class})
@WebFluxTest
@Import({CorsConfig.class, SecurityHeadersConfig.class, ConfigTest.MockBeans.class})
class ConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @TestConfiguration
    static class MockBeans {
        @Bean
        IRegistrarUsuarioUseCase registrarUsuarioUseCase() { return mock(IRegistrarUsuarioUseCase.class); }
        @Bean
        Validator validator() { return mock(Validator.class); }
        @Bean
        UsuarioDTOMapper usuarioDTOMapper() { return mock(UsuarioDTOMapper.class); }
    }

    @Test
    void securityHeadersShouldBePresent() {
        webTestClient.post()
                .uri("/api/v1/usuarios")
                .exchange()
                .expectStatus().is4xxClientError()
                .expectHeader().valueEquals("Content-Security-Policy",
                        "default-src 'self'; frame-ancestors 'self'; form-action 'self'")
                .expectHeader().valueEquals("Strict-Transport-Security", "max-age=31536000;")
                .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
                .expectHeader().valueEquals("Server", "")
                .expectHeader().valueEquals("Cache-Control", "no-store")
                .expectHeader().valueEquals("Pragma", "no-cache")
                .expectHeader().valueEquals("Referrer-Policy", "strict-origin-when-cross-origin");
    }

}