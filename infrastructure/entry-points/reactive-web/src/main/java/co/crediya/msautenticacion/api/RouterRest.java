package co.crediya.msautenticacion.api;

import co.crediya.msautenticacion.api.dto.UsuarioRequest;
import co.crediya.msautenticacion.api.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterRest {
    @Bean
    @RouterOperations({
        @RouterOperation(
            path = "/api/v1/usuarios",
            produces = {MediaType.APPLICATION_JSON_VALUE},
            method = RequestMethod.POST,
            beanClass = Handler.class,
            beanMethod = "registrarUsuario",
            operation = @Operation(
                operationId = "registrarUsuario",
                summary = "Registrar un usuario",
                requestBody = @RequestBody(required = true, content = @Content(schema = @Schema(implementation = UsuarioRequest.class))),
                responses = {
                    @ApiResponse(responseCode = "200", description = "Usuario registrado",
                        content = @Content(schema = @Schema(implementation = UsuarioResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
                }
            )
        )
    })
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
    return route(POST("/api/v1/usuarios"), handler::registrarUsuario);
    }
}
