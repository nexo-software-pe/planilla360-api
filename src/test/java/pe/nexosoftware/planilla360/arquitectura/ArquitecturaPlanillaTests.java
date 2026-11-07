package pe.nexosoftware.planilla360.arquitectura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

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
}
