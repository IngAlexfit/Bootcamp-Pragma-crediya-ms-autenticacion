package co.crediya.msautenticacion.model.usuario.gateways;

import co.crediya.msautenticacion.model.usuario.Usuario;
import reactor.core.publisher.Mono;

/**
 * Interfaz para operaciones de persistencia de usuarios.
 */
public interface UsuarioRepository {
    Mono<Usuario> save(Usuario usuario);
    Mono<Usuario> findByEmail(String email);
}