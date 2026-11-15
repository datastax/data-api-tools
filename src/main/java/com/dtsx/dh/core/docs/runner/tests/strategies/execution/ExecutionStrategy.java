package com.dtsx.dh.core.docs.runner.tests.strategies.execution;

import com.dtsx.dh.core.docs.planner.fixtures.FixtureMetadata;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.core.docs.runner.tests.results.TestOutcome;
import com.dtsx.dh.lib.CliLogger.MessageUpdater;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ExecutionStrategy {
    protected final ConcurrentHashMap<ClientLanguage, Map<Path, TestOutcome>> outcomes = new ConcurrentHashMap<>();

    public final Map<ClientLanguage, Map<Path, TestOutcome>> execute(Map<ClientLanguage, Set<Path>> testFiles, MessageUpdater msgUpdater, TestFileRunner testFileRunner) {
        executeImpl(testFiles, msgUpdater, testFileRunner);
        return outcomes;
    }

    protected abstract void executeImpl(Map<ClientLanguage, Set<Path>> testFiles, MessageUpdater msgUpdater, TestFileRunner testFileRunner);

    public interface TestFileRunner {
        TestOutcome run(ClientLanguage language, Set<Path> filesForLang, FixtureMetadata md, TestResetter resetter, MessageUpdater msgUpdater);
    }

    public record TestResetter(Runnable beforeEach, Runnable afterEach) {
        public static TestResetter NOOP = new TestResetter(() -> {}, () -> {});
    }
}
