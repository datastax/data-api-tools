package com.dtsx.dh.lib;

import com.datastax.astra.client.admin.DatabaseAdmin;
import com.dtsx.dh.config.ConnectionInfo;
import com.dtsx.dh.config.SystemKeyspaces;
import lombok.val;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executors;

/// Keyspace bootstrap and content-wipe logic shared by `dh db bootstrap`, `dh db clean`, and the
/// `dh clients test` runner's own bootstrap/wipe steps.
public class KeyspaceOps {
    /// The collections, tables and UDTs in a single keyspace.
    public record KeyspaceContents(List<String> collections, List<String> tables, List<String> udts) {
        public int total() {
            return collections.size() + tables.size() + udts.size();
        }
    }

    /// Lists every non-system keyspace on the target database, sorted.
    public static List<String> listNonSystemKeyspaces(DatabaseAdmin admin) {
        return filterNonSystem(admin.listKeyspaceNames());
    }

    /// Filters an already-fetched keyspace listing down to the non-system ones, sorted.
    public static List<String> filterNonSystem(Collection<String> keyspaces) {
        return keyspaces.stream()
            .filter((ks) -> !SystemKeyspaces.isSystemKeyspace(ks))
            .sorted()
            .toList();
    }

    /// Lists the collections, tables and UDTs in `keyspace`.
    public static KeyspaceContents listContents(ConnectionInfo connInfo, String keyspace) {
        val db = DataAPIUtils.getDatabase(connInfo, keyspace);
        return new KeyspaceContents(db.listCollectionNames(), db.listTableNames(), db.listTypeNames());
    }

    /// Wipes the contents of every non-system keyspace on the target database.
    public static void wipeAllContents(ConnectionInfo connInfo, DatabaseAdmin admin) {
        for (val ks : listNonSystemKeyspaces(admin)) {
            val contents = listContents(connInfo, ks);
            dropContents(connInfo, ks, contents);
        }
    }

    /// Drops every collection, table and UDT in `contents` from `keyspace`. Collections and tables
    /// drop concurrently first, then UDTs concurrently, since tables can reference UDTs.
    public static void dropContents(ConnectionInfo connInfo, String keyspace, KeyspaceContents contents) {
        val db = DataAPIUtils.getDatabase(connInfo, keyspace);

        val collectionsAndTables = new ArrayList<Runnable>();
        contents.collections().forEach((name) -> collectionsAndTables.add(() -> db.dropCollection(name)));
        contents.tables().forEach((name) -> collectionsAndTables.add(() -> db.dropTable(name)));
        runConcurrently(collectionsAndTables);

        runConcurrently(contents.udts().stream().<Runnable>map((name) -> () -> db.dropType(name)).toList());
    }

    /// Creates `keyspace` if it isn't already present in `existing`.
    public static void ensureKeyspace(DatabaseAdmin admin, String keyspace, Collection<String> existing) {
        if (!existing.contains(keyspace)) {
            admin.createKeyspace(keyspace);
        }
    }

    private static void runConcurrently(List<Runnable> tasks) {
        if (tasks.isEmpty()) {
            return;
        }

        try (val executor = Executors.newVirtualThreadPerTaskExecutor()) {
            val futures = ExecutorUtils.emptyFuturesList();
            tasks.forEach((task) -> futures.add(executor.submit(task)));
            ExecutorUtils.awaitAll(futures);
        }
    }
}
