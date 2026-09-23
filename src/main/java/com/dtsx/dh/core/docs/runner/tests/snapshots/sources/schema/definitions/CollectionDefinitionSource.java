package com.dtsx.dh.core.docs.runner.tests.snapshots.sources.schema.definitions;

import com.dtsx.dh.commands.docs.test.TestCtx;
import com.dtsx.dh.core.docs.planner.meta.snapshot.meta.WithNameAndKeyspace;
import com.dtsx.dh.core.docs.runner.Placeholders;
import com.dtsx.dh.lib.DataAPIUtils;

import java.util.Optional;

public class CollectionDefinitionSource extends SchemaObjectDefinitionSource {
    public CollectionDefinitionSource(String name, WithNameAndKeyspace.CollectionImpl nameAndKeyspace) {
        super(name, nameAndKeyspace);
    }

    @Override
    protected Optional<String> extractSchemaObjectName(Placeholders placeholders) {
        return placeholders.collectionName();
    }

    @Override
    protected Object getDefinition(TestCtx ctx, String name, String keyspace) {
        return DataAPIUtils.getCollection(ctx.connectionInfo(), name, keyspace).getDefinition();
    }
}
