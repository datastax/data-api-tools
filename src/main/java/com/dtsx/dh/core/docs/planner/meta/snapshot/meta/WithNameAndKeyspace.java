package com.dtsx.dh.core.docs.planner.meta.snapshot.meta;

import java.util.Optional;

public interface WithNameAndKeyspace extends WithKeyspace {
    Optional<String> name();

    record CollectionImpl(
        Optional<String> collection,
        Optional<String> keyspace
    ) implements WithNameAndKeyspace {
        @Override
        public Optional<String> name() {
            return collection;
        }
    }

    record TableImpl(
        Optional<String> table,
        Optional<String> keyspace
    ) implements WithNameAndKeyspace {
        @Override
        public Optional<String> name() {
            return table;
        }
    }
}
