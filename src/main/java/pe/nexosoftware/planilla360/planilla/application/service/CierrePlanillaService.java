package pe.nexosoftware.planilla360.planilla.application.service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado.Accion;
import pe.nexosoftware.planilla360.compartido.excepcion.ConflictoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;
import pe.nexosoftware.planilla360.planilla.application.model.ResultadoCierre;
import pe.nexosoftware.planilla360.planilla.application.model.ResumenPlanilla;
import pe.nexosoftware.planilla360.planilla.application.port.in.CerrarPlanillaUseCase;
import pe.nexosoftware.planilla360.planilla.application.port.out.PlanillaPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.PublicadorEventosPort;
import pe.nexosoftware.planilla360.planilla.domain.event.PlanillaCerrada;
import pe.nexosoftware.planilla360.planilla.domain.model.EstadoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;
import pe.nexosoftware.planilla360.planilla.domain.validation.ResultadoValidacion;

/**
 * Cierre idempotente. La fila de la planilla se bloquea mientras dura la transacción: dos peticiones simultáneas con
 * la misma clave se serializan y la segunda ve la planilla ya cerrada, así que no hay doble cierre ni doble evento.
 */
@Service
@Transactional(readOnly = true)
public class CierrePlanillaService implements CerrarPlanillaUseCase {

    private static final int LONGITUD_MAXIMA_CLAVE = 100;

    private final PlanillaPort planillas;
    private final ValidadorDePlanilla validador;
    private final PublicadorEventosPort eventos;
    private final Clock reloj;

    public CierrePlanillaService(
            PlanillaPort planillas, ValidadorDePlanilla validador, PublicadorEventosPort eventos, Clock reloj) {
        this.planillas = planillas;
        this.validador = validador;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Override
    @Transactional
    public ResultadoCierre cerrar(UUID planillaId, String claveIdempotencia) {
        String clave = claveValida(claveIdempotencia);
        Planilla planilla = planillas.buscarParaActualizar(planillaId).orElseThrow(PlanillaService::noExiste);
        Optional<UUID> usadaEn = planillas.idConClave(clave);
        if (usadaEn.isPresent() && !usadaEn.get().equals(planilla.id())) {
            throw new ConflictoException("La clave de idempotencia ya se usó para cerrar otra planilla.");
        }
        ResumenPlanilla antes = ResumenPlanilla.de(planilla);
        ResultadoValidacion validacion = planilla.estado() == EstadoPlanilla.CERRADA
                ? new ResultadoValidacion(List.of())
                : validador.validar(planilla);
        Optional<PlanillaCerrada> evento = planilla.cerrar(clave, validacion, reloj.instant());
        if (evento.isEmpty()) {
            return new ResultadoCierre(planilla, true);
        }
        Planilla cerrada = planillas.guardar(planilla);
        eventos.registrarCambio(cerrada.id(), Accion.ACTUALIZAR, antes, ResumenPlanilla.de(cerrada));
        eventos.publicar(evento.get());
        return new ResultadoCierre(cerrada, false);
    }

    private static String claveValida(String clave) {
        if (clave == null || clave.isBlank()) {
            throw new ReglaDeNegocioException("Indique la clave de idempotencia del cierre (cabecera Idempotency-Key).");
        }
        String limpia = clave.trim();
        if (limpia.length() > LONGITUD_MAXIMA_CLAVE) {
            throw new ReglaDeNegocioException(
                    "La clave de idempotencia no puede superar los " + LONGITUD_MAXIMA_CLAVE + " caracteres.");
        }
        return limpia;
    }
}
