package com.dtsx.dh.core.docs.planner.meta.snapshot.meta;

import lombok.NonNull;

import java.util.List;
import java.util.Optional;

public record UdtDefinitionSourceMeta(
    @NonNull List<String> types,
    @NonNull Optional<String> keyspace
) implements WithKeyspace {}
