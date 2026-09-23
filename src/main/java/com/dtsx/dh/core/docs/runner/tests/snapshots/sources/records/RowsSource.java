package com.dtsx.dh.core.docs.runner.tests.snapshots.sources.records;

import com.datastax.astra.client.tables.commands.options.TableFindOptions;
import com.datastax.astra.client.tables.definition.rows.Row;
import com.dtsx.dh.commands.docs.test.TestCtx;
import com.dtsx.dh.core.docs.planner.meta.snapshot.meta.RecordSourceMeta.RowsSourceMeta;
import com.dtsx.dh.core.docs.runner.Placeholders;
import com.dtsx.dh.lib.DataAPIUtils;
import lombok.val;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/// Implementation of [RecordSource] that captures rows from a table.
public final class RowsSource extends RecordSource {
    public RowsSource(String name, RowsSourceMeta meta) {
        super(name, meta);
    }

    @Override
    protected Optional<String> extractSchemaObjectName(Placeholders placeholders) {
        return placeholders.tableName();
    }

    @Override
    public Stream<Map<String, Object>> streamRecords(TestCtx ctx, String name, String keyspace) {
        val table = DataAPIUtils.getTable(ctx.connectionInfo(), name, keyspace);

        val options = new TableFindOptions();
        projection.ifPresent(options::projection);

        return table.find(filter.orElse(null), options).stream().map(Row::getColumnMap);
    }
}
