package com.dtsx.dh.core.docs.runner.tests.snapshots.sources.schema.definitions;

import com.datastax.astra.client.tables.definition.indexes.TableIndexDescriptor;
import com.dtsx.dh.commands.docs.test.TestCtx;
import com.dtsx.dh.core.docs.planner.fixtures.FixtureMetadata;
import com.dtsx.dh.core.docs.planner.meta.snapshot.meta.TableIndexDefinitionSourceMeta;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.dh.lib.DataAPIUtils;
import com.dtsx.dh.lib.ExternalPrograms.RunResult;
import com.dtsx.dh.lib.JacksonUtils;
import lombok.val;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.dtsx.dh.core.docs.runner.tests.snapshots.sources.SnapshotSourceUtils.mkJsonDeterministic;
import static java.util.stream.Collectors.toMap;

public class TableIndexDefinitionsSource extends SnapshotSource {
    private final List<String> indexes;
    protected final Optional<String> overrideKeyspace;
    protected final Optional<String> overrideName;

    public TableIndexDefinitionsSource(String name, TableIndexDefinitionSourceMeta meta) {
        super(name);
        this.indexes = meta.indexes();
        this.overrideKeyspace = meta.keyspace();
        this.overrideName = meta.name();
    }

    @Override
    public String mkSnapshotImpl(TestCtx ctx, ClientDriver driver, RunResult res, FixtureMetadata md) {
        val tableName = resolveName("table name", md, driver, overrideName, md::tableName);

        val table = DataAPIUtils.getTable(
            ctx.connectionInfo(),
            tableName,
            overrideKeyspace.orElse(md.keyspaceName())
        );

        val existingIndexes = table.listIndexes().stream()
            .collect(toMap(TableIndexDescriptor::getName, i -> i));

        val capturesIndexes = resolveIndexes(md, driver)
            .map((i) -> {
                return Optional.ofNullable(existingIndexes.get(i))
                    .<Object>map(desc -> desc.name("example_index_name"))
                    .orElse("index not found");
            })
            .toList();

        return JacksonUtils.formatJsonPretty(
            mkJsonDeterministic(capturesIndexes)
        );
    }

    private Stream<String> resolveIndexes(FixtureMetadata md, ClientDriver driver) {
        return indexes.stream().map(i -> resolveName(md, driver, i));
    }
}
