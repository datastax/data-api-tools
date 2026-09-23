package com.dtsx.dh.core.docs.runner.tests.reporter;

import com.dtsx.dh.commands.docs.test.TestCtx;
import com.dtsx.dh.core.docs.planner.TestPlan;
import com.dtsx.dh.core.docs.planner.TestRoot;
import com.dtsx.dh.core.docs.planner.fixtures.JSFixture;
import com.dtsx.dh.core.docs.runner.tests.DuplicatesFinder.Duplicate;
import com.dtsx.dh.core.docs.runner.tests.DuplicatesFinder.Duplicates;
import com.dtsx.dh.core.docs.runner.tests.results.TestOutcome;
import com.dtsx.dh.core.docs.runner.tests.results.TestOutcome.*;
import com.dtsx.dh.core.docs.runner.tests.results.TestResults;
import com.dtsx.dh.core.docs.runner.tests.results.TestRootResults;
import com.dtsx.dh.lib.CliLogger;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;
import picocli.CommandLine.Help.Ansi.Style;

import java.util.Comparator;
import java.util.function.Supplier;

import static com.dtsx.dh.core.docs.runner.tests.VerifyMode.DRY_RUN;
import static com.dtsx.dh.lib.ColorUtils.color;
import static com.dtsx.dh.lib.ColorUtils.highlight;
import static java.util.stream.Collectors.joining;

/// The base class for test reporters that format/print test execution results.
///
/// There are two available reporters:
/// - `all_tests` ([AllTestsReporter]) - Shows all test results, passed and failed
/// - `only_failures` ([OnlyFailuresReporter]) - Only shows failed tests, hides passing ones
///
/// There are over 500 different documentation examples across multiple languages; the `only_failures` reporter
/// aides in reducing noise when most tests pass successfully.
///
/// @see AllTestsReporter
/// @see OnlyFailuresReporter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class TestReporter {
    protected final TestCtx ctx;

    /// Prints the header at the start of test execution.
    ///
    /// Example:
    /// ```
    /// Running 123 tests...
    /// ```
    public void printHeader(TestPlan plan) {
        val prefix = (ctx.verifyMode() == DRY_RUN)
            ? "Dry-running"
            : "Running";

        CliLogger.println(false, "@|bold " + prefix + " @!" + plan.totalTests() + "!@ tests...|@");
    }

    /// Prints the heading for a base fixture group.
    ///
    /// Called once per base fixture before running its associated test.
    ///
    /// Example:
    /// ```
    /// 1) basic-collection.js
    /// ```
    public abstract void printBaseFixtureHeading(JSFixture baseFixture, TestResults history);

    /// Prints the results for a single test root.
    ///
    /// Called after each test root completes.
    ///
    /// Example:
    /// ```
    /// <header>
    ///   - ✓ dates (typescript,java)
    ///   - delete-many
    ///     - ✓ <examples_dir>/delete-many/example.ts
    ///     - ✗ <examples_dir>/delete-many/java/src/main/java/Example.java
    /// ```
    public abstract void printTestRootResults(JSFixture baseFixture, TestRootResults result, TestResults history, long duration);

    /// Prints all bailed test roots (tests that were skipped due to bail mode)
    ///
    /// Called when bail mode is triggered and there are remaining untested roots.
    public void printBailedTestRoots(TestPlan plan, TestResults history) {
        plan.forEachPool((pool, testRoots) -> {
            for (val testRoot : testRoots) {
                if (!history.hasResults(pool.fixture(), testRoot)) {
                    printBailedTestRoot(testRoot);
                }
            }
        });
    }

    /// Prints the final summary after all tests complete (total, passed, failed, potentially bailed counts)
    ///
    /// Example:
    /// ```
    /// Test Summary:
    /// - Total tests: 123
    /// - Passed tests: 120
    /// - Failed tests: 3
    /// ```
    public void printSummary(TestPlan plan, TestResults history) {
        val skippedTests = plan.totalTests() - history.totalTests();

        CliLogger.println(true, "\n@|bold Test Summary:|@");
        CliLogger.println(true, "@!-!@ Total tests: " + plan.totalTests());
        CliLogger.println(true, "@!-!@ Passed tests: " + history.passedTests());
        CliLogger.println(true, "@!-!@ Failed tests: " + history.failedTests());

        if (skippedTests > 0) {
            CliLogger.println(true, "@!-!@ Bailed tests: " + skippedTests);
        }
    }

    public void printDuplicates(Supplier<Duplicates> findDuplicates) {
        CliLogger.println(true, "\n@|bold Scanning for duplicate snapshots...|@");
        CliLogger.println(true);

        val duplicates = findDuplicates.get();
        val unwrapped = duplicates.unwrap();

        if (unwrapped.isEmpty()) {
            CliLogger.println(true, "@|green ✓|@ No unnecessarily unique snapshot files found!");
            return;
        }

        CliLogger.println(true, "@|bold,yellow Warning: found|@ @!" + duplicates.totalCount() + "!@ @|bold,yellow unnecessarily unique snapshot file(s):|@");
        CliLogger.println(true);

        for (var i = 0; i < unwrapped.size(); i++) {
            printDuplicate(unwrapped.get(i));
            if (i < unwrapped.size() - 1) {
                CliLogger.println(true);
            }
        }
    }

    private void printDuplicate(Duplicate duplicate) {
        CliLogger.println(true, "  @|red ✗|@ @|faint Root:|@ " + duplicate.testPath());
        CliLogger.println(true, "    @|faint Languages:|@ @|yellow " + String.join(", ", duplicate.languages()) + "|@");
    }

    /// Helper to print a numbered fixture heading.
    ///
    /// Example:
    /// ```
    /// 1) basic-collection.js
    /// ```
    protected void printFixtureHeading(int index, JSFixture baseFixture) {
        CliLogger.println(false, "\n@|bold @!" + index + ") " + baseFixture.fixtureName() + "!@|@");
    }

    /// Helper to print test root results with status indicators.
    ///
    /// If all languages passed, it shows a compact single-line format:
    /// ```
    /// ✓ dates (typescript,python,java)
    /// ```
    ///
    /// If any failed, it shows a per-language breakdown:
    /// ```
    /// ✗ dates
    ///   ✓ ./path/to/example.ts
    ///   ✗ ./path/to/example.py
    ///   ! ./path/to/example.java
    /// ```
    protected void printTestRootResults(TestRootResults results, long duration) {
        val sb = new StringBuilder("  "); // this is not a weird face I promise

        val allResultsTheSame = results.outcomes().values().stream()
            .flatMap(m -> m.values().stream())
            .map(Object::getClass)
            .distinct()
            .count() == 1;

        CliLogger.println(
            false,
            (allResultsTheSame)
                ? mkShorthandReport(results, sb, duration)
                : mkDetailedReport(results, sb, duration)
        );
    }

    private void printBailedTestRoot(TestRoot testRoot) {
        val sb = new StringBuilder("  @|red B|@ ");
        sb.append(testRoot.rootName());

        val languages = testRoot.filesToTest().keySet().stream()
            .map(lang -> lang.name().toLowerCase())
            .collect(joining(","));

        sb.append(Style.faint.on())
            .append(" (")
            .append(languages)
            .append(")")
            .append(Style.faint.off());

        CliLogger.println(false, sb.toString());
    }

    private String mkShorthandReport(TestRootResults results, StringBuilder sb, long duration) {
        val outcome = results.outcomes().values().stream().findFirst().flatMap(r -> r.values().stream().findFirst()).orElseThrow();

        val languages = results.outcomes().entrySet().stream()
            .map((e) -> {
                return Pair.of(
                    e.getKey().name().toLowerCase(), // language name
                    e.getValue().size() // file count for that language
                );
            })
            .sorted(Comparator.<Pair<String, Integer>>comparingInt(Pair::getRight).reversed())
            .map((p) -> {
                return (p.getRight() > 1)
                    ? p.getRight() + "×" + p.getLeft()
                    : p.getLeft();
            })
            .collect(joining(","));

        sb.append(outputPrefix(outcome))
            .append(" ")
            .append(results.testRoot().rootName())
            .append(Style.faint.on())
            .append(" (")
            .append(languages)
            .append(")")
            .append(Style.faint.off());

        appendDuration(sb, duration);
        return sb.toString();
    }

    private String mkDetailedReport(TestRootResults results, StringBuilder sb, long duration) {
        val shorthandSymbol = results.allPassed()
            ? "@|green ✓|@"
            : "@|red ✗|@";

        sb
            .append(shorthandSymbol)
            .append(" ")
            .append(results.testRoot().rootName());

        appendDuration(sb, duration);

        results.outcomes().forEach((_, outcomes) -> {
            outcomes.forEach((path, outcome) -> {
                sb.append(highlight("\n    "))
                    .append(outputPrefix(outcome))
                    .append(" ")
                    .append(color(Style.faint, results.testRoot().displayPath(path)));
            });
        });

        return sb.toString();
    }

    private void appendDuration(StringBuilder sb, long duration) {
        if (duration < 5000) {
            return;
        }

        sb
            .append(" ")
            .append(Style.fg_red.on())
            .append("(")
            .append(duration)
            .append("ms)"
            ).append(Style.fg_red.off());
    }

    private String outputPrefix(TestOutcome outcome) {
        return switch (outcome) {
            case Passed _ -> "@|green ✓|@";
            case DryPassed _ -> "@!?!@";
            case FailedToVerify _, FailedToCompile _ -> "@|red ✗|@";
            case Mismatch _ -> "@|red M|@";
            case Errored _ -> "@|yellow !|@";
        };
    }
}
