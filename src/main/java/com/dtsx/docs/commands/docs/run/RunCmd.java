package com.dtsx.docs.commands.docs.run;

import com.dtsx.docs.commands.BaseCmd;
import com.dtsx.docs.core.runner.scripts.ScriptRunner;
import lombok.Getter;
import lombok.val;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(
    name = "run",
    description = "Run script files without testing them."
)
public class RunCmd extends BaseCmd<RunCtx> {
    @Mixin @Getter
    private RunArgs $args;

    @Override
    public int run() {
        val ok = ScriptRunner.runScripts(ctx);
        return (ok) ? 0 : 1;
    }
}
