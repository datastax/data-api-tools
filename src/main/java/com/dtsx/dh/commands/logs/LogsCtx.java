package com.dtsx.dh.commands.logs;

import com.dtsx.dh.config.ctx.BaseCtx;
import lombok.Getter;
import picocli.CommandLine.Model.CommandSpec;

@Getter
public class LogsCtx extends BaseCtx {
    private final LogsMode mode;

    public LogsCtx(LogsArgs args, CommandSpec spec) {
        super(args, spec);

        this.mode = (args.which.last)
            ? LogsMode.LAST
            : LogsMode.DIR;
    }

    public enum LogsMode {
        LAST,
        DIR
    }
}
