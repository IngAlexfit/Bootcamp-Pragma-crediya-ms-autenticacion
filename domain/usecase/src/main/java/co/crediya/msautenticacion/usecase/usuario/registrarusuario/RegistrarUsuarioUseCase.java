package co.crediya.msautenticacion.usecase.usuario.registrarusuario;

import co.crediya.msautenticacion.model.usuario.gateways.UsuarioRepository;
import co.crediya.msautenticacion.usecase.usuario.registrarusuario.interfaces.IRegistrarUsuarioUseCase;
import co.crediya.msautenticacion.model.usuario.Usuario;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;



/**
 * Caso de uso para registrar un nuevo usuario en el sistema.
 */

@RequiredArgsConstructor
public class RegistrarUsuarioUseCase implements IRegistrarUsuarioUseCase{
    private final UsuarioRepository usuarioRepository;

   /**
     * Valida y registra un nuevo usuario.
     * @param usuario Usuario a registrar
     * @return Mono<Usuario> usuario registrado
     */
    public Mono<Usuario> registrar(Usuario usuario) {
             return validarEmailNoExistente(usuario)
                .flatMap(this::guardarNuevoUsuario);
    }

    /**
     * Valida que no exista otro usuario con el mismo correo electrónico.
     * @param usuario Usuario a validar
     * @return Mono<Usuario> usuario si el correo no existe, error si ya existe
     */
    private Mono<Usuario> validarEmailNoExistente(Usuario usuario) {
        return usuarioRepository.findByEmail(usuario.getEmail())
                .flatMap(existingUser ->
                        Mono.error(new IllegalArgumentException("El correo electrónico ya se encuentra registrado."))
                )
                .switchIfEmpty(Mono.just(usuario))
                .cast(Usuario.class);
    }

     /**
     * Guarda el nuevo usuario en el repositorio.
     * @param usuario Usuario a guardar
     * @return Mono<Usuario> usuario guardado
     */
    private Mono<Usuario> guardarNuevoUsuario(Usuario usuario) {
        return usuarioRepository.save(usuario);
    }

}

