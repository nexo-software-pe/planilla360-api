package pe.nexosoftware.planilla360.auditoria.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.nexosoftware.planilla360.auditoria.entity.AuditoriaCambio;

public interface AuditoriaCambioRepository
        extends JpaRepository<AuditoriaCambio, UUID>, JpaSpecificationExecutor<AuditoriaCambio> {
}
