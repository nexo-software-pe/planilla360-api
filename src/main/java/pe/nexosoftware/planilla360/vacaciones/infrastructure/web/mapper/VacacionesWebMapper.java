package pe.nexosoftware.planilla360.vacaciones.infrastructure.web.mapper;

import org.mapstruct.Mapper;
import pe.nexosoftware.planilla360.vacaciones.domain.model.Permiso;
import pe.nexosoftware.planilla360.vacaciones.domain.model.SolicitudVacaciones;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.web.dto.PermisoResponse;
import pe.nexosoftware.planilla360.vacaciones.infrastructure.web.dto.SolicitudVacacionesResponse;

@Mapper(componentModel = "spring")
public interface VacacionesWebMapper {

    default SolicitudVacacionesResponse aResponse(SolicitudVacaciones s) {
        return new SolicitudVacacionesResponse(
                s.id(), s.empleadoId(), s.periodoId(), s.desde(), s.hasta(), s.dias(), s.estado(), s.motivoRechazo());
    }

    default PermisoResponse aResponse(Permiso p) {
        return new PermisoResponse(
                p.id(), p.empleadoId(), p.tipo(), p.desde(), p.hasta(), p.dias(), p.conGoce(), p.motivo(), p.estado(),
                p.motivoRechazo());
    }
}
