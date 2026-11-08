package pe.nexosoftware.planilla360.asistencia.domain.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import pe.nexosoftware.planilla360.asistencia.domain.model.AsistenciaDiaria;
import pe.nexosoftware.planilla360.asistencia.domain.model.ContextoDia;
import pe.nexosoftware.planilla360.asistencia.domain.model.EstadoAsistencia;
import pe.nexosoftware.planilla360.asistencia.domain.model.Horario;
import pe.nexosoftware.planilla360.asistencia.domain.model.Marcacion;
import pe.nexosoftware.planilla360.asistencia.domain.model.TipoMarcacion;

/**
 * Convierte las marcaciones de un día en tardanza, horas extra y estado. Servicio de dominio sin estado:
 * solo guarda la zona horaria con la que se interpretan las marcaciones.
 *
 * <p>Reglas: el día se evalúa en hora de Lima; los minutos se truncan (08:10:59 es 08:10); la tardanza
 * se cuenta desde la hora de entrada, pero solo si la llegada supera la tolerancia; los minutos extra
 * son los posteriores a la hora de salida.
 */
public final class ConsolidadorAsistencia {

    public static final ZoneId ZONA_LIMA = ZoneId.of("America/Lima");

    private final ZoneId zona;

    public ConsolidadorAsistencia() {
        this(ZONA_LIMA);
    }

    public ConsolidadorAsistencia(ZoneId zona) {
        this.zona = zona;
    }

    public AsistenciaDiaria consolidar(ContextoDia contexto) {
        LocalDate fecha = contexto.fecha();
        Horario horario = contexto.horario();
        if (!horario.trabajaEl(fecha.getDayOfWeek())) {
            return resultado(fecha, 0, 0, EstadoAsistencia.DESCANSO);
        }
        if (contexto.feriado()) {
            return resultado(fecha, 0, 0, EstadoAsistencia.FERIADO);
        }
        List<Marcacion> delDia = contexto.marcaciones().stream()
                .filter(m -> fecha.equals(m.instante().atZone(zona).toLocalDate()))
                .sorted(Comparator.comparing(Marcacion::instante))
                .toList();
        if (delDia.isEmpty()) {
            return resultado(
                    fecha, 0, 0, contexto.justificado() ? EstadoAsistencia.JUSTIFICADA : EstadoAsistencia.FALTA);
        }
        Optional<Marcacion> entrada = delDia.stream().filter(m -> m.tipo() == TipoMarcacion.ENTRADA).findFirst();
        if (entrada.isEmpty()) {
            return resultado(fecha, 0, 0, EstadoAsistencia.INCOMPLETA);
        }
        int tardanza = minutosDeTardanza(horario, horaLocal(entrada.get()));
        Optional<Marcacion> salida = delDia.stream()
                .filter(m -> m.tipo() == TipoMarcacion.SALIDA)
                .filter(m -> m.instante().isAfter(entrada.get().instante()))
                .reduce((primera, ultima) -> ultima);
        if (salida.isEmpty()) {
            return resultado(fecha, tardanza, 0, EstadoAsistencia.INCOMPLETA);
        }
        int extra = minutosExtra(horario, horaLocal(salida.get()));
        return resultado(fecha, tardanza, extra, tardanza > 0 ? EstadoAsistencia.TARDANZA : EstadoAsistencia.PRESENTE);
    }

    private LocalTime horaLocal(Marcacion marcacion) {
        return marcacion.instante().atZone(zona).toLocalTime().truncatedTo(ChronoUnit.MINUTES);
    }

    private static int minutosDeTardanza(Horario horario, LocalTime llegada) {
        if (!llegada.isAfter(horario.limiteSinTardanza())) {
            return 0;
        }
        return (int) Duration.between(horario.entrada(), llegada).toMinutes();
    }

    private static int minutosExtra(Horario horario, LocalTime salida) {
        if (!salida.isAfter(horario.salida())) {
            return 0;
        }
        return (int) Duration.between(horario.salida(), salida).toMinutes();
    }

    private static AsistenciaDiaria resultado(LocalDate fecha, int tardanza, int extra, EstadoAsistencia estado) {
        return new AsistenciaDiaria(fecha, tardanza, extra, estado);
    }
}
