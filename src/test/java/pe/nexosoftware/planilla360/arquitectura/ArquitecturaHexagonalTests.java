package pe.nexosoftware.planilla360.arquitectura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/**
 * Reglas hexagonales de los módulos núcleo {@code asistencia} y {@code vacaciones}: el dominio es Java
 * puro, la aplicación solo conoce el dominio y sus puertos, y la infraestructura implementa los puertos.
 * (Las reglas de {@code planilla} están en {@link ArquitecturaPlanillaTests}.)
 */
@AnalyzeClasses(packages = "pe.nexosoftware.planilla360", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaHexagonalTests {

    @ArchTest
    static final ArchRule EL_DOMINIO_SOLO_USA_JAVA_Y_EL_KERNEL = noClasses()
            .that().resideInAnyPackage("..asistencia.domain..", "..vacaciones.domain..")
            .should().dependOnClassesThat().resideOutsideOfPackages(
                    "..asistencia.domain..", "..vacaciones.domain..", "..compartido.dominio..", "java..")
            .because("el dominio de los módulos núcleo es Java puro: sin Spring, JPA ni otros módulos");

    @ArchTest
    static final ArchRule EL_DOMINIO_NO_CONOCE_LAS_CAPAS_EXTERNAS = noClasses()
            .that().resideInAnyPackage("..asistencia.domain..", "..vacaciones.domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..application..", "..infrastructure..");

    @ArchTest
    static final ArchRule LA_APLICACION_NO_CONOCE_LA_INFRAESTRUCTURA = noClasses()
            .that().resideInAnyPackage("..asistencia.application..", "..vacaciones.application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .because("la aplicación depende de puertos (interfaces), no de adaptadores");

    @ArchTest
    static final ArchRule LA_APLICACION_NO_USA_JPA_NI_LA_CAPA_WEB = noClasses()
            .that().resideInAnyPackage("..asistencia.application..", "..vacaciones.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..", "org.hibernate..", "org.springframework.data..",
                    "org.springframework.web..", "org.springframework.http..")
            .because("la lógica de los casos de uso no sabe cómo se guarda ni cómo se expone");

    @ArchTest
    static final ArchRule LA_APLICACION_NO_USA_OTROS_MODULOS_DIRECTAMENTE = noClasses()
            .that().resideInAnyPackage("..asistencia.application..", "..vacaciones.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..empleado..", "..empresa..", "..catalogo..", "..identidad..")
            .because("otros módulos se consultan por puertos de salida con un adaptador (capa anticorrupción)");

    @ArchTest
    static final ArchRule LA_CAPA_WEB_NO_TOCA_LA_PERSISTENCIA = noClasses()
            .that().resideInAnyPackage("..infrastructure.web..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure.persistence..");

    @ArchTest
    static final ArchRule LOS_ADAPTADORES_DE_PERSISTENCIA_IMPLEMENTAN_PUERTOS = classes()
            .that().resideInAPackage("..infrastructure.persistence.adapter..")
            .should(implementarUnPuertoDeSalida());

    private static ArchCondition<JavaClass> implementarUnPuertoDeSalida() {
        return new ArchCondition<>("implementar una interfaz de ..port.out..") {
            @Override
            public void check(JavaClass clase, ConditionEvents eventos) {
                boolean implementa = clase.getAllRawInterfaces().stream()
                        .anyMatch(i -> i.getPackageName().contains(".port.out"));
                if (!implementa) {
                    eventos.add(SimpleConditionEvent.violated(
                            clase, clase.getName() + " no implementa ningún puerto de salida"));
                }
            }
        };
    }
}
