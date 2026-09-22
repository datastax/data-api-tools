package com.dtsx.docs.core.runner.tests.snapshots.sources.schema.definitions;

import com.datastax.astra.client.collections.definition.documents.Document;
import com.datastax.astra.client.core.commands.Command;
import com.datastax.astra.client.tables.commands.options.ListTypesOptions;
import com.dtsx.docs.commands.docs.test.TestCtx;
import com.dtsx.docs.core.planner.fixtures.FixtureMetadata;
import com.dtsx.docs.core.planner.meta.snapshot.meta.UdtDefinitionSourceMeta;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.docs.lib.DataAPIUtils;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.lib.JacksonUtils;
import lombok.val;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSourceUtils.mkJsonDeterministic;
import static java.util.stream.Collectors.toMap;

public class UdtDefinitionsSource extends SnapshotSource {
    private final List<String> types;
    protected final Optional<String> overrideKeyspace;

    public UdtDefinitionsSource(String name, UdtDefinitionSourceMeta meta) {
        super(name);
        this.types = meta.types();
        this.overrideKeyspace = meta.keyspace();
    }

    record TypeDescriptor(String udtName, Object definition) {}

    @Override
    public String mkSnapshotImpl(TestCtx ctx, ClientDriver driver, RunResult res, FixtureMetadata md) {
        val database = DataAPIUtils.getDatabase(
            ctx.connectionInfo(),
            overrideKeyspace.orElse(md.keyspaceName())
        );

        val listTypesCommand = Command
            .create("listTypes")
            .withOptions(new Document().append("explain", true));

        val existingTypes = database.runCommand(listTypesCommand, (ListTypesOptions) null)
            .getStatusKeyAsList("types", TypeDescriptor.class)
            .stream()
            .collect(toMap(TypeDescriptor::udtName, TypeDescriptor::definition));

        val capturesIndexes = resolveTypes(md, driver)
            .map((t) -> {
                return existingTypes.getOrDefault(t, "type not found");
            })
            .toList();

        return JacksonUtils.formatJsonPretty(
            mkJsonDeterministic(capturesIndexes)
        );
    }

    private Stream<String> resolveTypes(FixtureMetadata md, ClientDriver driver) {
        return types.stream().map(i -> resolveName(md, driver, i));
    }
}
