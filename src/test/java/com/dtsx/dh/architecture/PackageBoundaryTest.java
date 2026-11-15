package com.dtsx.dh.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The `docs` and `clients` missions are mechanically independent: neither's `core` package may
/// depend on the other's.
public class PackageBoundaryTest {
    private static final JavaClasses CLASSES = new ClassFileImporter().importPackages("com.dtsx.dh");

    /// An importer that reads no classes makes every rule below pass vacuously, which an ArchUnit
    /// too old for this project's bytecode will silently do.
    @Test
    void importsClassesFromBothMissions() {
        assertTrue(hasClassesIn(".core.docs."), "no core.docs classes were imported");
        assertTrue(hasClassesIn(".core.clients."), "no core.clients classes were imported");
    }

    private static boolean hasClassesIn(String packageFragment) {
        return CLASSES.stream().anyMatch((c) -> c.getPackageName().contains(packageFragment));
    }

    @Test
    void clientsDoesNotDependOnDocs() {
        noClasses().that().resideInAPackage("..core.clients..")
            .should().dependOnClassesThat().resideInAPackage("..core.docs..")
            .check(CLASSES);
    }

    @Test
    void docsDoesNotDependOnClients() {
        noClasses().that().resideInAPackage("..core.docs..")
            .should().dependOnClassesThat().resideInAPackage("..core.clients..")
            .check(CLASSES);
    }
}
