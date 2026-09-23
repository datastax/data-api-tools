package com.dtsx.dh.config.ctx;

import com.dtsx.dh.config.ConnectionInfo;
import com.dtsx.dh.config.args.BaseConnectedArgs;
import lombok.Getter;
import picocli.CommandLine.Model.CommandSpec;

@Getter
public abstract class BaseConnectedCtx extends BaseCtx {
    /// Parsed from the `-t`/`-e`/`--local` flags, and possibly the `ASTRA_TOKEN`/`API_ENDPOINT`
    /// env vars, depending on which of [ConnectionInfo#fromFlags]/[ConnectionInfo#fromFlagsOrEnv]
    /// the subclass resolves it with.
    private final ConnectionInfo connectionInfo;

    protected BaseConnectedCtx(BaseConnectedArgs<?> args, CommandSpec spec, ConnectionInfo connectionInfo) {
        super(args, spec);
        this.connectionInfo = connectionInfo;
    }
}
