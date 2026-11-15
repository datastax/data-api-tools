package com.dtsx.dh.commands.startgate;

import com.dtsx.dh.commands.BaseCmd;
import com.dtsx.dh.lib.CliLogger;
import lombok.Getter;
import lombok.val;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.io.IOException;
import java.util.ArrayList;

import static com.dtsx.dh.HelperCli.CLI_DIR;

@Command(
    name = "startgate",
    description = "Spin up the Data API on DSE or HCD."
)
public class StartgateCmd extends BaseCmd<StartgateCtx> {
    @Mixin
    @Getter
    private StartgateArgs $args;

    @Override
    protected int run() {
        val target = switch (ctx.mode()) {
            case DSE -> "DSE";
            case HCD -> "HCD";
        };

        CliLogger.println(false, "@|bold Starting Stargate with " + target + " database...|@");

        val file = CLI_DIR.resolve("scripts/utils/docker-" + target.toLowerCase() + "/docker-compose.yml");

        val commandList = new ArrayList<String>();
        if (isDockerComposeAvailable()) {
            commandList.add("docker-compose");
        } else {
            commandList.add("podman-compose");
        }

        commandList.add("-f");
        commandList.add(file.toAbsolutePath().toString());
        
        if (ctx.down()) {
            commandList.add("down");
            CliLogger.println(false, "@|bold Stopping Stargate (" + target + ")...|@");
        } else {
            commandList.add("up");
            if (ctx.detached()) {
                commandList.add("-d");
            }
        }

        CliLogger.println(false, "@|bold Running command:|@ @!" + String.join(" ", commandList) + "!@");

        val pb = new ProcessBuilder(commandList)
            .directory(file.getParent().toFile())
            .inheritIO();

        try {
            val exitCode = pb.start().waitFor();
            if (exitCode != 0) {
                CliLogger.println(false, "@|red Command failed with exit code: " + exitCode + "|@");
                return exitCode;
            }
            return 0;
        } catch (IOException | InterruptedException e) {
            CliLogger.println(false, "@|red Error: Failed to execute docker-compose or podman-compose. Please make sure either one is installed and running.|@");
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return 1;
        }
    }

    private boolean isDockerComposeAvailable() {
        try {
            val process = new ProcessBuilder("docker-compose", "--version")
                .redirectErrorStream(true)
                .start();
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
