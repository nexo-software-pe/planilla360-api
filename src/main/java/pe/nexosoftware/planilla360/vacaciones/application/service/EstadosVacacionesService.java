package pe.nexosoftware.planilla360.vacaciones.application.service;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.vacaciones.application.port.in.ActualizarEstadosVacacionesUseCase;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.PeriodoVacacionalPort;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.PeriodoVacacionalPort.Guardado;
import pe.nexosoftware.planilla360.vacaciones.application.port.out.SolicitudVacacionesPort;
import pe.nexosoftware.planilla360.vacaciones.domain.model.SolicitudVacaciones;

/** Avanza las vacaciones aprobadas según el calendario: inicio del goce y finalización. */
@Service
public class EstadosVacacionesService implements ActualizarEstadosVacacionesUseCase {

    private final SolicitudVacacionesPort solicitudes;
    private final PeriodoVacacionalPort periodos;
    private final Clock reloj;

    public EstadosVacacionesService(SolicitudVacacionesPort solicitudes, PeriodoVacacionalPort periodos, Clock reloj) {
        this.solicitudes = solicitudes;
        this.periodos = periodos;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public Resultado actualizar() {
        LocalDate hoy = HoraLima.hoy(reloj);
        int iniciadas = 0;
        for (SolicitudVacaciones solicitud : solicitudes.porIniciarGoce(hoy)) {
            solicitud.iniciarGoce(hoy);
            solicitudes.guardar(solicitud);
            iniciadas++;
        }
        int finalizadas = 0;
        for (SolicitudVacaciones solicitud : solicitudes.porFinalizar(hoy)) {
            Guardado guardado = periodos.buscar(solicitud.periodoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("No existe el periodo vacacional."));
            solicitud.finalizar(hoy);
            guardado.periodo().registrarGoce(solicitud.dias());
            periodos.guardar(guardado.empleadoId(), guardado.periodo());
            solicitudes.guardar(solicitud);
            finalizadas++;
        }
        return new Resultado(iniciadas, finalizadas);
    }
}
