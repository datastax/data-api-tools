package com.dtsx.dh.commands.db.bootstrap;

import com.dtsx.dh.commands.BaseCmd;
import com.dtsx.dh.lib.CliLogger;
import com.dtsx.dh.lib.DataAPIUtils;
import lombok.Getter;
import lombok.val;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(
    name = "bootstrap",
    description = "Create keyspaces on a target database if they don't already exist."
)
public class BootstrapCmd extends BaseCmd<BootstrapCtx> {
    @Mixin @Getter
    private BootstrapArgs $args;

    @Override
    protected int run() {
        val admin = DataAPIUtils.getDatabaseAdmin(ctx.connectionInfo());
        val existing = admin.listKeyspaceNames();

        CliLogger.println(false, "@|bold Plan:|@");
        for (val ks : ctx.keyspaces()) {
            val status = existing.contains(ks) ? "already exists" : "would be created";
            CliLogger.println(false, "  @!" + ks + "!@: " + status);
        }

        if (!ctx.yes()) {
            CliLogger.println(false);
            CliLogger.println(false, "@|bold Dry run|@ - pass -y to actually create these.");
            return 0;
        }

        CliLogger.println(false);

        for (val ks : ctx.keyspaces()) {
            if (existing.contains(ks)) {
                CliLogger.println(false, "@!" + ks + "!@ @|faint already exists.|@");
                continue;
            }

            CliLogger.loading("Creating keyspace @!" + ks + "!@...", (update) -> {
                admin.createKeyspace(ks);
                return null;
            });

            CliLogger.println(false, "@|green Created|@ @!" + ks + "!@.");
        }

        return 0;
    }
}
