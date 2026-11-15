package com.dtsx.dh.core.docs.runner.tests.snapshots.reducers;

import com.dtsx.dh.core.docs.runner.tests.snapshots.verifier.Snapshot;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public interface SnapshotsReducer {
    Snapshot reduceSnapshots(Map<Snapshot, Set<Path>> snapshots) throws SnapshotReductionException;
}
