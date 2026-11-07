package pe.nexosoftware.planilla360.arquitectura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;

/** Reglas de arquitectura del proyecto, verificadas en cada build. */
@AnalyzeClasses(packages = "pe.nexosoftware.planilla360", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquitecturaTests {

    @ArchTest
    static final ArchRule EL_DOMINIO_NO_DEPENDE_DE_SPRING_NI_DE_JPA = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "org.hibernate..")
            .allowEmptyShould(true)
            .because("el dominio de los módulos núcleo es Java puro");

    @ArchTest
    static final ArchRule LOS_CONTROLLERS_NO_USAN_REPOSITORIOS_NI_ENTIDADES = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAnyPackage("..repository..", "..entity..")
            .because("los controllers solo orquestan servicios y hablan en DTO");

    @ArchTest
    static final ArchRule LOS_SERVICIOS_NO_DEPENDEN_DE_LA_CAPA_WEB = noClasses()
            .that().resideInAPackage("..service..")
            .should().dependOnClassesThat().resideInAnyPackage("..controller..")
            .because("la capa de servicio no conoce la capa web");

    @ArchTest
    static final ArchRule LOS_REPOSITORIOS_NO_DEPENDEN_DE_SERVICIOS_NI_CONTROLLERS = noClasses()
            .that().resideInAPackage("..repository..")
            .should().dependOnClassesThat().resideInAnyPackage("..service..", "..controller..");

    @ArchTest
    static final ArchRule LOS_DTO_NO_DEPENDEN_DE_ENTIDADES = noClasses()
            .that().resideInAPackage("..dto..")
            .should().dependOnClassesThat().resideInAnyPackage("..entity..", "..repository..")
            .because("el contrato de la API no debe acoplarse al modelo de persistencia");

    @ArchTest
    static final ArchRule LAS_ENTIDADES_JPA_VIVEN_SOLO_EN_PAQUETES_ENTITY = classes()
            .that().areAnnotatedWith(jakarta.persistence.Entity.class)
            .should().resideInAPackage("..entity..");

    @ArchTest
    static final ArchRule LOS_CONTROLLERS_SE_LLAMAN_CONTROLLER = classes()
            .that().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .should().haveSimpleNameEndingWith("Controller")
            .andShould().resideInAPackage("..controller..");

    @ArchTest
    static final ArchRule SIN_INYECCION_POR_CAMPO = noFields()
            .should().beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
            .because("se inyecta por constructor");

    @ArchTest
    static final ArchRule SIN_JAVA_UTIL_LOGGING = GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

    @ArchTest
    static final ArchRule SIN_SALIDA_ESTANDAR = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

    @ArchTest
    static final ArchRule SIN_EXCEPCIONES_GENERICAS = GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
}
