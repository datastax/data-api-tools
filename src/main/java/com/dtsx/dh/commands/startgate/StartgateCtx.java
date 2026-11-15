package com.dtsx.dh.commands.startgate;

import com.dtsx.dh.config.ctx.BaseCtx;
import lombok.Getter;
import picocli.CommandLine.Model.CommandSpec;

@Getter
public class StartgateCtx extends BaseCtx {
    private final StartgateMode mode;
    private final boolean detached;
    private final boolean down;

    public StartgateCtx(StartgateArgs args, CommandSpec spec) {
        super(args, spec);
        this.mode = args.$mode;
        this.detached = args.$detached;
        this.down = args.$down;
    }
}
