package com.dtsx.dh.core.docs.runner.tests.snapshots.sources.schema.names;

import com.datastax.astra.client.databases.Database;
import com.dtsx.dh.commands.docs.test.DocsTestCtx;
import com.dtsx.dh.core.docs.planner.fixtures.FixtureMetadata;
import com.dtsx.dh.core.docs.planner.meta.snapshot.meta.WithKeyspace;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.dh.lib.DataAPIUtils;
import com.dtsx.dh.lib.ExternalPrograms.RunResult;
import com.dtsx.dh.lib.JacksonUtils;
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
    public String mkSnapshotImpl(DocsTestCtx ctx, ClientDriver driver, RunResult res, FixtureMetadata md) {
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
