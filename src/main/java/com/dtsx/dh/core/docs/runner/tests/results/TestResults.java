package com.dtsx.dh.core.docs.runner.tests.results;

import com.dtsx.dh.core.docs.planner.TestRoot;
import com.dtsx.dh.core.docs.planner.fixtures.JSFixture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestResults {
    private final Map<JSFixture, List<TestRootResults>> results = new HashMap<>();

    public void add(JSFixture baseFixture, TestRootResults result) {
        results.computeIfAbsent(baseFixture, _ -> new ArrayList<>()).add(result);
    }

    public Map<JSFixture, List<TestRootResults>> unwrap() {
        return results;
    }

    public int totalTests() {
        return results.values().stream().flatMap(List::stream).mapToInt(TestRootResults::totalTests).sum();
    }

    public int passedTests() {
        return results.values().stream().flatMap(List::stream).mapToInt(TestRootResults::passedTests).sum();
    }

    public int failedTests() {
        return totalTests() - passedTests();
    }

    public boolean allPassed() {
        return failedTests() == 0;
    }

    public boolean hasResults(JSFixture baseFixture, TestRoot testRoot) {
        return results.getOrDefault(baseFixture, List.of()).stream()
            .anyMatch(r -> r.testRoot().equals(testRoot));
    }
}
