package com.dtsx.docs.core.runner.tests.snapshots.sources.schema.names;

import com.datastax.astra.client.databases.Database;
import com.dtsx.docs.commands.docs.test.TestCtx;
import com.dtsx.docs.core.planner.fixtures.FixtureMetadata;
import com.dtsx.docs.core.planner.meta.snapshot.meta.WithKeyspace;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.docs.lib.DataAPIUtils;
import com.dtsx.docs.lib.ExternalPrograms.RunResult;
import com.dtsx.docs.lib.JacksonUtils;
import lombok.val;

import java.util.List;
import java.util.Optional;

public abstract class NamesSource extends SnapshotSource {
    private final Optional<String> keyspace;

    public NamesSource(String name, WithKeyspace keyspace) {
        super(name);
        this.keyspace = keyspace.keyspace();
    }

    public abstract List<String> names(Database db, ClientDriver driver, FixtureMetadata md);

    @Override
    public String mkSnapshotImpl(TestCtx ctx, ClientDriver driver, RunResult res, FixtureMetadata md) {
        val db = DataAPIUtils.getDatabase(
            ctx.connectionInfo(),
            this.keyspace.orElse(md.keyspaceName())
        );

        return JacksonUtils.formatJsonPretty(
            names(db, driver, md).stream()
                .sorted()
                .toList()
        );
    }
}
