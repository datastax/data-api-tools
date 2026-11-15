package com.dtsx.dh.core.docs.planner.meta.snapshot;

import com.dtsx.dh.core.docs.planner.TestRoot;
import com.dtsx.dh.core.docs.planner.fixtures.BaseFixturePool;
import com.dtsx.dh.core.docs.planner.fixtures.JSFixture;
import com.dtsx.dh.core.docs.runner.tests.strategies.execution.ExecutionStrategy;
import com.dtsx.dh.core.docs.runner.tests.strategies.execution.IsolatedExecutionStrategy;
import com.dtsx.dh.core.docs.runner.tests.strategies.execution.SequentialExecutionStrategy;
import com.dtsx.dh.core.docs.runner.tests.strategies.execution.SharedExecutionStrategy;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import lombok.RequiredArgsConstructor;
import org.lambda.functions.Function4;

import java.util.function.BiFunction;

@RequiredArgsConstructor
public enum ExecutionMode {
    SEQUENTIAL(SequentialExecutionStrategy::new, SequentialExecutionStrategy::slicePool),
    SHARED(SharedExecutionStrategy::new, SharedExecutionStrategy::slicePool),
    ISOLATED(IsolatedExecutionStrategy::new, IsolatedExecutionStrategy::slicePool);

    private final Function4<ExternalProgram, JSFixture, BaseFixturePool, TestRoot, ExecutionStrategy> constructor;
    private final BiFunction<TestRoot, BaseFixturePool, BaseFixturePool> slicePool;

    public BaseFixturePool slicePool(TestRoot testRoot, BaseFixturePool pool) {
        return slicePool.apply(testRoot, pool);
    }

    public ExecutionStrategy createStrategy(ExternalProgram tsx, JSFixture testFixture, BaseFixturePool pool, TestRoot testRoot) {
        return constructor.call(tsx, testFixture, pool, testRoot);
    }
}
