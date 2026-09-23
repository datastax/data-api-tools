package com.dtsx.dh.core.docs.runner.tests.strategies.execution;

import com.dtsx.dh.core.docs.planner.TestRoot;
import com.dtsx.dh.core.docs.planner.fixtures.BaseFixturePool;
import com.dtsx.dh.core.docs.planner.fixtures.JSFixture;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.lib.CliLogger.MessageUpdater;
import com.dtsx.dh.lib.ExecutorUtils;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
public class IsolatedExecutionStrategy extends ExecutionStrategy {
    private final ExternalProgram tsx;
    private final JSFixture testFixture;
    private final BaseFixturePool pool;
    private final TestRoot testRoot;

    public static BaseFixturePool slicePool(TestRoot testRoot, BaseFixturePool baseFixturePool) {
        return baseFixturePool.slice(0, testRoot.numLanguagesToTest());
    }

    @Override
    protected void executeImpl(Map<ClientLanguage, Set<Path>> testFiles, MessageUpdater ignored, TestFileRunner testFileRunner) {
        try (val executor = Executors.newVirtualThreadPerTaskExecutor()) {
            val futures = ExecutorUtils.emptyFuturesList();

            testFiles.forEach((lang, files) -> {
                futures.add(executor.submit(() -> {
                    executeLanguageWithIsolation(lang, files, testFileRunner);
                }));
            });

            ExecutorUtils.awaitAll(futures);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    private void executeLanguageWithIsolation(ClientLanguage language, Set<Path> filesForLang, TestFileRunner testFileRunner) {
        val index = pool.acquire();

        try {
            val slice = pool.slice(index.unwrap(), index.unwrap() + 1);
            val strategy = new SequentialExecutionStrategy(tsx, testFixture, slice, testRoot, index);

            outcomes.putAll(
                strategy.execute(Map.of(language, filesForLang), (_) -> {}, testFileRunner)
            );
        } finally {
            pool.release(index);
        }
    }
}
