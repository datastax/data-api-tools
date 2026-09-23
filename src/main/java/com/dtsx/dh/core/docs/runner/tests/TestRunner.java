package com.dtsx.dh.core.docs.runner.tests;

import com.dtsx.dh.commands.docs.test.DocsTestCtx;
import com.dtsx.dh.core.docs.planner.TestPlan;
import com.dtsx.dh.core.docs.planner.fixtures.JSFixture;
import com.dtsx.dh.core.docs.runner.ExecutionEnvironment;
import com.dtsx.dh.core.docs.runner.tests.results.TestResults;
import com.dtsx.dh.lib.CliLogger;
import com.dtsx.dh.lib.ExternalPrograms;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import lombok.val;

public class TestRunner {
    private final DocsTestCtx ctx;
    private final ExternalProgram tsx;
    private final TestPlan plan;

    private TestRunner(DocsTestCtx ctx, TestPlan plan) {
        this.ctx = ctx;
        this.tsx = ExternalPrograms.tsx(ctx);
        this.plan = plan;
    }

    public static boolean runTests(DocsTestCtx ctx, TestPlan plan) {
        val ok = new TestRunner(ctx, plan).runAllTests();

        if (!ok) {
            return false;
        }

        ctx.reporter().printDuplicates(
            () -> DuplicatesFinder.findDuplicates(ctx.examplesFolder())
        );

        return true;
    }

    // Don't love using exceptions for control flow, but eh, keeps it simple here
    private static class BailException extends RuntimeException {}

    private boolean runAllTests() {
        val execEnvs = ExecutionEnvironment.setup(ctx, plan.usedDrivers(), () -> {
            JSFixture.installDependencies(ctx);
        });

        val history = new TestResults();

        ctx.reporter().printHeader(plan);

        try {
            plan.forEachPool((pool, testRoots) -> {
                ctx.reporter().printBaseFixtureHeading(pool.fixture(), history);
                
                try {
                    pool.setup(tsx);

                    for (val testRoot : testRoots) {
                        try {
                            val slicedPool = testRoot.testStrategy().slicePool(testRoot, pool);

                            val startTime = System.currentTimeMillis();
                            val result = testRoot.testStrategy().runTestsInRoot(tsx, testRoot, execEnvs, slicedPool);
                            val duration = System.currentTimeMillis() - startTime;

                            ctx.reporter().printTestRootResults(pool.fixture(), result, history, duration);
                            history.add(pool.fixture(), result);
                            
                            if (ctx.bail() && !result.allPassed()) {
                                throw new BailException();
                            }
                        } catch (Exception e) {
                            CliLogger.exception("Error running tests in test root '" + testRoot.rootName() + "' (" + e.getClass().getSimpleName() + ")");
                            throw e;
                        }
                    }
                } finally {
                    pool.teardown(tsx);
                }
            });

            return history.allPassed();
        } catch (BailException e) {
            ctx.reporter().printBailedTestRoots(plan, history);
            return false;
        } finally {
            ctx.reporter().printSummary(plan, history);
        }
    }
}
