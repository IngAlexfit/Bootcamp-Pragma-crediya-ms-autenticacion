package co.crediya.msautenticacion.usecase.usuario.registrarusuario.interfaces;

import reactor.core.publisher.Mono;
import co.crediya.msautenticacion.model.usuario.Usuario;

public interface IRegistrarUsuarioUseCase {
    Mono<Usuario> registrar(Usuario usuario);
}
