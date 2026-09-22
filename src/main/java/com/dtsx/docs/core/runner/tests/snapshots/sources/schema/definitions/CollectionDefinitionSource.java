package com.dtsx.docs.core.runner.tests.snapshots.sources.schema.definitions;

import com.dtsx.docs.commands.docs.test.TestCtx;
import com.dtsx.docs.core.planner.meta.snapshot.meta.WithNameAndKeyspace;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.lib.DataAPIUtils;

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
