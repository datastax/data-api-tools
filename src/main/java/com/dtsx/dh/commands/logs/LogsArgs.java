package com.dtsx.dh.commands.logs;

import com.dtsx.dh.config.args.BaseArgs;
import lombok.ToString;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

@ToString
public class LogsArgs extends BaseArgs<LogsCtx> {
    @ArgGroup(multiplicity = "1")
    public WhichLogs which;

    @ToString
    public static class WhichLogs {
        @Option(
            names = { "--last" },
            description = "Path to the last generated log file",
            required = true
        )
        boolean last;

        @Option(
            names = { "--dir" },
            description = "Path to the directory containing all log files",
            required = true
        )
        boolean dir;
    }

    @Override
    public LogsCtx toCtx(CommandSpec spec) {
        return new LogsCtx(this, spec);
    }
}
