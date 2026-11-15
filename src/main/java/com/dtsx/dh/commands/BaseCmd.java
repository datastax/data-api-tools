package com.dtsx.dh.commands;

import com.dtsx.dh.config.args.BaseArgs;
import com.dtsx.dh.config.ctx.BaseCtx;
import com.dtsx.dh.lib.CliLogger;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.concurrent.Callable;

@Command(mixinStandardHelpOptions = true)
public abstract class BaseCmd<Ctx extends BaseCtx> implements Callable<Integer> {
    public static final int EXIT_CODE_TESTS_FAILED = 100;

    @Spec
    protected CommandSpec spec;

    protected Ctx ctx;

    @Override
    public final Integer call() {
        ctx = $args().toCtx(spec);
        CliLogger.initialize(ctx);
        ctx.verifyRequiredProgramsAvailable(spec.commandLine());
        return run();
    }

    protected abstract int run();
    protected abstract BaseArgs<Ctx> $args();
}
