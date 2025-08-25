package co.crediya.msautenticacion.r2dbc;

import co.crediya.msautenticacion.r2dbc.entity.UsuarioEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UsuarioReactiveRepository extends ReactiveCrudRepository<UsuarioEntity, java.util.UUID>, ReactiveQueryByExampleExecutor<UsuarioEntity> {
    Mono<UsuarioEntity> findByEmail(String email);
}