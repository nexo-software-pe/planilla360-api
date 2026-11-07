package pe.nexosoftware.planilla360.identidad.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.nexosoftware.planilla360.identidad.dto.UsuarioResponse;
import pe.nexosoftware.planilla360.identidad.entity.Usuario;

@Mapper(componentModel = "spring")
public interface UsuarioMapper {

    @Mapping(source = "createdAt", target = "creadoEn")
    UsuarioResponse aResponse(Usuario usuario);
}
