package com.dtsx.docs.core.runner.tests.strategies.test;

import com.dtsx.docs.commands.docs.test.TestCtx;
import com.dtsx.docs.core.planner.TestRoot;
import com.dtsx.docs.core.planner.fixtures.BaseFixturePool;
import com.dtsx.docs.core.planner.meta.compiles.CompilesTestMeta;
import com.dtsx.docs.core.runner.ExecutionEnvironment;
import com.dtsx.docs.core.runner.ExecutionEnvironments;
import com.dtsx.docs.core.runner.ExecutionEnvironment.TestFileModifiers;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import com.dtsx.docs.core.runner.tests.results.TestOutcome;
import com.dtsx.docs.core.runner.tests.results.TestRootResults;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import lombok.val;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.dtsx.docs.core.runner.tests.VerifyMode.DRY_RUN;
import static com.dtsx.docs.core.runner.tests.VerifyMode.NO_COMPILE_ONLY;

public final class CompilesTestStrategy extends TestStrategy<CompilesTestMeta> {
    public CompilesTestStrategy(TestCtx ctx, CompilesTestMeta m) {
        super(ctx, m);
    }

    @Override
    public BaseFixturePool slicePool(TestRoot testRoot, BaseFixturePool pool) {
        return pool.slice(0, 0);
    }

    @Override
    public TestRootResults runTestsInRoot(ExternalProgram tsx, TestRoot testRoot, ExecutionEnvironments execEnvs, BaseFixturePool pool) {
        val displayMsg = "Compiling @!%d!@ file%s in @!%s!@".formatted(testRoot.numFilesToTest(), (testRoot.numFilesToTest() == 1) ? "" : "s", testRoot.rootName());

        val outcomes = new ConcurrentHashMap<ClientLanguage, Map<Path, TestOutcome>>();

        return CliLogger.loading(displayMsg, (_) -> {
            try (val executor = Executors.newVirtualThreadPerTaskExecutor()) {
                val futures = new ArrayList<Future<?>>();

                testRoot.filesToTest().forEach((lang, paths) -> {
                    val driver = ctx.driver(lang);
                    val execEnv = execEnvs.forLanguage(lang);

                    futures.add(executor.submit(() -> {
                        paths.forEach(path -> {
                            runSingleTest(testRoot, outcomes, path, driver, execEnv);
                        });
                    }));
                });

                for (val future : futures) {
                    future.get();
                }
            }

            return new TestRootResults(testRoot, outcomes);
        });
    }

    private void runSingleTest(TestRoot testRoot, Map<ClientLanguage, Map<Path, TestOutcome>> outcomes, Path path, ClientDriver driver, ExecutionEnvironment execEnv) {
        val lang = driver.language();

        outcomes.computeIfAbsent(lang, _ -> new HashMap<>());

        if (ctx.verifyMode() == DRY_RUN || ctx.verifyMode() == NO_COMPILE_ONLY) {
            outcomes.get(lang).put(path, TestOutcome.DryPassed.INSTANCE);
            return;
        }

        try {
            val outcome = execEnv.withTestFileCopied(driver, path, mkPlaceholders(testRoot), TestFileModifiers.NONE, () -> {
                val res = driver.compileScript(ctx, execEnv);

                return (res.notOk())
                    ? new TestOutcome.FailedToCompile(res.output()).alsoLog(testRoot, lang, res.output())
                    : TestOutcome.Passed.INSTANCE;
            });

            outcomes.get(lang).put(path, outcome);
        } catch (Exception e) {
            outcomes.get(lang).put(path, new TestOutcome.Errored(e).alsoLog(testRoot, lang));
        }
    }

    private Placeholders mkPlaceholders(TestRoot testRoot) {
        return new Placeholders(
            Optional.of("compiles_test_collection"),
            Optional.of("compiles_test_table"),
            "compiles_test_keyspace",
            testRoot.vars()
        );
    }
}
