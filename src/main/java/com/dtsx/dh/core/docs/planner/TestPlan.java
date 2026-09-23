package com.dtsx.dh.core.docs.planner;

import com.dtsx.dh.commands.docs.test.TestCtx;
import com.dtsx.dh.core.docs.planner.fixtures.BaseFixturePool;
import com.dtsx.dh.core.docs.planner.fixtures.JSFixture;
import com.dtsx.dh.core.docs.planner.meta.snapshot.ExecutionMode;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.core.docs.runner.tests.strategies.test.SnapshotTestStrategy;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.function.BiConsumer;

@RequiredArgsConstructor
public class TestPlan {
    private final TestCtx ctx;
    private final SortedMap<BaseFixturePool, SortedSet<TestRoot>> plan;
    private final Set<ClientLanguage> usedLanguages;

    public void forEachPool(BiConsumer<BaseFixturePool, SortedSet<TestRoot>> consumer) {
        plan.forEach(consumer);
    }

    public List<ClientDriver> usedDrivers() {
        return ctx.drivers().stream()
            .filter(d -> usedLanguages.contains(d.language()))
            .toList();
    }

    public int totalTests() {
        return plan.values().stream().flatMap(Set::stream).mapToInt(root -> root.filesToTest().values().stream().mapToInt(Set::size).sum()).sum();
    }

    public static class Builder {
        private final Map<JSFixture, PoolInfo> poolInfos = new HashMap<>();
        private final Set<ClientLanguage> usedLanguages = new HashSet<>();

        private static class PoolInfo {
            final List<TestRoot> testRoots = new ArrayList<>();
            int maxNeededFixtures = 0;
        }

        public void addRoot(Pair<JSFixture, TestRoot> pair) {
            val baseFixture = pair.getLeft();
            val testRoot = pair.getRight();

            val info = poolInfos.computeIfAbsent(baseFixture, _ -> new PoolInfo());
            info.testRoots.add(testRoot);

            usedLanguages.addAll(testRoot.filesToTest().keySet());
            updateMaxNeededFixtures(info, testRoot);
        }

        public TestPlan build(TestCtx ctx) {
            val plan = new TreeMap<BaseFixturePool, SortedSet<TestRoot>>();

            for (val entry : poolInfos.entrySet()) {
                val pool = mkPool(entry.getKey(), entry.getValue(), ctx.maxFixtureInstances());
                plan.put(pool, new TreeSet<>(entry.getValue().testRoots));
            }

            return new TestPlan(ctx, plan, usedLanguages);
        }

        private void updateMaxNeededFixtures(PoolInfo info, TestRoot testRoot) {
            if (testRoot.testStrategy() instanceof SnapshotTestStrategy s) {
                if (s.meta().executionMode() == ExecutionMode.ISOLATED) {
                    info.maxNeededFixtures = Math.max(info.maxNeededFixtures, testRoot.numLanguagesToTest());
                }
            }
        }
        
        private BaseFixturePool mkPool(JSFixture baseFixture, PoolInfo info, int maxFixtureInstances) {
            val poolSize = Math.min(info.maxNeededFixtures, maxFixtureInstances);
            return new BaseFixturePool(baseFixture, Math.max(poolSize, 1));
        }
    }
}
