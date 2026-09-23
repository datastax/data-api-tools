package com.dtsx.dh.commands.db.clean;

import com.datastax.astra.client.admin.DatabaseAdmin;
import com.dtsx.dh.commands.BaseCmd;
import com.dtsx.dh.config.ConnectionInfo;
import com.dtsx.dh.lib.CliLogger;
import com.dtsx.dh.lib.DataAPIUtils;
import com.dtsx.dh.lib.KeyspaceOps;
import lombok.Getter;
import lombok.val;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.util.List;

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
            CliLogger.loading("Dropping keyspace(s) @!" + String.join(", ", keyspacesToDrop) + "!@...", (_) -> {
                keyspacesToDrop.forEach(admin::dropKeyspace);
                return null;
            });
        }

        CliLogger.println(false, "@|bold,green Done.|@");
        return 0;
    }

    /// Resolves the live keyspace listing to all non-system keyspaces.
    private List<String> resolveTargetKeyspaces(DatabaseAdmin admin) {
        val existing = admin.listKeyspaceNames();

        CliLogger.debug("Raw listKeyspaces() result: " + existing);

        return KeyspaceOps.filterNonSystem(existing);
    }

    /// Lists and prints the contents of a single keyspace and, when `-y` was passed, drops them.
    /// Returns the item count, for the running total.
    private int cleanKeyspace(ConnectionInfo connInfo, String keyspace) {
        val contents = KeyspaceOps.listContents(connInfo, keyspace);

        if (contents.total() == 0) {
            CliLogger.println(false, "  @!" + keyspace + "!@: nothing to drop");
            return 0;
        }

        CliLogger.println(false, "  @!" + keyspace + "!@:");
        printItems("collections", contents.collections());
        printItems("tables", contents.tables());
        printItems("UDTs", contents.udts());

        if (ctx.yes()) {
            CliLogger.loading("Dropping contents of @!" + keyspace + "!@...", (_) -> {
                KeyspaceOps.dropContents(connInfo, keyspace, contents);
                return null;
            });
        }

        return contents.total();
    }

    private void printItems(String label, List<String> names) {
        if (names.isEmpty()) {
            return;
        }
        CliLogger.println(false, "    " + label + ": " + String.join(", ", names));
    }
}
