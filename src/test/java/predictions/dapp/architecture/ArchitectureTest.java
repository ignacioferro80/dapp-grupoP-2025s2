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

    // -------------------------------
    // 1) Validación de capas
    // -------------------------------
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

    // -------------------------------
    // 2) Validación de anotaciones correctas
    // -------------------------------
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

    // -------------------------------
    // 3) Evitar ciclos entre paquetes
    // -------------------------------
    @Test
    void noCyclicDependenciesBetweenPackages() {
        slices().matching(BASE_PACKAGE + ".(*)..")
                .should().beFreeOfCycles()
                .check(classes);
    }

    // -------------------------------
    // 4) Restricción: controllers NO pueden acceder a repositories
    // -------------------------------
    @Test
    void controllersShouldNotAccessRepositories() {
        ArchRuleDefinition.noClasses()
                .that().resideInAPackage(BASE_PACKAGE + ".controller..")
                .should().accessClassesThat()
                .resideInAPackage(BASE_PACKAGE + ".repositories..")
                .check(classes);
    }

    // -------------------------------
    // 5) Model NO depende de Service ni Controller
    // -------------------------------
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
}
