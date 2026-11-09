package pe.nexosoftware.planilla360.integracion.service;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;
import pe.nexosoftware.planilla360.compartido.dominio.Dni;
import pe.nexosoftware.planilla360.compartido.dominio.Ruc;
import pe.nexosoftware.planilla360.compartido.excepcion.ReglaDeNegocioException;
import pe.nexosoftware.planilla360.compartido.excepcion.ServicioNoDisponibleException;
import pe.nexosoftware.planilla360.integracion.cliente.IntegracionProperties;
import pe.nexosoftware.planilla360.integracion.cliente.ProveedorConsultas;
import pe.nexosoftware.planilla360.integracion.cliente.ResilienciaExterna;
import pe.nexosoftware.planilla360.integracion.model.ContribuyenteRuc;
import pe.nexosoftware.planilla360.integracion.model.PersonaDni;
import pe.nexosoftware.planilla360.integracion.model.TipoCambio;

/**
 * Consultas a servicios externos con resiliencia. El tipo de cambio se guarda un rato (cambia una vez al día) y, si
 * el servicio no responde, se devuelve el último valor conocido marcado como desactualizado.
 */
@Service
public class ConsultaExternaService {

    private record Guardado(TipoCambio valor, Instant obtenidoEn) {
    }

    private final ProveedorConsultas proveedor;
    private final ResilienciaExterna resiliencia;
    private final IntegracionProperties propiedades;
    private final Clock reloj;
    private final AtomicReference<Guardado> tipoCambio = new AtomicReference<>();

    public ConsultaExternaService(
            ProveedorConsultas proveedor,
            ResilienciaExterna resiliencia,
            IntegracionProperties propiedades,
            Clock reloj) {
        this.proveedor = proveedor;
        this.resiliencia = resiliencia;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    public PersonaDni dni(String dni) {
        if (!Dni.esValido(dni)) {
            throw new ReglaDeNegocioException("El DNI debe tener 8 dígitos.");
        }
        return resiliencia.ejecutar(() -> proveedor.consultarDni(dni));
    }

    public ContribuyenteRuc ruc(String ruc) {
        if (!Ruc.esValido(ruc)) {
            throw new ReglaDeNegocioException("El RUC no es válido (11 dígitos con dígito verificador).");
        }
        return resiliencia.ejecutar(() -> proveedor.consultarRuc(ruc));
    }

    public TipoCambio tipoCambio() {
        Instant ahora = Instant.now(reloj);
        Guardado guardado = tipoCambio.get();
        if (guardado != null && guardado.obtenidoEn().plus(propiedades.cacheTipoCambio()).isAfter(ahora)) {
            return guardado.valor();
        }
        try {
            TipoCambio nuevo = resiliencia.ejecutar(proveedor::consultarTipoCambio);
            tipoCambio.set(new Guardado(nuevo, ahora));
            return nuevo;
        } catch (ServicioNoDisponibleException e) {
            if (guardado != null) {
                return guardado.valor().comoDesactualizado();
            }
            throw e;
        }
    }
}
