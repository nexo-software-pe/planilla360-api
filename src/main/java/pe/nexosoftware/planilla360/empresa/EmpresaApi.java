package pe.nexosoftware.planilla360.empresa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** API pública del módulo de empresas (tenancy). */
public interface EmpresaApi {

    Optional<EmpresaResumen> buscarPorId(UUID id);

    /** Identificadores de las empresas activas (para los procesos programados que recorren todas). */
    List<UUID> idsActivas();
}
