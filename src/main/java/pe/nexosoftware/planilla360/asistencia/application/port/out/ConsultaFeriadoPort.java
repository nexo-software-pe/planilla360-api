package pe.nexosoftware.planilla360.asistencia.application.port.out;

import java.time.LocalDate;

public interface ConsultaFeriadoPort {

    boolean esFeriado(LocalDate fecha);
}
