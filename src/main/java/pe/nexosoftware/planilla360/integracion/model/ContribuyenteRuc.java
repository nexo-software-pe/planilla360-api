package pe.nexosoftware.planilla360.integracion.model;

/** Datos de un contribuyente según el padrón de RUC. */
public record ContribuyenteRuc(
        String ruc, String razonSocial, String estado, String condicion, String direccion) {
}
