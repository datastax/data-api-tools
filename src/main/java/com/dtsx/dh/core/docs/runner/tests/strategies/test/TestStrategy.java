package com.dtsx.dh.core.docs.runner.tests.strategies.test;

import com.dtsx.dh.commands.docs.test.TestCtx;
import com.dtsx.dh.core.docs.planner.TestRoot;
import com.dtsx.dh.core.docs.planner.fixtures.BaseFixturePool;
import com.dtsx.dh.core.docs.planner.meta.BaseMetaYml;
import com.dtsx.dh.core.docs.runner.ExecutionEnvironments;
import com.dtsx.dh.core.docs.runner.tests.results.TestRootResults;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
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
