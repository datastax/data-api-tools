package com.dtsx.docs.core.planner;

import com.dtsx.docs.commands.docs.test.TestCtx;
import com.dtsx.docs.core.planner.meta.snapshot.SnapshotTestMetaRep;
import com.dtsx.docs.core.runner.PlaceholderVars;
import com.dtsx.docs.core.runner.RunException;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import com.dtsx.docs.core.runner.tests.strategies.test.TestStrategy;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Set;
import java.util.TreeMap;

/// Represents a test root containing test files for different client languages, represented by a `meta.yml` file.
///
/// Example:
/// ```
/// examples/
///  dates/            <- test root
///    example.sh
///    meta.yml
///  delete-many/
///    with-filter/    <- test root
///      java/src/main/java/Example.java
///      example.ts
///      meta.yml
///    without-filter/ <- test root
///      example.go
///      example.py
///      meta.yml
/// ```
///
/// Test roots may be nested for organizational purposes; it's simply the presence of a `meta.yml` file that defines a test root.
///
/// Each test root contains different client variants of the same example that share the same fixture and snapshot configuration, with test files called `example.<ext>`.
///
/// @see SnapshotTestMetaRep
@Getter
public class TestRoot implements Comparable<TestRoot> {
    private final Path path;
    private final TreeMap<ClientLanguage, Set<Path>> filesToTest;
    private final int numLanguagesToTest;
    private final int numFilesToTest;
    private final TestStrategy<?> testStrategy;
    private final String rootName;
    private final PlaceholderVars vars;

    public TestRoot(TestCtx ctx, Path path, TreeMap<ClientLanguage, Set<Path>> filesToTest, TestStrategy<?> testStrategy) {
        this.path = path;
        this.filesToTest = filesToTest;
        this.testStrategy = testStrategy;
        this.rootName = ctx.examplesFolder().relativize(path).toString();
        this.numLanguagesToTest = filesToTest.size();
        this.numFilesToTest = filesToTest.values().stream().mapToInt(Set::size).sum();
        this.vars = testStrategy.meta().vars();
    }

    /// Returns the relative path of the example file for the specified file to test from this test root.
    ///
    /// Example:
    /// ```
    /// dates/
    ///   example.ts                       -> "example.ts"
    ///   java/src/main/java/Example.java  -> "java/src/main/java/Example.java"
    /// ```
    ///
    /// @param fileToTest the path to the example file to test
    /// @return the relative path from this test root to the example file
    public String displayPath(Path fileToTest) {
        if (!fileToTest.startsWith(this.path)) {
            throw new RunException("File to test is not within the test root path"); // sanity check; should never be thrown
        }
        return rootName + "/" + path.relativize(fileToTest);
    }

    @Override
    public int compareTo(@NotNull TestRoot o) {
        return rootName.compareTo(o.rootName);
    }
}
