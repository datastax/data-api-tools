package com.dtsx.dh.config.args.mixins;

import com.dtsx.dh.config.ArgUtils;
import lombok.val;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ExamplesFolderMixin {
    @Option(
        names = { "-ef", "--examples-folder" },
        description = "Path to the folder containing example projects.",
        defaultValue = "${EXAMPLES_FOLDER:-" + Option.NULL_VALUE + "}",
        paramLabel = "FOLDER"
    )
    public Optional<String> $examplesFolder;

    @Spec
    private CommandSpec spec;

    public Path resolve() {
        val folder = ArgUtils.requirePath(spec.commandLine(), $examplesFolder.orElse("."), "examples folder", "-ef", "EXAMPLES_FOLDER");

        Predicate<Path> isValidExampleFolder = (path) -> {
            return Stream.of(path, path.resolve("_base"), path.resolve("_fixtures")).allMatch(Files::isDirectory);
        };

        val subPaths = List.of(
            Path.of(""),
            Path.of("modules/api-reference/examples"),
            Path.of("resources/mock_examples")
        );

        for (val subPath : subPaths) {
            val candidate = folder.resolve(subPath);
            if (isValidExampleFolder.test(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }

        throw new ParameterException(spec.commandLine(), "None of the following paths are valid examples folders: " +
            subPaths.stream().map(p -> folder.resolve(p).toString()).reduce((a, b) -> a + ", " + b).orElse(""));
    }
}
