package com.dtsx.dh.commands.docs;

import com.dtsx.dh.commands.docs.gen.GenCmd;
import com.dtsx.dh.commands.docs.review.ReviewCmd;
import com.dtsx.dh.commands.docs.run.RunCmd;
import com.dtsx.dh.commands.docs.test.TestCmd;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
    name = "docs",
    description = "Commands for testing/generating documentation examples",
    mixinStandardHelpOptions = true,
    subcommands = {
        TestCmd.class,
        RunCmd.class,
        ReviewCmd.class,
        GenCmd.class,
    }
)
public class DocsCmd implements Runnable {
    @Spec
    private CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }
}
