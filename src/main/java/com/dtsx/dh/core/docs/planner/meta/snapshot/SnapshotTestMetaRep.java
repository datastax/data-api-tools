package com.dtsx.dh.core.docs.planner.meta.snapshot;

import com.dtsx.dh.core.docs.planner.meta.BaseMetaYml.BaseMetaYmlRep;
import lombok.NonNull;

import java.util.Map;
import java.util.Optional;

import static com.dtsx.dh.core.docs.planner.meta.BaseMetaYml.BaseMetaYmlRep.TestType.SNAPSHOT;

public record SnapshotTestMetaRep(
    @NonNull String $schema,
    @NonNull TestBlock test,
    @NonNull Optional<FixturesConfig> fixtures,
    @NonNull SnapshotsConfig snapshots,
    @NonNull Optional<ExecutionMode> execution
) implements BaseMetaYmlRep {
    public record FixturesConfig(
        @NonNull Optional<String> base
    ) {}

    public record SnapshotsConfig(
        @NonNull Optional<Object> share,
        @NonNull Map<String, Map<String, Object>> sources
    ) {}

    @Override
    public TestType expectTestType() {
        return SNAPSHOT;
    }
}
