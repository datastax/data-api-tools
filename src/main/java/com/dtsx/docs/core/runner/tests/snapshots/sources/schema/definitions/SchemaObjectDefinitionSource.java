package com.dtsx.docs.core.runner.tests.snapshots.sources.schema.definitions;

import com.dtsx.docs.commands.docs.test.TestCtx;
import com.dtsx.docs.core.planner.fixtures.FixtureMetadata;
import com.dtsx.docs.core.planner.meta.snapshot.meta.WithNameAndKeyspace;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.lib.JacksonUtils;
import lombok.val;

import java.util.Optional;

import static com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSourceUtils.mkJsonDeterministic;

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
