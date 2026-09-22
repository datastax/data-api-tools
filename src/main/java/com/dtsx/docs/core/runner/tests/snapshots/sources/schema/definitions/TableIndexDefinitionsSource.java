package com.dtsx.docs.core.runner.tests.snapshots.sources.schema.definitions;

import com.datastax.astra.client.tables.definition.indexes.TableIndexDescriptor;
import com.dtsx.docs.commands.docs.test.TestCtx;
import com.dtsx.docs.core.planner.fixtures.FixtureMetadata;
import com.dtsx.docs.core.planner.meta.snapshot.meta.TableIndexDefinitionSourceMeta;
import com.dtsx.docs.core.runner.RunException;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.docs.lib.DataAPIUtils;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.lib.JacksonUtils;
import lombok.val;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSourceUtils.mkJsonDeterministic;
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
