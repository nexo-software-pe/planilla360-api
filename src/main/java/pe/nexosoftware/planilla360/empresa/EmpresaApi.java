package pe.nexosoftware.planilla360.empresa;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

/** API pública del módulo de empresas (tenancy). */
public interface EmpresaApi {

    Optional<EmpresaResumen> buscarPorId(UUID id);

    /**
     * Ejecuta la acción una vez por cada empresa activa, con esa empresa como contexto (tenant). Si la
     * acción falla para una empresa, se registra el error y se continúa con las demás. Es la forma de
     * escribir los procesos programados que recorren todas las empresas.
     */
    void porCadaEmpresaActiva(Consumer<UUID> accion);
}
