package com.dtsx.docs.config.args;

import com.dtsx.docs.config.ctx.BaseConnectedCtx;
import picocli.CommandLine.Option;

import java.util.Optional;

/// Shared `-e/--api-endpoint`, `-t/--astra-token` and `--local` options for anything that
/// connects to a target database. Each subclass's `Ctx` decides how to resolve them.
public abstract class BaseConnectedArgs<Ctx extends BaseConnectedCtx> extends BaseArgs<Ctx> {
    @Option(
        names = { "-e", "--api-endpoint" },
        description = "Target database API endpoint.",
        paramLabel = "ENDPOINT"
    )
    public Optional<String> $apiEndpoint;

    @Option(
        names = { "-t", "--astra-token" },
        description = "Astra token for the target database.",
        paramLabel = "TOKEN"
    )
    public Optional<String> $astraToken;

    @Option(
        names = { "--local" },
        description = "Target the local startgate stack. Defaults the endpoint to http://localhost:8181 and the token to the well-known local Cassandra token; '-e'/'-t' override either half individually."
    )
    public boolean $local;
}
