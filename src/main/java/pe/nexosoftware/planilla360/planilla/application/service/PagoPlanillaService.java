package pe.nexosoftware.planilla360.planilla.application.service;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado.Accion;
import pe.nexosoftware.planilla360.compartido.excepcion.ConflictoException;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;
import pe.nexosoftware.planilla360.planilla.application.model.PagoPlanilla;
import pe.nexosoftware.planilla360.planilla.application.port.in.RegistrarPagoUseCase;
import pe.nexosoftware.planilla360.planilla.application.port.out.PagoPlanillaPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.PlanillaPort;
import pe.nexosoftware.planilla360.planilla.application.port.out.PublicadorEventosPort;
import pe.nexosoftware.planilla360.planilla.domain.model.EstadoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;

@Service
@Transactional(readOnly = true)
public class PagoPlanillaService implements RegistrarPagoUseCase {

    private final PlanillaPort planillas;
    private final PagoPlanillaPort pagos;
    private final PublicadorEventosPort eventos;

    public PagoPlanillaService(PlanillaPort planillas, PagoPlanillaPort pagos, PublicadorEventosPort eventos) {
        this.planillas = planillas;
        this.pagos = pagos;
        this.eventos = eventos;
    }

    @Override
    @Transactional
    public PagoPlanilla registrar(UUID planillaId, LocalDate fechaPago, String referencia) {
        if (referencia == null || referencia.isBlank()) {
            throw new ReglaDeNegocioException("Indique la referencia del pago (por ejemplo el número de operación).");
        }
        Planilla planilla = planillas.buscarParaActualizar(planillaId).orElseThrow(PlanillaService::noExiste);
        if (planilla.estado() != EstadoPlanilla.CERRADA) {
            throw new ReglaDeNegocioException("Solo se registra el pago de una planilla cerrada.");
        }
        if (pagos.buscar(planilla.id()).isPresent()) {
            throw new ConflictoException("La planilla ya tiene un pago registrado.");
        }
        PagoPlanilla guardado = pagos.guardar(new PagoPlanilla(planilla.id(), fechaPago, referencia.trim()));
        eventos.registrarCambio(planilla.id(), Accion.ACTUALIZAR, null, guardado);
        return guardado;
    }
}
