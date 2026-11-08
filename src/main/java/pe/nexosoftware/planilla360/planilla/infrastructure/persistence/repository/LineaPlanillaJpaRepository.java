package pe.nexosoftware.planilla360.planilla.infrastructure.persistence.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.nexosoftware.planilla360.planilla.domain.model.EstadoPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;
import pe.nexosoftware.planilla360.planilla.infrastructure.persistence.entity.LineaPlanillaEntity;

public interface LineaPlanillaJpaRepository extends JpaRepository<LineaPlanillaEntity, UUID> {

    /** Total por trabajador. */
    interface SumaPorEmpleado {
        UUID getEmpleadoId();

        BigDecimal getTotal();
    }

    /** Un monto de un trabajador en un periodo. */
    interface MontoDelPeriodo {
        UUID getEmpleadoId();

        String getPeriodo();

        BigDecimal getMonto();
    }

    List<LineaPlanillaEntity> findByPlanillaIdOrderByOrden(UUID planillaId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ConceptoPlanillaEntity c where c.lineaId in "
            + "(select l.id from LineaPlanillaEntity l where l.planillaId = :planillaId)")
    void borrarConceptosDe(@Param("planillaId") UUID planillaId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from LineaPlanillaEntity l where l.planillaId = :planillaId")
    void borrarLineasDe(@Param("planillaId") UUID planillaId);

    @Query("select l.empleadoId as empleadoId, sum(l.totalIngresos) as total "
            + "from LineaPlanillaEntity l join PlanillaEntity p on p.id = l.planillaId "
            + "where p.estado = :estado and p.tipo in :tipos and p.periodo >= :desde and p.periodo < :hasta "
            + "group by l.empleadoId")
    List<SumaPorEmpleado> ingresosPagados(
            @Param("estado") EstadoPlanilla estado,
            @Param("tipos") Collection<TipoPlanilla> tipos,
            @Param("desde") String desde,
            @Param("hasta") String hasta);

    @Query("select l.empleadoId as empleadoId, p.periodo as periodo, l.horasExtra as monto "
            + "from LineaPlanillaEntity l join PlanillaEntity p on p.id = l.planillaId "
            + "where p.estado = :estado and p.tipo = :tipo and p.periodo >= :desde and p.periodo <= :hasta "
            + "order by p.periodo")
    List<MontoDelPeriodo> horasExtraPagadas(
            @Param("estado") EstadoPlanilla estado,
            @Param("tipo") TipoPlanilla tipo,
            @Param("desde") String desde,
            @Param("hasta") String hasta);

    @Query("select l.empleadoId as empleadoId, p.periodo as periodo, l.gratificacion as monto "
            + "from LineaPlanillaEntity l join PlanillaEntity p on p.id = l.planillaId "
            + "where p.estado = :estado and p.tipo = :tipo and p.periodo >= :desde and p.periodo < :hasta "
            + "order by p.periodo")
    List<MontoDelPeriodo> gratificacionesPagadas(
            @Param("estado") EstadoPlanilla estado,
            @Param("tipo") TipoPlanilla tipo,
            @Param("desde") String desde,
            @Param("hasta") String hasta);
}
