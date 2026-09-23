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
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.val;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toMap;

@RequiredArgsConstructor
@AllArgsConstructor
public class SequentialExecutionStrategy extends ExecutionStrategy {
    private final ExternalProgram tsx;
    private final JSFixture testFixture;
    private final BaseFixturePool pool;
    private final TestRoot testRoot;

    private FixtureIndex index = FixtureIndex.ZERO;

    public static BaseFixturePool slicePool(TestRoot ignored, BaseFixturePool baseFixturePool) {
        return baseFixturePool.slice(0, 1);
    }

    @Override
    protected void executeImpl(Map<ClientLanguage, Set<Path>> testFiles, MessageUpdater msgUpdater, TestFileRunner testFileRunner) {
        val md = pool.meta(tsx, index).withVars(testRoot.vars());

        try {
            pool.beforeEach(tsx);
            testFixture.setup(tsx, md);

            testFiles.forEach((lang, files) -> {
                executeLanguage(lang, files, md, msgUpdater, testFileRunner);
            });
        } finally {
            testFixture.teardown(tsx, md);
            pool.afterEach(tsx);
        }
    }

    private void executeLanguage(ClientLanguage language, Set<Path> filesForLang, FixtureMetadata md, MessageUpdater msgUpdater, TestFileRunner testFileRunner) {
        try {
            val resetter = new TestResetter(
                () -> testFixture.beforeEach(tsx, md, language),
                () -> testFixture.afterEach(tsx, md, language)
            );

            val result = testFileRunner.run(language, filesForLang, md, resetter, msgUpdater);
            outcomes.put(language, filesForLang.stream().collect(toMap(p -> p, _ -> result)));
        } catch (Exception ex) {
            CliLogger.exception("Error running snapshot tests for language '%s' in test root '%s'".formatted(language, testRoot.rootName()));
            outcomes.put(language, filesForLang.stream().collect(toMap(p -> p, _ -> new TestOutcome.Errored(ex).alsoLog(testRoot, language))));
        }
    }
}
