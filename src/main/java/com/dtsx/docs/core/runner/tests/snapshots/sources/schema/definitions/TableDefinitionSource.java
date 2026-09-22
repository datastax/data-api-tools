package com.dtsx.docs.core.runner.tests.snapshots.sources.schema.definitions;

import com.dtsx.docs.commands.docs.test.TestCtx;
import com.dtsx.docs.core.planner.meta.snapshot.meta.WithNameAndKeyspace;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.lib.DataAPIUtils;

import java.util.Optional;

public class TableDefinitionSource extends SchemaObjectDefinitionSource {
    public TableDefinitionSource(String name, WithNameAndKeyspace.TableImpl nameAndKeyspace) {
        super(name, nameAndKeyspace);
    }

    @Override
    protected Optional<String> extractSchemaObjectName(Placeholders placeholders) {
        return placeholders.tableName();
    }

    @Override
    protected Object getDefinition(TestCtx ctx, String name, String keyspace) {
        try {
            Thread.sleep(2500); // gives a sec for schema changes to propagate, making certain tests more consistent
        } catch (InterruptedException _) {}

        return DataAPIUtils.getTable(ctx.connectionInfo(), name, keyspace).getDefinition();
    }
}
