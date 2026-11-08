package pe.nexosoftware.planilla360.asistencia.infrastructure.web.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import pe.nexosoftware.planilla360.asistencia.application.port.in.RegistrarMarcacionUseCase.MarcacionRegistrada;
import pe.nexosoftware.planilla360.asistencia.application.port.out.HorarioPort.HorarioGuardado;
import pe.nexosoftware.planilla360.asistencia.domain.model.AsistenciaDiaria;
import pe.nexosoftware.planilla360.asistencia.domain.model.HoraExtra;
import pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto.AsistenciaDiariaResponse;
import pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto.HoraExtraResponse;
import pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto.HorarioResponse;
import pe.nexosoftware.planilla360.asistencia.infrastructure.web.dto.MarcacionResponse;

@Mapper(componentModel = "spring")
public interface AsistenciaWebMapper {

    MarcacionResponse aResponse(MarcacionRegistrada marcacion);

    AsistenciaDiariaResponse aResponse(AsistenciaDiaria dia);

    List<AsistenciaDiariaResponse> aResponses(List<AsistenciaDiaria> dias);

    default HoraExtraResponse aResponse(HoraExtra h) {
        return new HoraExtraResponse(
                h.id(), h.empleadoId(), h.fecha(), h.horas(), h.motivo(), h.estado(), h.aprobadoPor());
    }

    default HorarioResponse aResponse(HorarioGuardado g) {
        return new HorarioResponse(
                g.id(),
                g.nombre(),
                g.horario().entrada(),
                g.horario().salida(),
                g.horario().toleranciaMin(),
                g.horario().dias());
    }
}
