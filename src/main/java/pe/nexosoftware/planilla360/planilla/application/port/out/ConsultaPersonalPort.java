package pe.nexosoftware.planilla360.planilla.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.model.Hijo;
import pe.nexosoftware.planilla360.planilla.domain.regimen.TipoComision;
import pe.nexosoftware.planilla360.planilla.domain.regimen.TipoRegimenPensionario;

/** Datos del personal que la planilla necesita (los provee el módulo de empleados). */
public interface ConsultaPersonalPort {

    record Trabajador(UUID id, String nombreCompleto, LocalDate ingreso, LocalDate cese) {
    }

    record ContratoVigente(
            UUID empleadoId,
            Dinero sueldoBasico,
            int jornadaSemanal,
            TipoRegimenPensionario regimenPensionario,
            UUID afpId,
            TipoComision tipoComision) {
    }

    /** Trabajadores que estuvieron vigentes algún día del rango, ordenados por apellidos y nombres. */
    List<Trabajador> vigentesEntre(LocalDate desde, LocalDate hasta);

    Optional<ContratoVigente> contratoVigenteEn(UUID empleadoId, LocalDate fecha);

    List<Hijo> hijosDe(UUID empleadoId);
}
