package pe.nexosoftware.planilla360.empresa;

import java.util.Optional;
import java.util.UUID;

/** API pública del módulo de empresas (tenancy). */
public interface EmpresaApi {

    Optional<EmpresaResumen> buscarPorId(UUID id);
}
