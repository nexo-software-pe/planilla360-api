package pe.nexosoftware.planilla360.planilla.application.port.out;

import java.util.Optional;
import java.util.UUID;
import pe.nexosoftware.planilla360.planilla.application.model.PagoPlanilla;

public interface PagoPlanillaPort {

    PagoPlanilla guardar(PagoPlanilla pago);

    Optional<PagoPlanilla> buscar(UUID planillaId);
}
