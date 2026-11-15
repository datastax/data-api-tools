package com.dtsx.dh.core.docs.planner.meta.snapshot.meta;

import lombok.NonNull;

import java.util.Map;
import java.util.Optional;

public interface RecordSourceMeta extends WithNameAndKeyspace {
    Optional<Map<String, Object>> filter();
    Optional<Map<String, Object>> projection();

    record DocumentsSourceMeta(
        @NonNull Optional<Map<String, Object>> filter,
        @NonNull Optional<Map<String, Object>> projection,
        @NonNull Optional<String> collection,
        @NonNull Optional<String> keyspace
    ) implements RecordSourceMeta {
        @Override
        public Optional<String> name() {
            return collection;
        }
    }

    record RowsSourceMeta(
        @NonNull Optional<Map<String, Object>> filter,
        @NonNull Optional<Map<String, Object>> projection,
        @NonNull Optional<String> table,
        @NonNull Optional<String> keyspace
    ) implements RecordSourceMeta {
        @Override
        public Optional<String> name() {
            return table;
        }
    }
}
