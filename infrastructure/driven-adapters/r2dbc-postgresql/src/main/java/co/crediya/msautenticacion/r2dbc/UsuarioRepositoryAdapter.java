package co.crediya.msautenticacion.r2dbc;

import co.crediya.msautenticacion.model.usuario.Usuario;
import co.crediya.msautenticacion.model.usuario.gateways.UsuarioRepository;
import co.crediya.msautenticacion.r2dbc.entity.UsuarioEntity;
import lombok.extern.slf4j.Slf4j;

import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import reactor.core.publisher.Mono;

/**
 * Adaptador para operaciones de persistencia de usuarios usando R2DBC.
 */
@Slf4j
@Component
public class UsuarioRepositoryAdapter implements UsuarioRepository {

    private final UsuarioReactiveRepository repository;
    private final ObjectMapper mapper;

    public UsuarioRepositoryAdapter(UsuarioReactiveRepository repository, ObjectMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /**
     * Guarda un usuario en la base de datos de forma transaccional.
     * @param usuario Usuario a guardar
     * @return Mono<Usuario> usuario guardado
     */
    @Override
    @Transactional
    public Mono<Usuario> save(Usuario usuario) {
        UsuarioEntity entity = mapper.map(usuario, UsuarioEntity.class);
        return repository.save(entity)
            .map(saved -> mapper.map(saved, Usuario.class))
            .doOnError(e -> log.error("Error al guardar usuario en BD: {}", e.getMessage(), e));
    }

    /**
     * Busca un usuario por correo electrónico.
     * @param email Correo electrónico a buscar
     * @return Mono<Usuario> usuario encontrado o vacío
     */
    @Transactional
    @Override
    public Mono<Usuario> findByEmail(String email) {
        return repository.findByEmail(email)
            .map(entity -> mapper.map(entity, Usuario.class))
            .doOnError(e -> log.error("Error al buscar usuario por email: {}", e.getMessage(), e));
    }
}