package pe.nexosoftware.planilla360.integracion.cliente;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.compartido.excepcion.RecursoNoEncontradoException;
import pe.nexosoftware.planilla360.integracion.model.ContribuyenteRuc;
import pe.nexosoftware.planilla360.integracion.model.PersonaDni;
import pe.nexosoftware.planilla360.integracion.model.TipoCambio;

/** Respuestas ficticias en el mismo proceso: lo que usan staging y las pruebas sin servicio externo. */
@Component
@ConditionalOnProperty(name = "app.integracion.modo", havingValue = "stub", matchIfMissing = true)
class StubProveedorConsultas implements ProveedorConsultas {

    private final Clock reloj;

    StubProveedorConsultas(Clock reloj) {
        this.reloj = reloj;
    }

    @Override
    public PersonaDni consultarDni(String dni) {
        if (dni.equals("00000000")) {
            throw new RecursoNoEncontradoException("No se encontró el DNI " + dni + ".");
        }
        return new PersonaDni(dni, "JUAN CARLOS", "PEREZ", "GOMEZ");
    }

    @Override
    public ContribuyenteRuc consultarRuc(String ruc) {
        return new ContribuyenteRuc(ruc, "EMPRESA EJEMPLO S.A.C.", "ACTIVO", "HABIDO", "AV. EJEMPLO 123, LIMA");
    }

    @Override
    public TipoCambio consultarTipoCambio() {
        return new TipoCambio(LocalDate.now(reloj), "USD", new BigDecimal("3.715"), new BigDecimal("3.725"), false);
    }
}
