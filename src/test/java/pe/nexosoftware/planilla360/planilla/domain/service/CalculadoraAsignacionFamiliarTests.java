package pe.nexosoftware.planilla360.planilla.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegalesBuilder.parametros;
import static pe.nexosoftware.planilla360.planilla.domain.model.ParametrosLegalesBuilder.referencia;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pe.nexosoftware.planilla360.compartido.dominio.Dinero;
import pe.nexosoftware.planilla360.planilla.domain.model.Hijo;

@DisplayName("Asignación familiar")
class CalculadoraAsignacionFamiliarTests {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 7);
    private final CalculadoraAsignacionFamiliar calculadora = new CalculadoraAsignacionFamiliar();

    @Test
    void sinHijosNoHayAsignacion() {
        assertThat(calculadora.calcular(List.of(), HOY, referencia())).isEqualTo(Dinero.CERO);
    }

    @Test
    void conHijosElegiblesSeAbonaElDiezPorCientoDeLaRmv() {
        List<Hijo> hijos = List.of(new Hijo(LocalDate.of(2020, 3, 1), false));
        assertThat(calculadora.calcular(hijos, HOY, referencia())).isEqualTo(Dinero.de("113.00"));
    }

    @Test
    void esUnMontoPorTrabajadorNoPorHijo() {
        List<Hijo> hijos = List.of(
                new Hijo(LocalDate.of(2015, 1, 1), false),
                new Hijo(LocalDate.of(2018, 1, 1), false),
                new Hijo(LocalDate.of(2022, 1, 1), false));
        assertThat(calculadora.hijosElegibles(hijos, HOY)).isEqualTo(3);
        assertThat(calculadora.calcular(hijos, HOY, referencia())).isEqualTo(Dinero.de("113.00"));
    }

    @Test
    void usaLaRmvYElPorcentajeVigentes() {
        List<Hijo> hijos = List.of(new Hijo(LocalDate.of(2020, 3, 1), false));
        assertThat(calculadora.calcular(hijos, HOY,
                parametros().conRmv("1200").conPctAsignacionFamiliar("12").build())).isEqualTo(Dinero.de("144.00"));
    }

    @ParameterizedTest(name = "nacido {0}, estudia={1} -> elegible={2}")
    @CsvSource({
        "2008-11-08, false, true",
        "2008-11-07, false, false",
        "2000-01-01, false, false",
        "2008-11-07, true, true",
        "2003-11-07, true, true",
        "2002-11-07, true, true",
        "2001-11-08, true, true",
        "2001-11-07, true, false",
        "2001-11-07, false, false",
        "1990-01-01, true, false"
    })
    void evaluaLaEdadYLosEstudios(String nacimiento, boolean estudia, boolean elegible) {
        Hijo hijo = new Hijo(LocalDate.parse(nacimiento), estudia);
        assertThat(calculadora.hijosElegibles(List.of(hijo), HOY)).isEqualTo(elegible ? 1 : 0);
    }

    @Test
    void soloCuentaLosHijosElegibles() {
        List<Hijo> hijos = List.of(
                new Hijo(LocalDate.of(2020, 1, 1), false),
                new Hijo(LocalDate.of(1995, 1, 1), false),
                new Hijo(LocalDate.of(2005, 5, 5), true));
        assertThat(calculadora.hijosElegibles(hijos, HOY)).isEqualTo(2);
    }

    @Test
    void unHijoPorNacerNoEsElegible() {
        assertThatThrownBy(() -> calculadora.hijosElegibles(List.of(new Hijo(HOY.plusDays(1), false)), HOY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
