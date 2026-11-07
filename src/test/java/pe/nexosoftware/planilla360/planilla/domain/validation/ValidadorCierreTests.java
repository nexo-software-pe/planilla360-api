package pe.nexosoftware.planilla360.planilla.domain.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.nexosoftware.planilla360.planilla.domain.model.LineasDePrueba.empleado;
import static pe.nexosoftware.planilla360.planilla.domain.model.LineasDePrueba.linea;
import static pe.nexosoftware.planilla360.planilla.domain.model.LineasDePrueba.lineaConNetoNegativo;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pe.nexosoftware.planilla360.compartido.dominio.Periodo;
import pe.nexosoftware.planilla360.planilla.domain.model.LineaPlanilla;
import pe.nexosoftware.planilla360.planilla.domain.model.Planilla;
import pe.nexosoftware.planilla360.planilla.domain.model.TipoPlanilla;

@DisplayName("Validaciones previas al cierre (Chain of Responsibility)")
class ValidadorCierreTests {

    private static Planilla calculada(LineaPlanilla... lineas) {
        Planilla planilla = Planilla.crearBorrador(new UUID(1, 1), new UUID(2, 2), Periodo.de(2026, 10),
                TipoPlanilla.MENSUAL);
        planilla.registrarCalculo(List.of(lineas));
        return planilla;
    }

    private static ContextoCierre contextoLimpio() {
        return new ContextoCierre(calculada(linea(1), linea(2)), Set.of(), true, 0, false);
    }

    private static List<String> codigos(List<Incidencia> incidencias) {
        return incidencias.stream().map(Incidencia::codigo).toList();
    }

    @Test
    void unaPlanillaEnOrdenNoTieneIncidencias() {
        ResultadoValidacion resultado = ValidadorCierre.estandar().validar(contextoLimpio());
        assertThat(resultado.esValida()).isTrue();
        assertThat(resultado.incidencias()).isEmpty();
    }

    @Test
    void reportaTodasLasIncidenciasJuntasEnElOrdenDeLaCadena() {
        ContextoCierre contexto = new ContextoCierre(
                calculada(linea(1), lineaConNetoNegativo(2)), Set.of(empleado(1)), false, 2, true);

        ResultadoValidacion resultado = ValidadorCierre.estandar().validar(contexto);

        assertThat(resultado.esValida()).isFalse();
        assertThat(codigos(resultado.incidencias())).containsExactly(
                "SIN_REGIMEN_PENSIONARIO", "ASISTENCIA_INCOMPLETA", "VACACIONES_PENDIENTES", "PLANILLA_DUPLICADA",
                "NETO_NEGATIVO");
    }

    @Test
    void elResultadoNoSePuedeModificar() {
        ContextoCierre contexto = new ContextoCierre(calculada(linea(1)), Set.of(), false, 0, false);
        ResultadoValidacion resultado = ValidadorCierre.estandar().validar(contexto);
        List<Incidencia> incidencias = resultado.incidencias();
        assertThat(incidencias).hasSize(1);
        assertThatThrownBy(incidencias::clear).isInstanceOf(UnsupportedOperationException.class);
    }

    @Nested
    @DisplayName("Mecánica de la cadena")
    class Cadena {

        private final class Fijo extends EslabonValidacion {
            private final String codigo;

            private Fijo(String codigo) {
                this.codigo = codigo;
            }

            @Override
            protected List<Incidencia> revisar(ContextoCierre contexto) {
                return List.of(new Incidencia(codigo, "mensaje " + codigo));
            }
        }

        private final class Ninguno extends EslabonValidacion {
            @Override
            protected List<Incidencia> revisar(ContextoCierre contexto) {
                return List.of();
            }
        }

        @Test
        void unEslabonSinSiguienteSoloAportaLoSuyo() {
            assertThat(codigos(new Fijo("A").validar(contextoLimpio()))).containsExactly("A");
        }

        @Test
        void luegoDevuelveElSiguienteParaEncadenarEnFluido() {
            Fijo a = new Fijo("A");
            Fijo b = new Fijo("B");
            Fijo c = new Fijo("C");
            assertThat(a.luego(b)).isSameAs(b);
            b.luego(c);
            assertThat(codigos(a.validar(contextoLimpio()))).containsExactly("A", "B", "C");
        }

        @Test
        void unEslabonSinProblemasNoCortaLaCadena() {
            Ninguno inicio = new Ninguno();
            inicio.luego(new Fijo("B"));
            assertThat(codigos(inicio.validar(contextoLimpio()))).containsExactly("B");
        }

        @Test
        void validarDesdeElMedioNoIncluyeLosAnteriores() {
            Fijo a = new Fijo("A");
            Fijo b = new Fijo("B");
            a.luego(b);
            assertThat(codigos(b.validar(contextoLimpio()))).containsExactly("B");
        }
    }

    @Nested
    @DisplayName("Eslabones")
    class Eslabones {

        @Test
        void trabajadoresSinRegimenPensionarioGeneranUnaIncidenciaPorTrabajador() {
            ContextoCierre contexto = new ContextoCierre(calculada(linea(1), linea(2)),
                    Set.of(empleado(1), empleado(2)), true, 0, false);

            List<Incidencia> incidencias = new TrabajadoresConRegimenPensionario().validar(contexto);

            assertThat(incidencias).hasSize(2);
            assertThat(incidencias).allSatisfy(i -> assertThat(i.codigo()).isEqualTo("SIN_REGIMEN_PENSIONARIO"));
            assertThat(incidencias).extracting(Incidencia::mensaje)
                    .anyMatch(m -> m.contains(empleado(1).toString()))
                    .anyMatch(m -> m.contains(empleado(2).toString()));
        }

        @Test
        void todosConRegimenPensionarioNoGeneraIncidencias() {
            assertThat(new TrabajadoresConRegimenPensionario().validar(contextoLimpio())).isEmpty();
        }

        @Test
        void asistenciaIncompletaGeneraUnaIncidencia() {
            ContextoCierre contexto = new ContextoCierre(calculada(linea(1)), Set.of(), false, 0, false);
            List<Incidencia> incidencias = new AsistenciaDelMesCompleta().validar(contexto);
            assertThat(codigos(incidencias)).containsExactly("ASISTENCIA_INCOMPLETA");
            assertThat(new AsistenciaDelMesCompleta().validar(contextoLimpio())).isEmpty();
        }

        @Test
        void vacacionesPendientesIndicanCuantasSon() {
            ContextoCierre contexto = new ContextoCierre(calculada(linea(1)), Set.of(), true, 3, false);
            List<Incidencia> incidencias = new SinVacacionesPendientes().validar(contexto);
            assertThat(codigos(incidencias)).containsExactly("VACACIONES_PENDIENTES");
            assertThat(incidencias.get(0).mensaje()).contains("3");
            assertThat(new SinVacacionesPendientes().validar(contextoLimpio())).isEmpty();
        }

        @Test
        void otraPlanillaCerradaDelMismoPeriodoGeneraUnaIncidencia() {
            ContextoCierre contexto = new ContextoCierre(calculada(linea(1)), Set.of(), true, 0, true);
            assertThat(codigos(new SinOtraPlanillaCerrada().validar(contexto))).containsExactly("PLANILLA_DUPLICADA");
            assertThat(new SinOtraPlanillaCerrada().validar(contextoLimpio())).isEmpty();
        }

        @Test
        void soloLosNetosNegativosGeneranIncidencia() {
            ContextoCierre contexto = new ContextoCierre(
                    calculada(linea(1), lineaConNetoNegativo(2), lineaConNetoNegativo(3)), Set.of(), true, 0, false);

            List<Incidencia> incidencias = new NetosNoNegativos().validar(contexto);

            assertThat(incidencias).hasSize(2);
            assertThat(incidencias).extracting(Incidencia::mensaje)
                    .anyMatch(m -> m.contains(empleado(2).toString()))
                    .anyMatch(m -> m.contains(empleado(3).toString()))
                    .noneMatch(m -> m.contains(empleado(1).toString()));
            assertThat(new NetosNoNegativos().validar(contextoLimpio())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Contexto de cierre")
    class Contexto {

        @Test
        void exigeLaPlanilla() {
            Set<UUID> ninguno = Set.of();
            assertThatNullPointerException().isThrownBy(() -> new ContextoCierre(null, ninguno, true, 0, false));
        }

        @Test
        void rechazaConteoNegativoDeVacaciones() {
            Planilla planilla = calculada(linea(1));
            Set<UUID> ninguno = Set.of();
            assertThatThrownBy(() -> new ContextoCierre(planilla, ninguno, true, -1, false))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void copiaElConjuntoDeTrabajadoresSinRegimen() {
            java.util.HashSet<UUID> origen = new java.util.HashSet<>();
            ContextoCierre contexto = new ContextoCierre(calculada(linea(1)), origen, true, 0, false);
            origen.add(empleado(1));
            assertThat(contexto.empleadosSinRegimenPensionario()).isEmpty();
        }
    }

    @Test
    void elResultadoSabeSiEsValida() {
        assertThat(new ResultadoValidacion(List.of()).esValida()).isTrue();
        assertThat(new ResultadoValidacion(List.of(new Incidencia("X", "x"))).esValida()).isFalse();
    }
}
