package com.dtsx.dh.core.docs.runner.tests.snapshots.sources.output;

import com.dtsx.dh.commands.docs.test.TestCtx;
import com.dtsx.dh.core.docs.planner.fixtures.FixtureMetadata;
import com.dtsx.dh.core.docs.planner.meta.snapshot.meta.OutputMatchesSourceMeta;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.SnapshotSourceUtils;
import com.dtsx.dh.lib.ExternalPrograms.RunResult;
import lombok.val;

import java.util.regex.Pattern;

public class OutputMatchesSource extends SnapshotSource {
    protected final Pattern regex;

    public OutputMatchesSource(String name, OutputMatchesSourceMeta meta) {
        super(name);
        this.regex = meta.regex();
    }

    @Override
    public String mkSnapshotImpl(TestCtx ctx, ClientDriver driver, RunResult res, FixtureMetadata md) {
        val output = SnapshotSourceUtils.extractOutput(name, res);

        if (regex.matcher(output).matches()) {
            return "Matches '" + regex.pattern() + "'";
        } else {
            return "Failed to match '" + regex.pattern() + "':\n\n" + output;
        }
    }
}
