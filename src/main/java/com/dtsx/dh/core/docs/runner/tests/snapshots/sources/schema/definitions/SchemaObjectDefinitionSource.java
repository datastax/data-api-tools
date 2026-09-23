package com.dtsx.dh.core.docs.runner.tests.snapshots.sources.schema.definitions;

import com.dtsx.dh.commands.docs.test.TestCtx;
import com.dtsx.dh.core.docs.planner.fixtures.FixtureMetadata;
import com.dtsx.dh.core.docs.planner.meta.snapshot.meta.WithNameAndKeyspace;
import com.dtsx.dh.core.docs.runner.Placeholders;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.dh.lib.ExternalPrograms.RunResult;
import com.dtsx.dh.lib.JacksonUtils;
import lombok.val;

import java.util.Optional;

import static com.dtsx.dh.core.docs.runner.tests.snapshots.sources.SnapshotSourceUtils.mkJsonDeterministic;

public abstract class SchemaObjectDefinitionSource extends SnapshotSource {
    protected final Optional<String> overrideName;
    protected final Optional<String> overrideKeyspace;

    public SchemaObjectDefinitionSource(String name, WithNameAndKeyspace nameAndKeyspace) {
        super(name);
        this.overrideName = nameAndKeyspace.name();
        this.overrideKeyspace = nameAndKeyspace.keyspace();
    }

    protected abstract Optional<String> extractSchemaObjectName(Placeholders placeholders);
    protected abstract Object getDefinition(TestCtx ctx, String name, String keyspace);

    @Override
    public String mkSnapshotImpl(TestCtx ctx, ClientDriver driver, RunResult res, FixtureMetadata md) {
        val schemaObjName = resolveName("schema object name", md, driver, overrideName, () -> extractSchemaObjectName(md));
        val schemaObjKeyspace = resolveName("keyspace", md, driver, overrideKeyspace, () -> Optional.of(md.keyspaceName()));

        return JacksonUtils.formatJsonPretty(
            mkJsonDeterministic(getDefinition(ctx, schemaObjName, schemaObjKeyspace))
        );
    }
}
