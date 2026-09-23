package com.dtsx.dh.commands.db.clean;

import com.datastax.astra.client.admin.DatabaseAdmin;
import com.dtsx.dh.commands.BaseCmd;
import com.dtsx.dh.config.ConnectionInfo;
import com.dtsx.dh.config.SystemKeyspaces;
import com.dtsx.dh.lib.CliLogger;
import com.dtsx.dh.lib.DataAPIUtils;
import com.dtsx.dh.lib.ExecutorUtils;
import lombok.Getter;
import lombok.val;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

@Command(
    name = "clean",
    description = "Drop collections, tables and UDTs from a target database, optionally the keyspaces too."
)
public class CleanCmd extends BaseCmd<CleanCtx> {
    @Mixin @Getter
    private CleanArgs $args;

    @Override
    protected int run() {
        val connInfo = ctx.connectionInfo();
        val admin = DataAPIUtils.getDatabaseAdmin(connInfo);

        val targets = resolveTargetKeyspaces(admin);

        if (targets.isEmpty()) {
            CliLogger.println(false, "@|bold No keyspaces to clean.|@");
            return 0;
        }

        CliLogger.println(false, "@|bold Plan:|@");

        var total = 0;

        for (val ks : targets) {
            total += cleanKeyspace(connInfo, ks);
        }

        val keyspacesToDrop = targets.stream()
            .filter((ks) -> !ctx.keepKeyspaces().contains(ks))
            .toList();

        if (ctx.dropKeyspaces() && !keyspacesToDrop.isEmpty()) {
            CliLogger.println(false);
            CliLogger.println(false, "@|red The keyspace(s) themselves will also be dropped:|@ " + String.join(", ", keyspacesToDrop));
        }

        CliLogger.println(false);
        CliLogger.println(false, "@|bold Total:|@ @!" + total + "!@ item(s) across " + targets.size() + " keyspace(s).");

        if (!ctx.yes()) {
            CliLogger.println(false);
            CliLogger.println(false, "@|bold Dry run|@ - pass -y to actually drop these.");
            return 0;
        }

        if (ctx.dropKeyspaces() && !keyspacesToDrop.isEmpty()) {
            CliLogger.println(false);
            CliLogger.loading("Dropping keyspace(s) @!" + String.join(", ", keyspacesToDrop) + "!@...", (update) -> {
                keyspacesToDrop.forEach(admin::dropKeyspace);
                return null;
            });
        }

        CliLogger.println(false, "@|bold,green Done.|@");
        return 0;
    }

    /// Resolves the live keyspace listing to all non-system keyspaces, via `SystemKeyspaces`.
    private List<String> resolveTargetKeyspaces(DatabaseAdmin admin) {
        val existing = admin.listKeyspaceNames();

        CliLogger.debug("Raw listKeyspaces() result: " + existing);

        return existing.stream()
            .filter((ks) -> !SystemKeyspaces.isSystemKeyspace(ks))
            .sorted()
            .toList();
    }

    /// Lists and prints the contents of a single keyspace and, when `-y` was passed, drops
    /// them - collections and tables concurrently first, then UDTs concurrently, since tables
    /// can reference UDTs. Returns the item count, for the running total.
    private int cleanKeyspace(ConnectionInfo connInfo, String keyspace) {
        val db = DataAPIUtils.getDatabase(connInfo, keyspace);

        val collections = db.listCollectionNames();
        val tables = db.listTableNames();
        val udts = db.listTypeNames();

        val total = collections.size() + tables.size() + udts.size();

        if (total == 0) {
            CliLogger.println(false, "  @!" + keyspace + "!@: nothing to drop");
            return 0;
        }

        CliLogger.println(false, "  @!" + keyspace + "!@:");
        printItems("collections", collections);
        printItems("tables", tables);
        printItems("UDTs", udts);

        if (ctx.yes()) {
            CliLogger.loading("Dropping contents of @!" + keyspace + "!@...", (update) -> {
                val collectionsAndTables = new ArrayList<Runnable>();
                collections.forEach((name) -> collectionsAndTables.add(() -> db.dropCollection(name)));
                tables.forEach((name) -> collectionsAndTables.add(() -> db.dropTable(name)));
                runConcurrently(collectionsAndTables);

                runConcurrently(udts.stream().<Runnable>map((name) -> () -> db.dropType(name)).toList());

                return null;
            });
        }

        return total;
    }

    private void printItems(String label, List<String> names) {
        if (names.isEmpty()) {
            return;
        }
        CliLogger.println(false, "    " + label + ": " + String.join(", ", names));
    }

    private void runConcurrently(List<Runnable> tasks) {
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
