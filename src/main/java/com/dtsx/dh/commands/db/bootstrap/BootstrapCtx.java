package com.dtsx.dh.commands.db.bootstrap;

import com.dtsx.dh.config.ConnectionInfo;
import com.dtsx.dh.config.ctx.BaseConnectedCtx;
import lombok.Getter;
import picocli.CommandLine.Model.CommandSpec;

import java.util.List;

@Getter
public class BootstrapCtx extends BaseConnectedCtx {
    /// Keyspaces named via `-k`, defaulted to just `default_keyspace` when none are given.
    private final List<String> keyspaces;

    private final boolean yes;

    public BootstrapCtx(BootstrapArgs args, CommandSpec spec) {
        super(args, spec, ConnectionInfo.fromFlags(spec.commandLine(), args));
        this.keyspaces = args.$keyspaces;
        this.yes = args.$yes.unwrap();
    }
}
