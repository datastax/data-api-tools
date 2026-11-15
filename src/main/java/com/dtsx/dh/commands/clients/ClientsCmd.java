package com.dtsx.dh.commands.clients;

import com.dtsx.dh.commands.clients.test.ClientsTestCmd;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
    name = "clients",
    description = "Commands for running each data API client's own integration test suite against a target database",
    mixinStandardHelpOptions = true,
    subcommands = {
        ClientsTestCmd.class,
    }
)
public class ClientsCmd implements Runnable {
    @Spec
    private CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}
