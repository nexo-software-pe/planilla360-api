package pe.nexosoftware.planilla360.empresa;

import java.util.UUID;

/** Vista pública mínima de una empresa cliente para el resto de módulos. */
public record EmpresaResumen(
        UUID id, String ruc, String razonSocial, RegimenLaboral regimenLaboral, boolean activa) {
}
