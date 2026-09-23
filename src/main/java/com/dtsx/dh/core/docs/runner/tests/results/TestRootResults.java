package com.dtsx.dh.core.docs.runner.tests.results;

import com.dtsx.dh.core.docs.planner.TestRoot;
import com.dtsx.dh.core.common.ClientLanguage;

import java.nio.file.Path;
import java.util.Map;

public record TestRootResults(TestRoot testRoot, Map<ClientLanguage, Map<Path, TestOutcome>> outcomes) {
    public int passedTests() {
        return outcomes.values().stream().mapToInt(langMap ->
            (int) langMap.values().stream().filter(TestOutcome::passed).count()
        ).sum();
    }

    public int totalTests() {
        return outcomes.values().stream().mapToInt(Map::size).sum();
    }

    public boolean allPassed() {
        return passedTests() == totalTests();
    }
}
