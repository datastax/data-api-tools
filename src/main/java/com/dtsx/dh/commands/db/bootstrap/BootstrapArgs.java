package com.dtsx.dh.commands.db.bootstrap;

import com.dtsx.dh.config.args.BaseConnectedArgs;
import com.dtsx.dh.config.args.mixins.YesMixin;
import lombok.ToString;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

import java.util.List;

@ToString
public class BootstrapArgs extends BaseConnectedArgs<BootstrapCtx> {
    @Option(
        names = { "-k", "--keyspace" },
        description = "Keyspaces to create if missing. Repeatable and comma-splittable. Defaults to 'default_keyspace'.",
        paramLabel = "KEYSPACE",
        split = ",",
        defaultValue = "default_keyspace"
    )
    public List<String> $keyspaces;

    @Mixin
    public YesMixin $yes;

    @Override
    public BootstrapCtx toCtx(CommandSpec spec) {
        return new BootstrapCtx(this, spec);
    }
}
