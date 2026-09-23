package com.dtsx.dh.commands.docs.review;

import com.dtsx.dh.config.args.BaseArgs;
import com.dtsx.dh.config.args.mixins.ExamplesFolderMixin;
import lombok.ToString;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

@ToString
public class ReviewArgs extends BaseArgs<ReviewCtx> {
    @Mixin
    public ExamplesFolderMixin $examplesFolder;

    @Option(
        names = { "-p", "--port" },
        description = "Port to run the dashboard on.",
        defaultValue = "${DASHBOARD_PORT:-3000}",
        paramLabel = "PORT"
    )
    public int $port;

    @Option(
        names = { "--no-open" },
        description = "Don't automatically open the browser.",
        defaultValue = "${NO_OPEN:-false}"
    )
    public boolean $noOpen;

    @Option(
        names = { "-d", "--detached" },
        description = "Run the dashboard in detached mode (background process).",
        defaultValue = "${DETACHED:-false}"
    )
    public boolean $detached;

    @Override
    public ReviewCtx toCtx(CommandSpec spec) {
        return new ReviewCtx(this, spec);
    }
}
