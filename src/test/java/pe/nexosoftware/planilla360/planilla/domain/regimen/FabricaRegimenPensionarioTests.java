package pe.nexosoftware.planilla360.planilla.domain.regimen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.nexosoftware.planilla360.planilla.domain.excepcion.ReglaDePlanillaException;

@DisplayName("FabricaRegimenPensionario")
class FabricaRegimenPensionarioTests {

    private static final AfpVigente PRIMA = new AfpVigente("Prima", new BigDecimal("1.60"), new BigDecimal("1.25"));

    @Test
    void creaOnpParaQuienEligioElSistemaNacional() {
        assertThat(FabricaRegimenPensionario.crear(TipoRegimenPensionario.ONP, null, null)).isInstanceOf(Onp.class);
    }

    @Test
    void onpIgnoraLosDatosDeAfp() {
        assertThat(FabricaRegimenPensionario.crear(TipoRegimenPensionario.ONP, PRIMA, TipoComision.FLUJO))
                .isEqualTo(new Onp());
    }

    @Test
    void creaAfpConComisionFlujo() {
        assertThat(FabricaRegimenPensionario.crear(TipoRegimenPensionario.AFP, PRIMA, TipoComision.FLUJO))
                .isEqualTo(new Afp("Prima", new BigDecimal("1.60")));
    }

    @Test
    void creaAfpConComisionMixta() {
        assertThat(FabricaRegimenPensionario.crear(TipoRegimenPensionario.AFP, PRIMA, TipoComision.MIXTA))
                .isEqualTo(new Afp("Prima", new BigDecimal("1.25")));
    }

    @Test
    void afpExigeLaAdministradora() {
        assertThatThrownBy(() -> FabricaRegimenPensionario.crear(TipoRegimenPensionario.AFP, null,
                TipoComision.FLUJO))
                .isInstanceOf(ReglaDePlanillaException.class)
                .hasMessageContaining("AFP");
    }

    @Test
    void afpExigeElTipoDeComision() {
        assertThatThrownBy(() -> FabricaRegimenPensionario.crear(TipoRegimenPensionario.AFP, PRIMA, null))
                .isInstanceOf(ReglaDePlanillaException.class)
                .hasMessageContaining("comisión");
    }

    @Test
    void exigeElTipoDeRegimen() {
        assertThatNullPointerException().isThrownBy(() -> FabricaRegimenPensionario.crear(null, null, null));
    }

    @Test
    void laAfpVigenteEligeLaComisionSegunElTipo() {
        assertThat(PRIMA.comision(TipoComision.FLUJO)).isEqualByComparingTo("1.60");
        assertThat(PRIMA.comision(TipoComision.MIXTA)).isEqualByComparingTo("1.25");
    }
}
