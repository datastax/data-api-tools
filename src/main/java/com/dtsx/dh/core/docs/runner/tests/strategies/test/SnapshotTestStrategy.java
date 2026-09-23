package com.dtsx.dh.core.docs.runner.tests.strategies.test;

import com.dtsx.dh.commands.docs.test.TestCtx;
import com.dtsx.dh.core.docs.planner.TestRoot;
import com.dtsx.dh.core.docs.planner.fixtures.BaseFixturePool;
import com.dtsx.dh.core.docs.planner.fixtures.FixtureMetadata;
import com.dtsx.dh.core.docs.planner.meta.snapshot.SnapshotTestMeta;
import com.dtsx.dh.core.docs.runner.ExecutionEnvironments;
import com.dtsx.dh.core.docs.runner.ExecutionEnvironment.TestFileModifiers;
import com.dtsx.dh.core.docs.runner.PlaceholderResolver;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.core.docs.runner.tests.results.TestOutcome;
import com.dtsx.dh.core.docs.runner.tests.results.TestRootResults;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.output.OutputJsonifySource;
import com.dtsx.dh.core.docs.runner.tests.snapshots.verifier.SnapshotVerifier;
import com.dtsx.dh.core.docs.runner.tests.strategies.execution.ExecutionStrategy;
import com.dtsx.dh.core.docs.runner.tests.strategies.execution.ExecutionStrategy.TestResetter;
import com.dtsx.dh.lib.CliLogger;
import com.dtsx.dh.lib.CliLogger.MessageUpdater;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import lombok.Getter;
import lombok.val;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

@Getter
public final class SnapshotTestStrategy extends TestStrategy<SnapshotTestMeta> {
    public SnapshotTestStrategy(TestCtx ctx, SnapshotTestMeta meta) {
        super(ctx, meta);
    }

    @Override
    public BaseFixturePool slicePool(TestRoot testRoot, BaseFixturePool pool) {
        return meta.executionMode().slicePool(testRoot, pool);
    }

    @Override
    public TestRootResults runTestsInRoot(ExternalProgram tsx, TestRoot testRoot, ExecutionEnvironments execEnvs, BaseFixturePool pool) {
        return new Runner(tsx, testRoot, execEnvs, pool).runTestsInRoot();
    }

    public class Runner {
        private final TestRoot testRoot;
        private final ExecutionEnvironments execEnvs;
        private final BaseFixturePool pool;
        private final SnapshotVerifier verifier;
        private final ExecutionStrategy executionStrategy;

        public Runner(ExternalProgram tsx, TestRoot testRoot, ExecutionEnvironments execEnvs, BaseFixturePool pool) {
            this.testRoot = testRoot;
            this.execEnvs = execEnvs;
            this.pool = pool;
            this.verifier = new SnapshotVerifier(ctx, meta.snapshotSources(), meta.shareConfig());
            this.executionStrategy = meta.executionMode().createStrategy(tsx, meta.testFixture(), pool, testRoot);
        }

        public TestRootResults runTestsInRoot() {
            return CliLogger.loading(mkLoadingMsg(), (msgUpdater) -> {
                val outcomes = executionStrategy.execute(
                    testRoot.filesToTest(),
                    msgUpdater,
                    this::run
                );
                return new TestRootResults(testRoot, outcomes);
            });
        }

        private String mkLoadingMsg() {
            return "Verifying @!%d!@ file%s in @!%s!@%s".formatted(
                testRoot.numFilesToTest(),
                (testRoot.numFilesToTest() > 1)
                    ? "s"
                    : "",
                testRoot.rootName(),
                (pool.size() > 1)
                    ? " @|faint (across %s instances)|@".formatted(pool.size())
                    : ""
            );
        }

        private TestOutcome run(ClientLanguage language, Set<Path> filesForLang, FixtureMetadata md, TestResetter resetter, MessageUpdater msgUpdater) {
            val driver = ctx.driver(language);
            val execEnv = execEnvs.forLanguage(language);

            val envVars = PlaceholderResolver.mkEnvVars(ctx, md, Optional.of(language));

            return verifier.verify(driver, testRoot, md, filesForLang, resetter, (path) -> {
                msgUpdater.update(_ -> "Verifying @!%s!@".formatted(testRoot.displayPath(path)));

                val testFileModifiers = (meta.snapshotSources().stream().anyMatch(OutputJsonifySource.class::isInstance))
                    ? TestFileModifiers.JSONIFY_OUTPUT
                    : TestFileModifiers.NONE;

                return execEnv.withTestFileCopied(driver, path, md, testFileModifiers, () -> {
                    return driver.executeScript(ctx, execEnv, envVars);
                });
            });
        }
    }
}
