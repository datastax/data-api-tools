package com.dtsx.dh.config.args;

import com.dtsx.dh.config.args.mixins.ExamplesFolderMixin;
import com.dtsx.dh.config.ctx.BaseScriptRunnerCtx;
import com.dtsx.dh.core.common.ClientLanguage;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.Map;

public abstract class BaseScriptRunnerArgs<Ctx extends BaseScriptRunnerCtx> extends BaseConnectedArgs<Ctx> {
    @Mixin
    public ExamplesFolderMixin $examplesFolder;

    @Option(
        names = { "-A", "--client-artifact" },
        description = "Client artifacts to install (e.g.,`-Atypescript=@datastax/astra-db-ts@v2.0.0' -Apython=/path/to/local/package`).",
        paramLabel = "CLIENT=ARTIFACT"
    )
    public Map<ClientLanguage, String> $artifactOverrides = Map.of();

    @Option(
        names = { "--clean" },
        description = "Whether to clean the execution environment before running tests.",
        defaultValue = "${CLEAN:-false}"
    )
    public boolean $clean;

    @Option(
        names = { "-b", "--bail" },
        description = "Whether to stop execution upon the first failure.",
        defaultValue = "${BAIL:-false}"
    )
    public boolean $bail;
}
