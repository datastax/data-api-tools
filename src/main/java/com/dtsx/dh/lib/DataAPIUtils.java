package com.dtsx.dh.lib;

import com.datastax.astra.client.DataAPIClient;
import com.datastax.astra.client.admin.DatabaseAdmin;
import com.datastax.astra.client.collections.Collection;
import com.datastax.astra.client.collections.definition.documents.Document;
import com.datastax.astra.client.core.options.DataAPIClientOptions;
import com.datastax.astra.client.databases.Database;
import com.datastax.astra.client.tables.Table;
import com.datastax.astra.client.tables.definition.rows.Row;
import com.dtsx.dh.config.ConnectionInfo;

public class DataAPIUtils {
    public static Collection<Document> getCollection(ConnectionInfo info, String name, String keyspace) {
        return mkDb(info, keyspace).getCollection(name);
    }

    public static Table<Row> getTable(ConnectionInfo info, String name, String keyspace) {
        return mkDb(info, keyspace).getTable(name);
    }

    public static Database getDatabase(ConnectionInfo info, String keyspace) {
        return mkDb(info, keyspace);
    }

    /// Admin operations (listing/creating/dropping keyspaces) aren't scoped to a keyspace,
    /// so this just needs any `Database` handle to hang the admin client off of.
    public static DatabaseAdmin getDatabaseAdmin(ConnectionInfo info) {
        return mkDb(info, DataAPIClientOptions.DEFAULT_KEYSPACE).getDatabaseAdmin();
    }

    private static DataAPIClient mkClient(ConnectionInfo info) {
        return new DataAPIClient(info.token(), new DataAPIClientOptions().destination(info.destination()));
    }

    private static Database mkDb(ConnectionInfo info, String keyspace) {
        return mkClient(info).getDatabase(info.endpoint(), keyspace);
    }
}
