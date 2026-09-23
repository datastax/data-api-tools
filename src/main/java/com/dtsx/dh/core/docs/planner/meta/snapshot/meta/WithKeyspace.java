package com.dtsx.dh.core.docs.planner.meta.snapshot.meta;

import java.util.Optional;

public interface WithKeyspace {
    Optional<String> keyspace();

    record Impl(
        Optional<String> keyspace
    ) implements WithKeyspace {}
}
