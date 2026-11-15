package com.dtsx.dh.config;

import lombok.val;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class ArgUtils {
    public static <T> T requireFlag(CommandLine cmd, Optional<T> optional, String name, String flag, String envVar) {
        return optional.orElseThrow(() -> new ParameterException(cmd,
            "Missing required option " + name + "; please provide it via the '" + flag + "' command line flag or the '" + envVar + "' environment variable."
        ));
    }

    @SuppressWarnings({ "SameParameterValue", "UnusedReturnValue" })
    public static <T> T requireParameter(CommandLine cmd, Optional<T> parameter, String name, int index, String envVar) {
        return parameter.orElseThrow(() -> new ParameterException(cmd,
            "Missing required parameter " + name + "; please provide it as parameter #" + index + " or via the '" + envVar + "' environment variable."
        ));
    }

    public static Path requirePath(CommandLine cmd, String rawPath, String name, String flag, String envVar) {
        val path = Path.of(rawPath);

        if (!Files.exists(path)) {
            throw new ParameterException(cmd,
                "The provided " + name + " path '" + path.toAbsolutePath() + "' does not exist; please provide a valid path via the '" + flag + "' command line flag or the '" + envVar + "' environment variable."
            );
        }
        return path;
    }
}
