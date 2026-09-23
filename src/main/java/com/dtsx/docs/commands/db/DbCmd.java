package com.dtsx.docs.commands.db;

import com.dtsx.docs.commands.db.bootstrap.BootstrapCmd;
import com.dtsx.docs.commands.db.clean.CleanCmd;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
    name = "db",
    description = "Commands for managing the contents of a target database",
    mixinStandardHelpOptions = true,
    subcommands = {
        CleanCmd.class,
        BootstrapCmd.class,
    }
)
public class DbCmd implements Runnable {
    @Spec
    private CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}
