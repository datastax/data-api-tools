package com.dtsx.docs.commands.docs.run;

import com.dtsx.docs.config.args.BaseScriptRunnerArgs;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RunArgs extends BaseScriptRunnerArgs<RunCtx> {
    @Parameters(
        description = "Files to run.",
        paramLabel = "FILE",
        split = ","
    )
    public List<String> $scripts;

    @Option(
        names = { "-c", "--collection" },
        description = "Collection name for placeholder replacement.",
        paramLabel = "NAME"
    )
    public Optional<String> $collection;

    @Option(
        names = { "--table" },
        description = "Table name for placeholder replacement.",
        paramLabel = "NAME"
    )
    public Optional<String> $table;

    @Option(
        names = { "-k", "--keyspace" },
        description = "Keyspace name for placeholder replacement.",
        defaultValue = "default_keyspace",
        paramLabel = "NAME"
    )
    public String $keyspace;

    @Option(
        names = { "-r", "--replace" },
        description = "Custom variables for placeholder replacement in the format `-v KEY=value`. Can be specified multiple times.",
        paramLabel = "KEY=value",
        split = ","
    )
    public Map<String, String> $replace = Map.of();

    @Option(
        names = { "-p", "--plain" },
        description = "Plain output without colors or formatting. Only works with a single script file."
    )
    public boolean $plain;

    @Override
    public RunCtx toCtx(CommandSpec spec) {
        return new RunCtx(this, spec);
    }
}
