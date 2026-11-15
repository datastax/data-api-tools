package com.dtsx.dh.commands.startgate;

import com.dtsx.dh.config.args.BaseArgs;
import lombok.ToString;
import picocli.CommandLine.Option;
import picocli.CommandLine.Model.CommandSpec;

@ToString
public class StartgateArgs extends BaseArgs<StartgateCtx> {
    @Option(
        names = { "-m", "--mode" },
        description = "Select the mode to run: ${COMPLETION-CANDIDATES}.",
        required = true
    )
    public StartgateMode $mode;

    @Option(
        names = { "-d", "--detached" },
        description = "Run in detached mode (in the background)."
    )
    public boolean $detached;

    @Option(
        names = { "-D", "--down" },
        description = "Stop and remove containers, networks, and volumes."
    )
    public boolean $down;

    @Override
    public StartgateCtx toCtx(CommandSpec spec) {
        return new StartgateCtx(this, spec);
    }
}
