package com.dtsx.dh.core.docs.runner.tests.snapshots.sources.output;

import com.dtsx.dh.commands.docs.test.TestCtx;
import com.dtsx.dh.core.docs.planner.fixtures.FixtureMetadata;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.SnapshotSourceUtils;
import com.dtsx.dh.lib.ExternalPrograms.RunResult;

public class OutputCaptureSource extends SnapshotSource {
    public OutputCaptureSource(String name, Void ignored) {
        super(name);
    }

    @Override
    public String mkSnapshotImpl(TestCtx ctx, ClientDriver driver, RunResult res, FixtureMetadata md) {
        return SnapshotSourceUtils.extractOutput(name, res);
    }
}
