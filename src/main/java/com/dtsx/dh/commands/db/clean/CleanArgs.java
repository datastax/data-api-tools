package com.dtsx.dh.commands.db.clean;

import com.dtsx.dh.config.args.BaseConnectedArgs;
import com.dtsx.dh.config.args.mixins.YesMixin;
import lombok.ToString;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

import java.util.List;

@ToString
public class CleanArgs extends BaseConnectedArgs<CleanCtx> {
    @Option(
        names = { "--drop-keyspaces" },
        description = "Also drop the keyspaces themselves, not just their contents."
    )
    public boolean $dropKeyspaces;

    @Option(
        names = { "--keep-keyspace" },
        description = "Keyspaces to preserve when '--drop-keyspaces' is passed; their contents are still cleaned.",
        paramLabel = "KEYSPACE",
        split = ","
    )
    public List<String> $keepKeyspaces = List.of();

    @Mixin
    public YesMixin $yes;

    @Override
    public CleanCtx toCtx(CommandSpec spec) {
        return new CleanCtx(this, spec);
    }
}
