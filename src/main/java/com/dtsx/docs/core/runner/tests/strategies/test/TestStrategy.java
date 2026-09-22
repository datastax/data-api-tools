package com.dtsx.docs.core.runner.tests.strategies.test;

import com.dtsx.docs.commands.docs.test.TestCtx;
import com.dtsx.docs.core.planner.TestRoot;
import com.dtsx.docs.core.planner.fixtures.BaseFixturePool;
import com.dtsx.docs.core.planner.meta.BaseMetaYml;
import com.dtsx.docs.core.runner.ExecutionEnvironments;
import com.dtsx.docs.core.runner.tests.results.TestRootResults;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public sealed abstract class TestStrategy<M extends BaseMetaYml> permits CompilesTestStrategy, SnapshotTestStrategy {
    protected final TestCtx ctx;
    protected final M meta;

    public M meta() {
        return meta;
    }

    public abstract BaseFixturePool slicePool(TestRoot testRoot, BaseFixturePool pool);
    public abstract TestRootResults runTestsInRoot(ExternalProgram tsx, TestRoot testRoot, ExecutionEnvironments execEnvs, BaseFixturePool pool);
}
