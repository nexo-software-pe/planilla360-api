package pe.nexosoftware.planilla360.planilla.application.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.planilla.application.port.out.ConsultaCatalogoPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.ConsultaPersonalPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.ConsultaPersonalPort.ContratoVigente;
import pe.nexosoftware.planilla360.planilla.application.port.out.ConsultaPersonalPort.Trabajador;
import pe.nexosoftware.planilla360.planilla.domain.excepcion.ReglaDePlanillaException;
import pe.nexosoftware.planilla360.planilla.domain.regimen.AfpVigente;
import pe.nexosoftware.planilla360.planilla.domain.regimen.FabricaRegimenPensionario;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenPensionario;

/**
 * Quiénes entran a una planilla y con qué régimen pensionario. Los trabajadores vigentes sin contrato o con un régimen
 * que no se puede armar (por ejemplo una AFP inexistente) no tienen línea: la validación de cierre los reporta.
 */
@Component
class PersonalDelPeriodo {

    record TrabajadorConRegimen(Trabajador trabajador, ContratoVigente contrato, RegimenPensionario regimen) {
    }

    record Resultado(List<TrabajadorConRegimen> conRegimen, Set<UUID> sinRegimen) {
    }

    private final ConsultaPersonalPort personal;
    private final ConsultaCatalogoPort catalogo;

    PersonalDelPeriodo(ConsultaPersonalPort personal, ConsultaCatalogoPort catalogo) {
        this.personal = personal;
        this.catalogo = catalogo;
    }

    Resultado resolver(LocalDate desde, LocalDate hasta) {
        List<TrabajadorConRegimen> conRegimen = new ArrayList<>();
        Set<UUID> sinRegimen = new HashSet<>();
        for (Trabajador trabajador : personal.vigentesEntre(desde, hasta)) {
            LocalDate fechaDelContrato = trabajador.cese() != null && trabajador.cese().isBefore(hasta)
                    ? trabajador.cese()
                    : hasta;
            Optional<TrabajadorConRegimen> resuelto = personal.contratoVigenteEn(trabajador.id(), fechaDelContrato)
                    .flatMap(contrato -> regimenDe(contrato).map(r -> new TrabajadorConRegimen(trabajador, contrato, r)));
            if (resuelto.isPresent()) {
                conRegimen.add(resuelto.get());
            } else {
                sinRegimen.add(trabajador.id());
            }
        }
        return new Resultado(conRegimen, sinRegimen);
    }

    private Optional<RegimenPensionario> regimenDe(ContratoVigente contrato) {
        AfpVigente afp = contrato.afpId() == null ? null : catalogo.afp(contrato.afpId()).orElse(null);
        try {
            return Optional.of(FabricaRegimenPensionario.crear(contrato.regimenPensionario(), afp,
                    contrato.tipoComision()));
        } catch (ReglaDePlanillaException e) {
            return Optional.empty();
        }
    }
}
