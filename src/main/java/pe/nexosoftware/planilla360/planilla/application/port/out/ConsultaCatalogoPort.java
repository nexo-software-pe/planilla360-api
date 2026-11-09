package pe.nexosoftware.planilla360.planilla.application.port.out;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import pe.nexosoftware.planilla360.planilla.application.model.SobretasasHorasExtra;
import pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegales;
import pe.nexosoftware.planilla360.planilla.domain.regimen.AfpVigente;

/** Parámetros legales y maestros vigentes (los provee el módulo de catálogos). */
public interface ConsultaCatalogoPort {

    ParametrosLegales parametrosEn(LocalDate fecha);

    SobretasasHorasExtra sobretasasHorasExtraEn(LocalDate fecha);

    Optional<AfpVigente> afp(UUID afpId);

    boolean esFeriado(LocalDate fecha);
}
