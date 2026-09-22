package com.dtsx.docs.core.runner.tests.snapshots.sources;

import com.dtsx.docs.commands.docs.test.TestCtx;
import com.dtsx.docs.core.planner.PlanException;
import com.dtsx.docs.core.planner.fixtures.FixtureMetadata;
import com.dtsx.docs.core.runner.PlaceholderVars;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.tests.snapshots.verifier.Snapshot.SnapshotPart;
import com.dtsx.docs.core.runner.tests.snapshots.sources.output.OutputCaptureSource;
import com.dtsx.docs.core.runner.tests.snapshots.sources.records.RecordSource;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.function.Supplier;

@RequiredArgsConstructor
public abstract class SnapshotSource implements Comparable<SnapshotSource> {
    protected final String name;

    public SnapshotPart mkSnapshot(TestCtx ctx, ClientDriver driver, RunResult res, FixtureMetadata md) {
        return new SnapshotPart(name, mkSnapshotImpl(ctx, driver, res, md));
    }

    protected abstract String mkSnapshotImpl(TestCtx ctx, ClientDriver driver, RunResult res, FixtureMetadata md);

    @Override
    public int compareTo(SnapshotSource other) {
        return this.name.compareTo(other.name); // ensures snapshot source ordering is always deterministic
    }

    protected String resolveName(String thing, FixtureMetadata md, ClientDriver driver, Optional<String> name, Supplier<Optional<String>> defaultSupplier) {
        return name.map(n -> resolveName(md, driver, n)).or(defaultSupplier)
            .orElseThrow(() -> new PlanException("Could not determine " + thing + " from fixture metadata or override for source: " + name));
    }

    protected String resolveName(FixtureMetadata md, ClientDriver driver, String name) {
        return PlaceholderVars.resolveVariables(md.vars(), driver.language(), md.index(), name);
    }
}
