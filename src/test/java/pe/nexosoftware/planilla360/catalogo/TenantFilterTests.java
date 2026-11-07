package pe.nexosoftware.planilla360.catalogo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.nexosoftware.planilla360.catalogo.entity.Area;
import pe.nexosoftware.planilla360.catalogo.entity.Cargo;
import pe.nexosoftware.planilla360.catalogo.repository.AreaRepository;
import pe.nexosoftware.planilla360.catalogo.repository.CargoRepository;
import pe.nexosoftware.planilla360.compartido.infrastructure.PersistenciaSliceConfig;
import pe.nexosoftware.planilla360.compartido.tenant.TenantContext;
import pe.nexosoftware.planilla360.empresa.PlanSaas;
import pe.nexosoftware.planilla360.empresa.RegimenLaboral;
import pe.nexosoftware.planilla360.empresa.entity.Empresa;
import pe.nexosoftware.planilla360.empresa.repository.EmpresaRepository;
import pe.nexosoftware.planilla360.soporte.RucDePrueba;
import pe.nexosoftware.planilla360.soporte.TestcontainersConfig;

/**
 * Filtro por empresa de Hibernate ({@code @TenantId}) contra PostgreSQL real. Cada operación abre su propia
 * sesión (sin transacción de prueba), porque el tenant se fija al abrir la sesión.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfig.class, PersistenciaSliceConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TenantFilterTests {

    @Autowired
    private AreaRepository areas;

    @Autowired
    private CargoRepository cargos;

    @Autowired
    private EmpresaRepository empresas;

    @Autowired
    private JdbcTemplate jdbc;

    private Empresa empresa() {
        return empresas.save(new Empresa(RucDePrueba.nuevo(), "Empresa", RegimenLaboral.GENERAL, PlanSaas.BASICO));
    }

    @AfterEach
    void limpiar() {
        TenantContext.limpiar();
        jdbc.update("delete from cargo");
        jdbc.update("delete from area");
        jdbc.update("delete from empresa");
    }

    @Test
    void alGuardarSeAsignaLaEmpresaDelContexto() {
        Empresa a = empresa();
        TenantContext.establecer(a.getId());

        Area guardada = areas.save(new Area("Producción"));

        assertThat(guardada.getTenantId()).isEqualTo(a.getId());
    }

    @Test
    void lasConsultasSoloDevuelvenFilasDeLaEmpresaActual() {
        Empresa a = empresa();
        Empresa b = empresa();
        TenantContext.establecer(a.getId());
        areas.save(new Area("Área de A"));
        TenantContext.establecer(b.getId());
        areas.save(new Area("Área de B"));

        assertThat(areas.findAll()).extracting(Area::getNombre).containsExactly("Área de B");
        TenantContext.establecer(a.getId());
        assertThat(areas.findAll()).extracting(Area::getNombre).containsExactly("Área de A");
    }

    @Test
    void buscarPorIdUnaFilaAjenaNoLaEncuentra() {
        Empresa a = empresa();
        Empresa b = empresa();
        TenantContext.establecer(a.getId());
        UUID idDeA = areas.save(new Area("Área de A")).getId();

        TenantContext.establecer(b.getId());
        assertThat(areas.findById(idDeA)).isEmpty();
        assertThat(areas.existsById(idDeA)).isFalse();
        assertThat(areas.existsByNombreIgnoreCase("Área de A")).isFalse();
    }

    @Test
    void sinEmpresaEnContextoNoSeVeNada() {
        Empresa a = empresa();
        TenantContext.establecer(a.getId());
        areas.save(new Area("Área de A"));

        TenantContext.limpiar();

        assertThat(areas.findAll()).isEmpty();
    }

    @Test
    void sinEmpresaEnContextoNoSePuedeEscribir() {
        TenantContext.limpiar();

        assertThatThrownBy(() -> areas.saveAndFlush(new Area("Huérfana"))).isInstanceOf(Exception.class);
    }

    @Test
    void laRelacionCargoAreaTambienRespetaElAislamiento() {
        Empresa a = empresa();
        Empresa b = empresa();
        TenantContext.establecer(a.getId());
        Area area = areas.save(new Area("Producción"));
        cargos.save(new Cargo(area.getId(), "Operario", new BigDecimal("1300.00")));

        TenantContext.establecer(b.getId());
        assertThat(cargos.findByAreaId(area.getId(), org.springframework.data.domain.Pageable.unpaged())).isEmpty();
        assertThat(cargos.existsByAreaId(area.getId())).isFalse();
    }
}
