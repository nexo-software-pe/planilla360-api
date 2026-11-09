package pe.nexosoftware.planilla360.arquitectura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/** Reglas hexagonales del módulo planilla: el dominio es Java puro y no conoce las capas externas. */
@AnalyzeClasses(packages = "pe.nexosoftware.planilla360.planilla", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaPlanillaTests {

    @ArchTest
    static final ArchRule EL_DOMINIO_NO_CONOCE_APLICACION_NI_INFRAESTRUCTURA = noClasses()
            .that().resideInAPackage("..planilla.domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..planilla.application..", "..planilla.infrastructure..")
            .allowEmptyShould(true)
            .because("las dependencias apuntan hacia el dominio, nunca desde él");

    @ArchTest
    static final ArchRule EL_DOMINIO_SOLO_USA_JAVA_Y_EL_KERNEL_COMPARTIDO = noClasses()
            .that().resideInAPackage("..planilla.domain..")
            .should().dependOnClassesThat().resideOutsideOfPackages(
                    "..planilla.domain..", "..compartido.dominio..", "java..")
            .because("el dominio de planilla es Java puro: sin Spring, JPA, Jackson ni otros módulos");

    @ArchTest
    static final ArchRule EL_MODELO_Y_LOS_REGIMENES_NO_DEPENDEN_DE_LAS_CALCULADORAS = noClasses()
            .that().resideInAnyPackage("..planilla.domain.model..", "..planilla.domain.regimen..")
            .should().dependOnClassesThat().resideInAPackage("..planilla.domain.service..")
            .because("las calculadoras usan el modelo, no al revés");

    @ArchTest
    static final ArchRule LAS_CALCULADORAS_NO_GUARDAN_ESTADO_MUTABLE = fields()
            .that().areDeclaredInClassesThat().resideInAPackage("..planilla.domain.service..")
            .and().areNotStatic()
            .should().beFinal()
            .allowEmptyShould(true)
            .because("son servicios de dominio sin estado: se pueden compartir entre hilos");

    @ArchTest
    static final ArchRule LA_APLICACION_NO_CONOCE_LA_INFRAESTRUCTURA = noClasses()
            .that().resideInAPackage("..planilla.application..")
            .should().dependOnClassesThat().resideInAPackage("..planilla.infrastructure..")
            .because("la aplicacion depende de puertos (interfaces), no de adaptadores");

    @ArchTest
    static final ArchRule LA_APLICACION_NO_USA_JPA_NI_LA_CAPA_WEB = noClasses()
            .that().resideInAPackage("..planilla.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..", "org.hibernate..", "org.springframework.data..",
                    "org.springframework.web..", "org.springframework.http..")
            .because("los casos de uso no saben como se guarda ni como se expone");

    @ArchTest
    static final ArchRule LA_APLICACION_NO_USA_OTROS_MODULOS_DIRECTAMENTE = noClasses()
            .that().resideInAPackage("..planilla.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "pe.nexosoftware.planilla360.empleado..", "pe.nexosoftware.planilla360.empresa..",
                    "pe.nexosoftware.planilla360.catalogo..", "pe.nexosoftware.planilla360.identidad..",
                    "pe.nexosoftware.planilla360.asistencia..", "pe.nexosoftware.planilla360.vacaciones..")
            .because("otros modulos se consultan por puertos de salida con un adaptador (capa anticorrupcion)");

    @ArchTest
    static final ArchRule LA_CAPA_WEB_NO_TOCA_LA_PERSISTENCIA = noClasses()
            .that().resideInAPackage("..planilla.infrastructure.web..")
            .should().dependOnClassesThat().resideInAPackage("..planilla.infrastructure.persistence..")
            .because("los controllers hablan con casos de uso, nunca con repositorios");

    @ArchTest
    static final ArchRule LOS_ADAPTADORES_IMPLEMENTAN_PUERTOS_DE_SALIDA = classes()
            .that().resideInAnyPackage(
                    "..planilla.infrastructure.persistence.adapter..", "..planilla.infrastructure.adapter..",
                    "..planilla.infrastructure.event..")
            .and().areTopLevelClasses()
            .should(new ArchCondition<JavaClass>("implementar una interfaz de ..port.out..") {
                @Override
                public void check(JavaClass clase, ConditionEvents eventos) {
                    boolean implementa = clase.getAllRawInterfaces().stream()
                            .anyMatch(i -> i.getPackageName().contains(".port.out"));
                    if (!implementa) {
                        eventos.add(SimpleConditionEvent.violated(
                                clase, clase.getName() + " no implementa ningun puerto de salida"));
                    }
                }
            });
}
