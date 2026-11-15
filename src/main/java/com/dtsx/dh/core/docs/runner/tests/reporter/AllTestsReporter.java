package com.dtsx.dh.core.docs.runner.tests.reporter;

import com.dtsx.dh.core.docs.planner.fixtures.JSFixture;
import com.dtsx.dh.commands.docs.test.DocsTestCtx;
import com.dtsx.dh.core.docs.runner.tests.results.TestResults;
import com.dtsx.dh.core.docs.runner.tests.results.TestRootResults;

/// Reporter that shows all test results, both passed and failed.
///
/// Output format:
/// ```
/// 1) basic-collection.js
///   - ✓ dates (typescript,python,java)
///   - ✗ find-many
///     - ✓ example.ts
///     - ✗ example.py
///     - ✓ example.java
/// ```
public class AllTestsReporter extends TestReporter {
    public AllTestsReporter(DocsTestCtx ctx) {
        super(ctx);
    }

    @Override
    public void printBaseFixtureHeading(JSFixture baseFixture, TestResults history) {
        printFixtureHeading(history.unwrap().size(), baseFixture);
    }

    @Override
    public void printTestRootResults(JSFixture baseFixture, TestRootResults results, TestResults history, long duration) {
        printTestRootResults(results, duration);
    }
}