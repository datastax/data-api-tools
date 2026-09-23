package com.dtsx.dh.core.docs.runner.tests.reporter;

import com.dtsx.dh.core.docs.planner.fixtures.JSFixture;
import com.dtsx.dh.commands.docs.test.TestCtx;
import com.dtsx.dh.core.docs.runner.tests.results.TestResults;
import com.dtsx.dh.core.docs.runner.tests.results.TestRootResults;
import org.jetbrains.annotations.Nullable;

/// Reporter that only shows failed tests, hiding passing ones for cleaner output.
///
/// Fixture headings are only printed if they contain at least one failure.
/// This keeps output minimal when most tests pass.
///
/// Output format (only failures shown):
/// ```
/// 1) ✗ basic-collection.js
///   - find-many
///     - ✓ example.ts
///     - ✗ example.py
///     - ✓ example.java
/// ```
public class OnlyFailuresReporter extends TestReporter {
    private int printedFixtures = 0;
    private @Nullable Runnable printBaseFixtureName;

    public OnlyFailuresReporter(TestCtx ctx) {
        super(ctx);
    }

    @Override
    public void printBaseFixtureHeading(JSFixture baseFixture, TestResults history) {
        // defer printing until we know there's a failure in this fixture group
        printBaseFixtureName = () -> printFixtureHeading(++printedFixtures, baseFixture);
    }

    @Override
    public void printTestRootResults(JSFixture baseFixture, TestRootResults results, TestResults history, long duration) {
        if (results.allPassed()) {
            return;
        }

        // print the fixture heading now that we know there's a failure
        if (printBaseFixtureName != null) {
            printBaseFixtureName.run();
            printBaseFixtureName = null;
        }

        printTestRootResults(results, duration);
    }
}
