package com.dtsx.docs.core.runner.tests.snapshots.sources.output;

import com.dtsx.docs.commands.docs.test.TestCtx;
import com.dtsx.docs.core.planner.fixtures.FixtureMetadata;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSourceUtils;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;

public class OutputCaptureSource extends SnapshotSource {
    public OutputCaptureSource(String name, Void ignored) {
        super(name);
    }

    @Override
    public String mkSnapshotImpl(TestCtx ctx, ClientDriver driver, RunResult res, FixtureMetadata md) {
        return SnapshotSourceUtils.extractOutput(name, res);
    }
}
