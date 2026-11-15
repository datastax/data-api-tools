package com.dtsx.dh.core.docs.runner.tests.snapshots.sources.schema.names;

import com.datastax.astra.client.databases.Database;
import com.dtsx.dh.core.docs.planner.fixtures.FixtureMetadata;
import com.dtsx.dh.core.docs.planner.meta.snapshot.meta.WithKeyspace;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;

import java.util.List;

public class CollectionNamesSource extends NamesSource {
    public CollectionNamesSource(String name, WithKeyspace.Impl keyspace) {
        super(name, keyspace);
    }

    @Override
    public List<String> names(Database db, ClientDriver driver, FixtureMetadata md) {
        return db.listCollectionNames();
    }
}
