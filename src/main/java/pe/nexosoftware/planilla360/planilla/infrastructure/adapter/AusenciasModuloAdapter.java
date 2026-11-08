package pe.nexosoftware.planilla360.planilla.infrastructure.adapter;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.asistencia.AsistenciaApi;
import pe.nexosoftware.planilla360.asistencia.AsistenciaApi.NovedadesDelMes;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.application.model.NovedadesAsistencia;
import pe.nexosoftware.planilla360.planilla.application.port.out.ConsultaAsistenciaPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.ConsultaVacacionesPort;
import pe.nexosoftware.planilla360.vacaciones.VacacionesApi;

/** Capa anticorrupción hacia {@code asistencia} y {@code vacaciones}. */
@Component
class AusenciasModuloAdapter implements ConsultaAsistenciaPort, ConsultaVacacionesPort {

    private final AsistenciaApi asistencia;
    private final VacacionesApi vacaciones;

    AusenciasModuloAdapter(AsistenciaApi asistencia, VacacionesApi vacaciones) {
        this.asistencia = asistencia;
        this.vacaciones = vacaciones;
    }

    @Override
    public Map<UUID, NovedadesAsistencia> novedadesDelMes(Periodo periodo) {
        Map<UUID, NovedadesAsistencia> novedades = new HashMap<>();
        for (NovedadesDelMes n : asistencia.novedadesDelMes(periodo.valor()).values()) {
            novedades.put(n.empleadoId(), new NovedadesAsistencia(n.minutosTardanza(), n.faltasInjustificadas()));
        }
        return novedades;
    }

    @Override
    public Map<UUID, List<BigDecimal>> horasExtraAprobadasDelMes(Periodo periodo) {
        return asistencia.horasExtraAprobadasDelMes(periodo.valor());
    }

    @Override
    public boolean mesCompleto(Periodo periodo) {
        return asistencia.mesCompleto(periodo.valor());
    }

    @Override
    public int solicitudesPendientesQueSolapan(Periodo periodo) {
        return vacaciones.solicitudesPendientesQueSolapan(periodo.inicio(), periodo.fin());
    }

    @Override
    public Map<UUID, Integer> diasDePermisoSinGoce(Periodo periodo) {
        return vacaciones.diasDePermisoSinGoce(periodo.inicio(), periodo.fin());
    }
}
