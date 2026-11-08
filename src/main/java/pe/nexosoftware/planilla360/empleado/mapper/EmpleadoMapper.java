package pe.nexosoftware.planilla360.empleado.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.nexosoftware.planilla360.empleado.EmpleadoResumen;
import pe.nexosoftware.planilla360.empleado.dto.ContratoResponse;
import pe.nexosoftware.planilla360.empleado.dto.EmpleadoResponse;
import pe.nexosoftware.planilla360.empleado.dto.FamiliarResponse;
import pe.nexosoftware.planilla360.empleado.entity.Contrato;
import pe.nexosoftware.planilla360.empleado.entity.Empleado;
import pe.nexosoftware.planilla360.empleado.entity.Familiar;

@Mapper(componentModel = "spring")
public interface EmpleadoMapper {

    EmpleadoResponse aResponse(Empleado empleado);

    ContratoResponse aResponse(Contrato contrato);

    FamiliarResponse aResponse(Familiar familiar);

    @Mapping(target = "nombreCompleto", expression = "java(empleado.nombreCompleto())")
    EmpleadoResumen aResumen(Empleado empleado);
}
