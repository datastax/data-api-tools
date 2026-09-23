package com.dtsx.dh.core.docs.runner.tests.snapshots.sources;

import com.dtsx.dh.commands.docs.test.TestCtx;
import com.dtsx.dh.core.docs.planner.PlanException;
import com.dtsx.dh.core.docs.planner.fixtures.FixtureMetadata;
import com.dtsx.dh.core.docs.runner.PlaceholderVars;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;
import com.dtsx.dh.core.docs.runner.tests.snapshots.verifier.Snapshot.SnapshotPart;
import com.dtsx.dh.lib.ExternalPrograms.RunResult;
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
