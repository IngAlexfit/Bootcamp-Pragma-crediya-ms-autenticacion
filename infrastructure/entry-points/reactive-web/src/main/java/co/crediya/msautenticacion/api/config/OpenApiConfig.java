package co.crediya.msautenticacion.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI(
            @Value("${spring.application.name:ms-autenticacion}") String appName,
            @Value("${server.port:8080}") int port
    ) {
        return new OpenAPI()
                .info(new Info()
                        .title(appName + " Microservicio Autenticación")
                        .description("Microservicio De Autenticación - CrediYa")
                        .version("v1")
                        .license(new License().name("Apache 2.0").url("https://www.apache.org/licenses/LICENSE-2.0.html"))
                        .contact(new Contact().name("Equipo CrediYa").email("info@pragma.com.co"))
                )
                .servers(List.of(
                        new Server().url("http://localhost:" + port).description("Local")
                ));
    }
}
