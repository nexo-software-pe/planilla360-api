package pe.nexosoftware.planilla360.planilla.infrastructure.persistence.adapter;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.planilla.application.model.PagoPlanilla;
import pe.nexosoftware.planilla360.planilla.application.port.out.PagoPlanillaPort;
import pe.nexosoftware.planilla360.planilla.infrastructure.persistence.entity.PagoPlanillaEntity;
import pe.nexosoftware.planilla360.planilla.infrastructure.persistence.repository.PagoPlanillaJpaRepository;

@Component
class PagoPlanillaPersistenceAdapter implements PagoPlanillaPort {

    private final PagoPlanillaJpaRepository repository;

    PagoPlanillaPersistenceAdapter(PagoPlanillaJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public PagoPlanilla guardar(PagoPlanilla pago) {
        repository.saveAndFlush(new PagoPlanillaEntity(pago.planillaId(), pago.fechaPago(), pago.referencia()));
        return pago;
    }

    @Override
    public Optional<PagoPlanilla> buscar(UUID planillaId) {
        return repository.findByPlanillaId(planillaId)
                .map(e -> new PagoPlanilla(e.getPlanillaId(), e.getFechaPago(), e.getReferencia()));
    }
}
