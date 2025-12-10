package predictions.dapp.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(
        packages = "predictions.dapp",
        importOptions = { ImportOption.DoNotIncludeTests.class }
)
@Tag("architecture")
class ArchitectureTest {

    private static final String BASE_PACKAGE = "predictions.dapp";

    private final JavaClasses classes =
            new ClassFileImporter().importPackages(BASE_PACKAGE);

    @Test
    void repositoriesShouldOnlyDependOnModelAndSpringData() {
        ArchRuleDefinition.classes()
                .that().resideInAPackage(BASE_PACKAGE + ".repositories..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        BASE_PACKAGE + ".model..",
                        "org.springframework..",
                        "java.."
                )
                .check(classes);
    }

    @Test
    void servicesMustBeAnnotatedWithService() {

        ArchRuleDefinition.classes()
                .that().resideInAPackage("predictions.dapp.service..")
                .and().haveSimpleNameEndingWith("Service")
                .should().beAnnotatedWith(org.springframework.stereotype.Service.class);
    }

    @Test
    void repositoriesMustBeAnnotatedWithRepository() {
        ArchRuleDefinition.classes()
                .that().resideInAPackage(BASE_PACKAGE + ".repositories..")
                .should().beAnnotatedWith(org.springframework.stereotype.Repository.class)
                .check(classes);
    }

    @Test
    void noCyclicDependenciesBetweenPackages() {
        slices().matching(BASE_PACKAGE + ".(*)..")
                .should().beFreeOfCycles()
                .check(classes);
    }

    @Test
    void controllersShouldNotAccessRepositories() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".controller..")
                .should().accessClassesThat()
                .resideInAPackage(BASE_PACKAGE + ".repositories..")
                .check(classes);
    }

    @Test
    void modelShouldNotDependOnServiceOrController() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".model..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        BASE_PACKAGE + ".service..",
                        BASE_PACKAGE + ".controller.."
                )
                .check(classes);
    }

    // ==================== CONTROLLER LAYER TESTS ====================

    @Test
    void controllersMustBeAnnotatedWithRestController() {
        ArchRuleDefinition.classes()
                .that().resideInAPackage(BASE_PACKAGE + ".controller..")
                .and().haveSimpleNameEndingWith("Controller")
                .and().areNotInterfaces()
                .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .check(classes);
    }

    @Test
    void controllersShouldOnlyResideInControllerPackage() {
        ArchRuleDefinition.classes()
                .that().haveSimpleNameEndingWith("Controller")
                .and().areNotInterfaces()
                .should().resideInAPackage(BASE_PACKAGE + ".controller..")
                .check(classes);
    }



    @Test
    void controllersShouldNotDependOnInfrastructurePackages() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".controller..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "java.sql..",
                        "javax.sql..",
                        "org.hibernate..",
                        "jakarta.persistence.."
                )
                .check(classes);
    }

    @Test
    void controllersShouldHaveRequestMapping() {
        ArchRuleDefinition.classes()
                .that().resideInAPackage(BASE_PACKAGE + ".controller..")
                .and().haveSimpleNameEndingWith("Controller")
                .and().areNotInterfaces()
                .and().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .should().beAnnotatedWith(org.springframework.web.bind.annotation.RequestMapping.class)
                .check(classes);
    }

    // ==================== SERVICE LAYER TESTS ====================

    @Test
    void servicesShouldOnlyResideInServicePackage() {
        ArchRuleDefinition.classes()
                .that().haveSimpleNameEndingWith("Service")
                .and().areNotInterfaces()
                .should().resideInAPackage(BASE_PACKAGE + ".service..")
                .check(classes);
    }


    @Test
    void servicesShouldNotDependOnControllers() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".service..")
                .should().dependOnClassesThat()
                .resideInAPackage(BASE_PACKAGE + ".controller..")
                .check(classes);
    }

    @Test
    void servicesShouldNotDependOnWebAnnotations() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".service..")
                .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .check(classes);
    }

    @Test
    void servicesShouldNotAccessWebSpecificClasses() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".service..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework.web.bind.annotation..",
                        "jakarta.servlet..",
                        "javax.servlet.."
                )
                .check(classes);
    }

    // ==================== REPOSITORY LAYER TESTS ====================

    @Test
    void repositoriesShouldOnlyResideInRepositoriesPackage() {
        ArchRuleDefinition.classes()
                .that().haveSimpleNameEndingWith("Repository")
                .and().areInterfaces()
                .should().resideInAPackage(BASE_PACKAGE + ".repositories..")
                .check(classes);
    }

    @Test
    void repositoriesShouldNotDependOnServicesOrControllers() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".repositories..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        BASE_PACKAGE + ".service..",
                        BASE_PACKAGE + ".controller.."
                )
                .check(classes);
    }

    @Test
    void repositoriesShouldNotDependOnDTOs() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".repositories..")
                .should().dependOnClassesThat()
                .resideInAPackage(BASE_PACKAGE + ".dtos..")
                .check(classes);
    }

    // ==================== MODEL/DOMAIN LAYER TESTS ====================

    @Test
    void modelShouldNotDependOnRepositories() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".model..")
                .should().dependOnClassesThat()
                .resideInAPackage(BASE_PACKAGE + ".repositories..")
                .check(classes);
    }

    @Test
    void modelShouldNotUseSpringStereotypeAnnotations() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".model..")
                .should().beAnnotatedWith(org.springframework.stereotype.Component.class)
                .orShould().beAnnotatedWith(org.springframework.stereotype.Service.class)
                .orShould().beAnnotatedWith(org.springframework.stereotype.Repository.class)
                .orShould().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .check(classes);
    }

    @Test
    void modelShouldOnlyDependOnAllowedPackages() {
        ArchRuleDefinition.classes()
                .that().resideInAPackage(BASE_PACKAGE + ".model..")
                .should().onlyDependOnClassesThat()
                .resideInAnyPackage(
                        BASE_PACKAGE + ".model..",
                        "java..",
                        "jakarta.persistence..",
                        "javax.persistence..",
                        "org.hibernate.annotations..",
                        "com.fasterxml.jackson.annotation.."
                )
                .check(classes);
    }

    @Test
    void entitiesShouldBeAnnotatedWithEntity() {
        ArchRuleDefinition.classes()
                .that().resideInAPackage(BASE_PACKAGE + ".model..")
                .and().areAnnotatedWith("jakarta.persistence.Entity")
                .and().areNotInterfaces()
                .should().haveSimpleNameNotEndingWith("DTO")
                .andShould().haveSimpleNameNotEndingWith("Dto")
                .check(classes);
    }

    @Test
    void modelShouldNotDependOnDTOs() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".model..")
                .should().dependOnClassesThat()
                .resideInAPackage(BASE_PACKAGE + ".dtos..")
                .check(classes);
    }

    // ==================== DTO TESTS ====================


    @Test
    void dtosShouldNotDependOnRepositoriesOrServices() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".dtos..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        BASE_PACKAGE + ".repositories..",
                        BASE_PACKAGE + ".service.."
                )
                .check(classes);
    }

    @Test
    void dtosShouldNotBeAnnotatedWithEntity() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".dtos..")
                .should().beAnnotatedWith("jakarta.persistence.Entity")
                .orShould().beAnnotatedWith("javax.persistence.Entity")
                .check(classes);
    }

    @Test
    void dtosShouldNotDependOnSpringWeb() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".dtos..")
                .should().dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework.web..",
                        "jakarta.servlet..",
                        "javax.servlet.."
                )
                .check(classes);
    }



    @Test
    void configClassesShouldBeAnnotatedWithConfiguration() {
        ArchRuleDefinition.classes()
                .that().resideInAPackage(BASE_PACKAGE + ".config..")
                .and().haveSimpleNameEndingWith("Config")
                .and().areNotInterfaces()
                .should().beAnnotatedWith(org.springframework.context.annotation.Configuration.class)
                .check(classes);
    }

    @Test
    void configShouldNotDependOnControllers() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".config..")
                .should().dependOnClassesThat()
                .resideInAPackage(BASE_PACKAGE + ".controller..")
                .check(classes);
    }


    @Test
    void exceptionsShouldOnlyResideInExceptionsPackage() {
        ArchRuleDefinition.classes()
                .that().haveSimpleNameEndingWith("Exception")
                .should().resideInAPackage(BASE_PACKAGE + ".exceptions..")
                .check(classes);
    }

    @Test
    void exceptionsShouldExtendRuntimeExceptionOrException() {
        ArchRuleDefinition.classes()
                .that().resideInAPackage(BASE_PACKAGE + ".exceptions..")
                .and().haveSimpleNameEndingWith("Exception")
                .should().beAssignableTo(Exception.class)
                .check(classes);
    }

    @Test
    void exceptionsShouldNotDependOnControllers() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".exceptions..")
                .should().dependOnClassesThat()
                .resideInAPackage(BASE_PACKAGE + ".controller..")
                .check(classes);
    }

    @Test
    void exceptionsShouldNotDependOnRepositories() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".exceptions..")
                .should().dependOnClassesThat()
                .resideInAPackage(BASE_PACKAGE + ".repositories..")
                .check(classes);
    }

    // ==================== SECURITY TESTS ====================



    @Test
    void securityShouldNotDependOnControllers() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".security..")
                .should().dependOnClassesThat()
                .resideInAPackage(BASE_PACKAGE + ".controller..")
                .check(classes);
    }

    // ==================== NAMING CONVENTION TESTS ====================

    @Test
    void interfacesShouldNotHavePrefixI() {
        ArchRuleDefinition.noClasses()
                .that().areInterfaces()
                .should().haveSimpleNameStartingWith("I")
                .check(classes);
    }

    @Test
    void implementationClassesShouldNotHaveSuffixImpl() {
        ArchRuleDefinition.noClasses()
                .that().resideInAnyPackage(
                        BASE_PACKAGE + ".service..",
                        BASE_PACKAGE + ".repositories.."
                )
                .should().haveSimpleNameEndingWith("Impl")
                .check(classes);
    }


    // ==================== DEPENDENCY INJECTION TESTS ====================


    @Test
    void controllersShouldUseConstructorInjection() {
        ArchRuleDefinition.constructors()
                .that().areDeclaredInClassesThat().resideInAPackage(BASE_PACKAGE + ".controller..")
                .and().areDeclaredInClassesThat().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .should().notBeAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
                .check(classes);
    }

    @Test
    void servicesShouldUseConstructorInjection() {
        ArchRuleDefinition.constructors()
                .that().areDeclaredInClassesThat().resideInAPackage(BASE_PACKAGE + ".service..")
                .and().areDeclaredInClassesThat().areAnnotatedWith(org.springframework.stereotype.Service.class)
                .should().notBeAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
                .check(classes);
    }


    // ==================== AUDIT INTERCEPTOR TEST ====================

    @Test
    void auditClassesShouldOnlyResideInAuditPackage() {
        ArchRuleDefinition.classes()
                .that().haveSimpleNameContaining("Audit")
                .and().areNotInterfaces()
                .should().resideInAPackage(BASE_PACKAGE + ".audit..")
                .check(classes);
    }

    @Test
    void auditShouldNotDependOnRepositoriesDirectly() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".audit..")
                .should().dependOnClassesThat()
                .resideInAPackage(BASE_PACKAGE + ".repositories..")
                .check(classes);
    }

    // ==================== CACHE TESTS ====================



    @Test
    void cacheShouldNotDependOnControllers() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".cache..")
                .should().dependOnClassesThat()
                .resideInAPackage(BASE_PACKAGE + ".controller..")
                .check(classes);
    }

    @Test
    void cacheShouldNotDependOnRepositories() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".cache..")
                .should().dependOnClassesThat()
                .resideInAPackage(BASE_PACKAGE + ".repositories..")
                .check(classes);
    }



    // ==================== PACKAGE COHESION TESTS ====================

    @Test
    void packagesShouldBeFreeOfCyclicDependencies() {
        slices().matching(BASE_PACKAGE + ".(**)")
                .should().beFreeOfCycles()
                .check(classes);
    }

    // ==================== LAYER ACCESS RULES ====================

    @Test
    void repositoriesShouldExtendSpringDataInterfaces() {
        ArchRuleDefinition.classes()
                .that().resideInAPackage(BASE_PACKAGE + ".repositories..")
                .and().areInterfaces()
                .should().beAssignableTo("org.springframework.data.repository.Repository")
                .orShould().beAssignableTo("org.springframework.data.jpa.repository.JpaRepository")
                .check(classes);
    }

    // ==================== API DOCUMENTATION TESTS ====================

    @Test
    void controllersShouldHaveSwaggerTagAnnotation() {
        ArchRuleDefinition.classes()
                .that().resideInAPackage(BASE_PACKAGE + ".controller..")
                .and().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                .and().areNotInterfaces()
                .should().beAnnotatedWith("io.swagger.v3.oas.annotations.tags.Tag")
                .because("All REST controllers should have Swagger documentation")
                .check(classes);
    }



    @Test
    void jwtUtilShouldOnlyResideInSecurityPackage() {
        ArchRuleDefinition.classes()
                .that().haveSimpleName("JwtUtil")
                .should().resideInAPackage(BASE_PACKAGE + ".security..")
                .check(classes);
    }

}
