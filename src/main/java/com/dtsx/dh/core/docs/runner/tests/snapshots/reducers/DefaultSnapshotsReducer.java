package com.dtsx.dh.core.docs.runner.tests.snapshots.reducers;

import com.dtsx.dh.core.docs.runner.tests.snapshots.verifier.Snapshot;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public enum DefaultSnapshotsReducer implements SnapshotsReducer {
    INSTANCE;

    @Override
    public Snapshot reduceSnapshots(Map<Snapshot, Set<Path>> snapshots) throws SnapshotReductionException {
        if (snapshots.size() == 1) {
            return snapshots.keySet().iterator().next();
        }
        throw new SnapshotReductionException();
    }
}
