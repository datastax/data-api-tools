package com.dtsx.docs.commands.docs.gen;

import com.dtsx.docs.commands.docs.gen.table_errors.TableErrorsCmd;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
    name = "gen",
    description = "Documentation generation commands",
    mixinStandardHelpOptions = true,
    subcommands = {
        TableErrorsCmd.class
    }
)
public class GenCmd implements Runnable {
    @Spec
    private CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}
