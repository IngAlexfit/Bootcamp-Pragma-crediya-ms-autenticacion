package co.crediya.msautenticacion.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import co.crediya.msautenticacion.api.dto.UsuarioRequest;
import co.crediya.msautenticacion.api.dto.UsuarioResponse;
import co.crediya.msautenticacion.model.usuario.Usuario;

@Mapper(componentModel = "spring")
public interface UsuarioDTOMapper {
    
    @Mapping(target = "userId", ignore = true)
    Usuario toModel(UsuarioRequest dto);

    UsuarioResponse toResponse(Usuario usuario);

}