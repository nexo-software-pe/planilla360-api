package pe.nexosoftware.planilla360.asistencia.application.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.asistencia.application.port.in.JustificarDiasUseCase;
import pe.nexosoftware.planilla360.asistencia.application.port.out.DiaJustificadoPort;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;

/** Registra los días que no deben contarse como falta (vacaciones y permisos aprobados). */
@Service
public class JustificacionService implements JustificarDiasUseCase {

    static final int MAXIMO_DIAS = 366;

    private final DiaJustificadoPort diasJustificados;

    public JustificacionService(DiaJustificadoPort diasJustificados) {
        this.diasJustificados = diasJustificados;
    }

    @Override
    @Transactional
    public void justificar(UUID empleadoId, LocalDate desde, LocalDate hasta, Motivo motivo, UUID origenId) {
        if (hasta.isBefore(desde)) {
            throw new ReglaDeNegocioException("El rango de fechas es inválido.");
        }
        long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
        if (dias > MAXIMO_DIAS) {
            throw new ReglaDeNegocioException("No se pueden justificar más de " + MAXIMO_DIAS + " días de una vez.");
        }
        List<LocalDate> fechas = desde.datesUntil(hasta.plusDays(1)).toList();
        diasJustificados.justificar(empleadoId, fechas, motivo, origenId);
    }
}
