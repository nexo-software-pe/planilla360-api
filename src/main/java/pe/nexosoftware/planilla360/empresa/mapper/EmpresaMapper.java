package pe.nexosoftware.planilla360.empresa.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.nexosoftware.planilla360.empresa.EmpresaResumen;
import pe.nexosoftware.planilla360.empresa.dto.EmpresaResponse;
import pe.nexosoftware.planilla360.empresa.entity.Empresa;

@Mapper(componentModel = "spring")
public interface EmpresaMapper {

    @Mapping(source = "createdAt", target = "creadaEn")
    EmpresaResponse aResponse(Empresa empresa);

    EmpresaResumen aResumen(Empresa empresa);
}
