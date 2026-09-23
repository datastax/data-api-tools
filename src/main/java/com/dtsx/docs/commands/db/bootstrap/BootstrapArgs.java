package com.dtsx.docs.commands.db.bootstrap;

import com.dtsx.docs.config.args.BaseConnectedArgs;
import lombok.ToString;
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

    @Option(
        names = { "-y", "--yes" },
        description = "Actually perform the operation. Without this, prints the plan and exits."
    )
    public boolean $yes;

    @Override
    public BootstrapCtx toCtx(CommandSpec spec) {
        return new BootstrapCtx(this, spec);
    }
}
