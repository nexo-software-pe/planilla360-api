package pe.nexosoftware.planilla360.integracion.cliente;

import pe.nexosoftware.planilla360.integracion.model.ContribuyenteRuc;
import pe.nexosoftware.planilla360.integracion.model.PersonaDni;
import pe.nexosoftware.planilla360.integracion.model.TipoCambio;

/**
 * Puerto de salida hacia el servicio externo de consultas (patrón Adapter). Las implementaciones lanzan
 * {@code RecursoNoEncontradoException} cuando el dato no existe y {@code ErrorTransitorioException} cuando el
 * servicio falla (la capa de resiliencia decide si reintenta o corta el circuito).
 */
public interface ProveedorConsultas {

    PersonaDni consultarDni(String dni);

    ContribuyenteRuc consultarRuc(String ruc);

    TipoCambio consultarTipoCambio();
}
