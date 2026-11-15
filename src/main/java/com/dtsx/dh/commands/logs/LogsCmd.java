package com.dtsx.dh.commands.logs;

import com.dtsx.dh.commands.BaseCmd;
import com.dtsx.dh.lib.CliLogger;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Comparator;

@Command(
    name = "logs",
    description = "Print the path for various logs",
    mixinStandardHelpOptions = true
)
public class LogsCmd extends BaseCmd<LogsCtx> {
    @Mixin
    @Getter
    private LogsArgs $args;

    @Override
    @SneakyThrows
    @SuppressWarnings("resource")
    protected int run() {
        val logsDir = CliLogger.logsDir(ctx);

        val toPrint = switch (ctx.mode()) {
            case LAST -> {
                yield Files.list(logsDir)
                    .max(Comparator.comparing(this::modifiedTime))
                    .orElseThrow(() -> new RuntimeException("No log files found in " + logsDir));
            }
            case DIR -> {
                yield logsDir;
            }
        };

        CliLogger.println(false, toPrint.toAbsolutePath().toString());
        return 0;
    }

    @SneakyThrows
    private FileTime modifiedTime(Path path) {
        return Files.getLastModifiedTime(path);
    }
}
