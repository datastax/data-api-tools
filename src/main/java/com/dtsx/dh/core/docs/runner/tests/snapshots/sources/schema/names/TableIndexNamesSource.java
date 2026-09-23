package com.dtsx.dh.core.docs.runner.tests.snapshots.sources.schema.names;

import com.datastax.astra.client.databases.Database;
import com.dtsx.dh.core.docs.planner.fixtures.FixtureMetadata;
import com.dtsx.dh.core.docs.planner.meta.snapshot.meta.WithNameAndKeyspace;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;
import lombok.val;

import java.util.List;
import java.util.Optional;

public class TableIndexNamesSource extends NamesSource {
    protected final Optional<String> overrideName;

    public TableIndexNamesSource(String name, WithNameAndKeyspace.TableImpl nameAndKeyspace) {
        super(name, nameAndKeyspace);
        this.overrideName = nameAndKeyspace.name();
    }

    @Override
    public List<String> names(Database db, ClientDriver driver, FixtureMetadata md) {
        val tableName = resolveName("table name", md, driver, overrideName, md::tableName);
        return db.getTable(tableName).listIndexesNames();
    }
}
