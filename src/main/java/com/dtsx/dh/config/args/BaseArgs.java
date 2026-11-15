package com.dtsx.dh.config.args;

import com.dtsx.dh.config.ctx.BaseCtx;
import com.dtsx.dh.lib.CliLogger;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgramType;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

import java.util.Map;

public abstract class BaseArgs<Ctx extends BaseCtx> {
    @Option(
        names = { "-C", "--command-override" },
        description = "Override commands for external programs (e.g., `-Ctsx='npx -y tsx' -Cbash=/usr/bin/bash`).",
        paramLabel = "PROGRAM=COMMAND"
    )
    public Map<ExternalProgramType, String> $commandOverrides = Map.of();

    @Option(
        names = { "--spinner" },
        description = "enable/disable spinner in the CLI output.",
        defaultValue = "${SPINNER:-true}",
        fallbackValue = "true",
        negatable = true
    )
    public void $spinner(boolean enabled) {
        CliLogger.setSpinnerEnabled(enabled);
    }

    public abstract Ctx toCtx(CommandSpec spec);
}
