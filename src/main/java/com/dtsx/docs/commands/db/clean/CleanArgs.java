package com.dtsx.docs.commands.db.clean;

import com.dtsx.docs.config.args.BaseConnectedArgs;
import lombok.ToString;
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

    @Option(
        names = { "-y", "--yes" },
        description = "Actually perform the operation. Without this, prints the plan and exits."
    )
    public boolean $yes;

    @Override
    public CleanCtx toCtx(CommandSpec spec) {
        return new CleanCtx(this, spec);
    }
}
