package pe.nexosoftware.planilla360.planilla.application.port.in;

import java.time.LocalDate;
import java.util.UUID;
import pe.nexosoftware.planilla360.planilla.application.model.PagoPlanilla;

public interface RegistrarPagoUseCase {

    /** Registra el pago de una planilla cerrada; desde entonces ya no se puede anular. */
    PagoPlanilla registrar(UUID planillaId, LocalDate fechaPago, String referencia);
}
