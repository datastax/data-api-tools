package com.dtsx.dh.core.docs.planner.meta.snapshot.meta;

import lombok.NonNull;

import java.util.List;
import java.util.Optional;

public record TableIndexDefinitionSourceMeta(
    @NonNull List<String> indexes,
    @NonNull Optional<String> table,
    @NonNull Optional<String> keyspace
) implements WithNameAndKeyspace {
    @Override
    public Optional<String> name() {
        return table;
    }
}
