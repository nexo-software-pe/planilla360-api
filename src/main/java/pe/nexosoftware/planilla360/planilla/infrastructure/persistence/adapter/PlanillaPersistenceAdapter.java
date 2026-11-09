package pe.nexosoftware.planilla360.planilla.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.compartido.paginacion.Pagina;
import pe.nexosoftware.planilla360.planilla.application.model.FiltroPlanillas;
import pe.nexosoftware.planilla360.planilla.application.model.ResumenPlanilla;
import pe.nexosoftware.planilla360.planilla.application.port.out.PlanillaPort;
import pe.nexosoftware.planilla360.planilla.domain.model.Concepto;
import pe.nexosoftware.planilla360.planilla.domain.model.EstadoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.LineaPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.regimen.Afp;
import pe.nexosoftware.planilla360.planilla.domain.regimen.DescuentoPension;
import pe.nexosoftware.planilla360.planilla.domain.regimen.Onp;
import pe.nexosoftware.planilla360.planilla.domain.regimen.RegimenPensionario;
import pe.nexosoftware.planilla360.planilla.domain.regimen.TipoRegimenPensionario;
import pe.nexosoftware.planilla360.planilla.infrastructure.persistence.entity.ConceptoPlanillaEntity;
import pe.nexosoftware.planilla360.planilla.infrastructure.persistence.entity.LineaPlanillaEntity;
import pe.nexosoftware.planilla360.planilla.infrastructure.persistence.entity.LineaPlanillaEntity.Importes;
import pe.nexosoftware.planilla360.planilla.infrastructure.persistence.entity.PlanillaEntity;
import pe.nexosoftware.planilla360.planilla.infrastructure.persistence.repository.ConceptoPlanillaJpaRepository;
import pe.nexosoftware.planilla360.planilla.infrastructure.persistence.repository.LineaPlanillaJpaRepository;
import pe.nexosoftware.planilla360.planilla.infrastructure.persistence.repository.PlanillaJpaRepository;

@Component
class PlanillaPersistenceAdapter implements PlanillaPort {

    private static final List<EstadoPlanilla> ESTADOS_VIGENTES =
            List.of(EstadoPlanilla.BORRADOR, EstadoPlanilla.CALCULADA, EstadoPlanilla.CERRADA);

    private final PlanillaJpaRepository planillas;
    private final LineaPlanillaJpaRepository lineas;
    private final ConceptoPlanillaJpaRepository conceptos;

    PlanillaPersistenceAdapter(
            PlanillaJpaRepository planillas,
            LineaPlanillaJpaRepository lineas,
            ConceptoPlanillaJpaRepository conceptos) {
        this.planillas = planillas;
        this.lineas = lineas;
        this.conceptos = conceptos;
    }

    @Override
    public Planilla guardar(Planilla planilla) {
        PlanillaEntity entidad = planillas
                .findById(planilla.id())
                .orElseGet(() -> new PlanillaEntity(
                        planilla.id(), planilla.periodo().toString(), planilla.tipo(), planilla.estado()));
        entidad.actualizar(
                planilla.estado(),
                planilla.claveIdempotencia().orElse(null),
                planilla.cerradaEn().orElse(null),
                planilla.lineas().size(),
                planilla.totalIngresos().monto(),
                planilla.totalDescuentos().monto(),
                planilla.totalNeto().monto(),
                planilla.totalEssalud().monto());
        planillas.saveAndFlush(entidad);
        if (planilla.estado().permiteModificarLineas()) {
            reemplazarLineas(planilla);
        }
        return planilla;
    }

    /** Mientras la planilla no está cerrada sus líneas se reescriben completas; cerrada, nunca se tocan. */
    private void reemplazarLineas(Planilla planilla) {
        this.lineas.borrarConceptosDe(planilla.id());
        this.lineas.borrarLineasDe(planilla.id());
        int orden = 0;
        for (LineaPlanilla linea : planilla.lineas()) {
            LineaPlanillaEntity guardada = this.lineas.saveAndFlush(aEntidad(planilla.id(), orden++, linea));
            List<ConceptoPlanillaEntity> renglones = new ArrayList<>();
            int posicion = 0;
            for (Concepto concepto : linea.conceptos()) {
                renglones.add(new ConceptoPlanillaEntity(guardada.getId(), posicion++, concepto.codigo(),
                        concepto.descripcion(), concepto.tipo(), concepto.monto().monto()));
            }
            conceptos.saveAll(renglones);
        }
        conceptos.flush();
    }

    @Override
    public Optional<Planilla> buscar(UUID id) {
        return planillas.findById(id).map(this::aDominio);
    }

    @Override
    public Optional<Planilla> buscarParaActualizar(UUID id) {
        return planillas.buscarConBloqueo(id).map(this::aDominio);
    }

    @Override
    public Optional<UUID> idConClave(String clave) {
        return planillas.findByIdempotencyKey(clave).map(PlanillaEntity::getId);
    }

    @Override
    public boolean existeVigente(Periodo periodo, TipoPlanilla tipo) {
        return planillas.existsByPeriodoAndTipoAndEstadoIn(periodo.toString(), tipo, ESTADOS_VIGENTES);
    }

    @Override
    public boolean existeCerradaDistinta(Periodo periodo, TipoPlanilla tipo, UUID planillaId) {
        return planillas.existsByPeriodoAndTipoAndEstadoAndIdNot(
                periodo.toString(), tipo, EstadoPlanilla.CERRADA, planillaId);
    }

    @Override
    public Pagina<ResumenPlanilla> listar(FiltroPlanillas filtro, int pagina, int tamano) {
        Specification<PlanillaEntity> criterio = Specification.unrestricted();
        if (filtro.periodo() != null) {
            criterio = criterio.and((p, q, cb) -> cb.equal(p.get("periodo"), filtro.periodo().toString()));
        }
        if (filtro.tipo() != null) {
            criterio = criterio.and((p, q, cb) -> cb.equal(p.get("tipo"), filtro.tipo()));
        }
        if (filtro.estado() != null) {
            criterio = criterio.and((p, q, cb) -> cb.equal(p.get("estado"), filtro.estado()));
        }
        Sort orden = Sort.by(Sort.Order.desc("periodo"), Sort.Order.asc("tipo"), Sort.Order.desc("createdAt"));
        Page<PlanillaEntity> resultado = planillas.findAll(criterio, PageRequest.of(pagina, tamano, orden));
        return Pagina.de(resultado, PlanillaPersistenceAdapter::aResumen);
    }

    private static ResumenPlanilla aResumen(PlanillaEntity e) {
        return new ResumenPlanilla(
                e.getId(),
                Periodo.parse(e.getPeriodo()),
                e.getTipo(),
                e.getEstado(),
                e.getTrabajadores(),
                Dinero.de(e.getTotalIngresos()),
                Dinero.de(e.getTotalDescuentos()),
                Dinero.de(e.getTotalNeto()),
                Dinero.de(e.getTotalEssalud()),
                e.getCerradaEn());
    }

    private Planilla aDominio(PlanillaEntity e) {
        List<LineaPlanilla> deLaPlanilla = lineas.findByPlanillaIdOrderByOrden(e.getId()).stream()
                .map(PlanillaPersistenceAdapter::aDominio)
                .toList();
        return Planilla.reconstruir(
                e.getId(),
                e.getTenantId(),
                Periodo.parse(e.getPeriodo()),
                e.getTipo(),
                e.getEstado(),
                deLaPlanilla,
                e.getIdempotencyKey(),
                e.getCerradaEn());
    }

    private static LineaPlanillaEntity aEntidad(UUID planillaId, int orden, LineaPlanilla l) {
        TipoRegimenPensionario tipo;
        String afpNombre = null;
        BigDecimal comision = null;
        switch (l.regimenPensionario()) {
            case Onp onp -> tipo = TipoRegimenPensionario.ONP;
            case Afp afp -> {
                tipo = TipoRegimenPensionario.AFP;
                afpNombre = afp.administradora();
                comision = afp.comisionPct();
            }
        }
        Importes importes = new Importes(
                l.basico().monto(),
                l.asignacionFamiliar().monto(),
                l.horasExtra().monto(),
                l.bonos().monto(),
                l.gratificacion().monto(),
                l.bonificacionExtraordinaria().monto(),
                l.cts().monto(),
                l.descuentoPension().aporte().monto(),
                l.descuentoPension().prima().monto(),
                l.descuentoPension().comision().monto(),
                l.retencion5ta().monto(),
                l.descuentoAsistencia().monto(),
                l.otrosDescuentos().monto(),
                l.aporteEssalud().monto(),
                l.totalIngresos().monto(),
                l.totalDescuentos().monto(),
                l.neto().monto());
        return new LineaPlanillaEntity(planillaId, l.empleadoId(), orden, importes, tipo, afpNombre, comision);
    }

    private static LineaPlanilla aDominio(LineaPlanillaEntity e) {
        RegimenPensionario regimen = e.getRegimenPensionario() == TipoRegimenPensionario.AFP
                ? new Afp(e.getAfpNombre(), e.getAfpComisionPct())
                : new Onp();
        return new LineaPlanilla(
                e.getEmpleadoId(),
                Dinero.de(e.getBasico()),
                Dinero.de(e.getAsignacionFamiliar()),
                Dinero.de(e.getHorasExtra()),
                Dinero.de(e.getBonos()),
                Dinero.de(e.getGratificacion()),
                Dinero.de(e.getBonificacionExtraordinaria()),
                Dinero.de(e.getCts()),
                regimen,
                new DescuentoPension(
                        Dinero.de(e.getPensionAporte()),
                        Dinero.de(e.getPensionPrima()),
                        Dinero.de(e.getPensionComision())),
                Dinero.de(e.getRetencion5ta()),
                Dinero.de(e.getDescuentoAsistencia()),
                Dinero.de(e.getOtrosDescuentos()),
                Dinero.de(e.getAporteEssalud()));
    }
}
