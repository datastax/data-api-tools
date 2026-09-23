package com.dtsx.dh.commands.db.clean;

import com.dtsx.dh.config.ConnectionInfo;
import com.dtsx.dh.config.ctx.BaseConnectedCtx;
import lombok.Getter;
import picocli.CommandLine.Model.CommandSpec;

import java.util.List;

@Getter
public class CleanCtx extends BaseConnectedCtx {
    private final boolean dropKeyspaces;

    /// Keyspaces to preserve when `dropKeyspaces` is set; their contents are still cleaned.
    private final List<String> keepKeyspaces;

    private final boolean yes;

    public CleanCtx(CleanArgs args, CommandSpec spec) {
        super(args, spec, ConnectionInfo.fromFlags(spec.commandLine(), args));
        this.dropKeyspaces = args.$dropKeyspaces;
        this.keepKeyspaces = args.$keepKeyspaces;
        this.yes = args.$yes;
    }
}
