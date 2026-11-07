package pe.nexosoftware.planilla360.planilla.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegalesBuilder.parametros;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;

@DisplayName("ParametrosLegales")
class ParametrosLegalesTests {

    @Test
    void aceptaLosValoresDeReferencia() {
        ParametrosLegales p = ParametrosLegalesBuilder.referencia();
        assertThat(p.rmv()).isEqualTo(Dinero.de("1130"));
        assertThat(p.uit()).isEqualTo(Dinero.de("5350"));
        assertThat(p.pctOnp()).isEqualByComparingTo("13");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1130"})
    void laRmvDebeSerPositiva(String rmv) {
        assertThatThrownBy(() -> parametros().conRmv(rmv).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RMV");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-5350"})
    void laUitDebeSerPositiva(String uit) {
        assertThatThrownBy(() -> parametros().conUit(uit).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("UIT");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.01", "100.01", "250"})
    void losPorcentajesDebenEstarEntreCeroYCien(String pct) {
        assertThatThrownBy(() -> parametros().conPctOnp(pct).build()).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ONP");
        assertThatThrownBy(() -> parametros().conPctEssalud(pct).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EsSalud");
        assertThatThrownBy(() -> parametros().conPctAsignacionFamiliar(pct).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("asignación familiar");
        assertThatThrownBy(() -> parametros().conPctBonificacionExtraordinaria(pct).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bonificación");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "100"})
    void losLimitesDelPorcentajeSonValidos(String pct) {
        assertThat(parametros().conPctOnp(pct).build().pctOnp()).isEqualByComparingTo(pct);
    }

    @Test
    void todosLosCamposSonObligatorios() {
        BigDecimal pct = BigDecimal.TEN;
        Dinero monto = Dinero.de("1");
        assertThatNullPointerException().isThrownBy(
                () -> new ParametrosLegales(null, monto, pct, pct, pct, pct, pct, pct));
        assertThatNullPointerException().isThrownBy(
                () -> new ParametrosLegales(monto, null, pct, pct, pct, pct, pct, pct));
        assertThatNullPointerException().isThrownBy(
                () -> new ParametrosLegales(monto, monto, null, pct, pct, pct, pct, pct));
        assertThatNullPointerException().isThrownBy(
                () -> new ParametrosLegales(monto, monto, pct, null, pct, pct, pct, pct));
        assertThatNullPointerException().isThrownBy(
                () -> new ParametrosLegales(monto, monto, pct, pct, null, pct, pct, pct));
        assertThatNullPointerException().isThrownBy(
                () -> new ParametrosLegales(monto, monto, pct, pct, pct, null, pct, pct));
        assertThatNullPointerException().isThrownBy(
                () -> new ParametrosLegales(monto, monto, pct, pct, pct, pct, null, pct));
        assertThatNullPointerException().isThrownBy(
                () -> new ParametrosLegales(monto, monto, pct, pct, pct, pct, pct, null));
    }
}
