package pe.nexosoftware.planilla360.planilla.domain.regimen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;

@DisplayName("RegimenLaboral")
class RegimenLaboralTests {

    @ParameterizedTest(name = "{0}: {1} -> {2}")
    @CsvSource({
        "GENERAL, 1000.00, 1000.00",
        "GENERAL, 333.33, 333.33",
        "PEQUENA_EMPRESA, 1000.00, 500.00",
        "PEQUENA_EMPRESA, 333.33, 166.67",
        "PEQUENA_EMPRESA, 0.00, 0.00"
    })
    void aplicaElPorcentajeDeBeneficiosDelRegimen(RegimenLaboral regimen, String completo, String esperado) {
        assertThat(regimen.aplicarBeneficio(Dinero.de(completo))).isEqualTo(Dinero.de(esperado));
    }

    @Test
    void conoceLosDiasDeVacaciones() {
        assertThat(RegimenLaboral.GENERAL.diasVacacionesPorAnio()).isEqualTo(30);
        assertThat(RegimenLaboral.PEQUENA_EMPRESA.diasVacacionesPorAnio()).isEqualTo(15);
    }

    @Test
    void soloElRegimenGeneralPermiteVenderVacaciones() {
        assertThat(RegimenLaboral.GENERAL.permiteVenderVacaciones()).isTrue();
        assertThat(RegimenLaboral.PEQUENA_EMPRESA.permiteVenderVacaciones()).isFalse();
    }
}
