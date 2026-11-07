package pe.nexosoftware.planilla360.auditoria.service;

import java.time.Clock;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.auditoria.entity.AuditoriaCambio;
import pe.nexosoftware.planilla360.auditoria.repository.AuditoriaCambioRepository;
import pe.nexosoftware.planilla360.compartido.eventos.CambioRegistrado;
import pe.nexosoftware.planilla360.compartido.seguridad.UsuarioAutenticado;
import tools.jackson.databind.json.JsonMapper;

/**
 * Guarda el historial de cada {@link CambioRegistrado}. Es un listener síncrono: se ejecuta dentro de la
 * transacción de quien publica, de modo que el cambio y su auditoría se confirman (o revierten) juntos.
 */
@Component
class RegistroAuditoriaListener {

    private final AuditoriaCambioRepository repository;
    private final JsonMapper mapper;
    private final Clock clock;

    RegistroAuditoriaListener(AuditoriaCambioRepository repository, JsonMapper mapper, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    @EventListener
    void alRegistrarCambio(CambioRegistrado evento) {
        String usuario = UsuarioAutenticado.actual().map(UsuarioAutenticado::email).orElse("sistema");
        repository.save(new AuditoriaCambio(
                evento.tenantId(),
                evento.entidad(),
                evento.entidadId(),
                evento.accion(),
                usuario,
                clock.instant(),
                serializar(evento.antes()),
                serializar(evento.despues())));
    }

    private String serializar(Object instantanea) {
        return instantanea == null ? null : mapper.writeValueAsString(instantanea);
    }
}
