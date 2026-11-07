package pe.nexosoftware.planilla360.identidad.dto;

import pe.nexosoftware.planilla360.empresa.EmpresaResumen;

/** Usuario autenticado y la empresa activa (nula para el administrador de la plataforma). */
public record PerfilResponse(UsuarioResponse usuario, EmpresaResumen empresa) {
}
