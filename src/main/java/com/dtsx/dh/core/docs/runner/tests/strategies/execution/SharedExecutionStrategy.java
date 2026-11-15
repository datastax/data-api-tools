package com.dtsx.dh.core.docs.runner.tests.strategies.execution;

import com.dtsx.dh.core.docs.planner.TestRoot;
import com.dtsx.dh.core.docs.planner.fixtures.BaseFixturePool;
import com.dtsx.dh.core.docs.planner.fixtures.BaseFixturePool.FixtureIndex;
import com.dtsx.dh.core.docs.planner.fixtures.FixtureMetadata;
import com.dtsx.dh.core.docs.planner.fixtures.JSFixture;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.core.docs.runner.tests.results.TestOutcome;
import com.dtsx.dh.lib.CliLogger;
import com.dtsx.dh.lib.CliLogger.MessageUpdater;
import com.dtsx.dh.lib.ExecutorUtils;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
public class SharedExecutionStrategy extends ExecutionStrategy {
    private final ExternalProgram tsx;
    private final JSFixture testFixture;
    private final BaseFixturePool pool;
    private final TestRoot testRoot;

    private final FixtureIndex index = FixtureIndex.ZERO;

    public static BaseFixturePool slicePool(TestRoot ignored, BaseFixturePool baseFixturePool) {
        return baseFixturePool.slice(0, 1);
    }

    @Override
    protected void executeImpl(Map<ClientLanguage, Set<Path>> testFiles, MessageUpdater ignored, TestFileRunner testFileRunner) {
        val md = pool.meta(tsx, index).withVars(testRoot.vars());
        
        try (val executor = Executors.newVirtualThreadPerTaskExecutor()) {
            pool.beforeEach(tsx);
            testFixture.setup(tsx, md);
            testFixture.beforeEach(tsx, md, null);

            val futures = ExecutorUtils.emptyFuturesList();

            testFiles.forEach((lang, files) -> {
                futures.add(executor.submit(() -> {
                    executeLanguage(lang, files, md, testFileRunner);
                }));
            });

            ExecutorUtils.awaitAll(futures);
        } finally {
            testFixture.afterEach(tsx, md, null);
            testFixture.teardown(tsx, md);
            pool.afterEach(tsx);
        }
    }

    private void executeLanguage(ClientLanguage language, Set<Path> filesForLang, FixtureMetadata md, TestFileRunner testFileRunner) {
        try {
            val result = testFileRunner.run(language, filesForLang, md, TestResetter.NOOP, (_) -> {});
            outcomes.put(language, filesForLang.stream().collect(toMap(p -> p, _ -> result)));
        } catch (Exception ex) {
            CliLogger.exception("Error running snapshot tests for language '%s' in test root '%s'".formatted(language, testRoot.rootName()));
            outcomes.put(language, filesForLang.stream().collect(toMap(p -> p, _ -> new TestOutcome.Errored(ex).alsoLog(testRoot, language))));
        }
    }
}
