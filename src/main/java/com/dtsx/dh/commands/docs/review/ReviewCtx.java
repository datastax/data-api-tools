package com.dtsx.dh.commands.docs.review;

import com.dtsx.dh.config.ctx.BaseCtx;
import com.dtsx.dh.lib.ExternalPrograms;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import static com.dtsx.dh.HelperCli.CLI_DIR;

@Getter
public class ReviewCtx extends BaseCtx {
    private final int port;
    private final Path examplesFolder;
    private final Path dashboardFolder;
    private final boolean openBrowser;
    private final boolean detached;

    public ReviewCtx(ReviewArgs args, CommandSpec spec) {
        super(args, spec);
        this.port = validatePort(args.$port);
        this.examplesFolder = args.$examplesFolder.resolve();
        this.dashboardFolder = CLI_DIR.resolve("snapshot-dashboard");
        this.openBrowser = !args.$noOpen;
        this.detached = args.$detached;
    }

    @Override
    @MustBeInvokedByOverriders
    protected Set<Function<BaseCtx, ExternalProgram>> requiredPrograms() {
        return new HashSet<>(super.requiredPrograms()) {{
            add(ExternalPrograms::npm);
        }};
    }

    private int validatePort(int port) {
        if (port < 1 || port > 65535) {
            throw new ParameterException(cmd, "Invalid port number: " + port + ". Port must be between 1 and 65535.");
        }

        try (val _ = new ServerSocket(port)) {
            return port;
        } catch (IOException e) {
            throw new ParameterException(cmd,
                "Port " + port + " is already in use.\n\n" +
                "Please either:\n" +
                "  1. Stop the process using port " + port + ", or\n" +
                "  2. Specify a different port: dh review -p <port>"
            );
        }
    }
}
