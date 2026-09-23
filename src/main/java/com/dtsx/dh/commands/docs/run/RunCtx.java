package com.dtsx.dh.commands.docs.run;

import com.dtsx.dh.config.ctx.BaseCtx;
import com.dtsx.dh.config.ctx.BaseScriptRunnerCtx;
import com.dtsx.dh.core.docs.runner.PlaceholderVars.PlaceholderVar;
import com.dtsx.dh.core.docs.runner.Placeholders;
import com.dtsx.dh.core.docs.runner.PlaceholderVars;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.core.docs.runner.scripts.reporter.DetailedScriptReporter;
import com.dtsx.dh.core.docs.runner.scripts.reporter.PlainScriptReporter;
import com.dtsx.dh.core.docs.runner.scripts.reporter.ScriptReporter;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.toSet;

@Getter
public class RunCtx extends BaseScriptRunnerCtx {
    private final Map<ClientDriver, Set<Path>> scripts;
    private final Placeholders placeholders;
    private final ScriptReporter reporter;

    public RunCtx(RunArgs args, CommandSpec spec) {
        super(args, spec);

        this.placeholders = mkPlaceholders(args);
        this.scripts = associateDriversToFiles(mkScriptPaths(args), args);
        this.reporter = mkReporter(args);
    }

    @Override
    @MustBeInvokedByOverriders
    protected Set<Function<BaseCtx, ExternalProgram>> requiredPrograms() {
        return new HashSet<>(super.requiredPrograms()) {{
            for (val driver : scripts.keySet()) {
                addAll(driver.requiredPrograms());
            }
        }};
    }

    private Placeholders mkPlaceholders(RunArgs args) {
        val vars = new HashMap<String, PlaceholderVar>();

        args.$replace.forEach((regex, value) -> {
            vars.put(regex, new PlaceholderVar(regex, value));
        });

        return new Placeholders(args.$collection, args.$table, args.$keyspace, new PlaceholderVars(vars));
    }

    private Set<Path> mkScriptPaths(RunArgs args) {
        if (args.$scripts == null || args.$scripts.isEmpty()) {
            throw new ParameterException(cmd, "Must provide at least one file to run.");
        }

        return args.$scripts.stream()
            .map(this::resolveAbsFilePath)
            .collect(toSet());
    }

    private Path resolveAbsFilePath(String fileStr) {
        val roots = List.of(
            examplesFolder(),
            Path.of(System.getenv("CLI_DIR"))
        );

        val triedPaths = new HashSet<String>();
        val absExamplesFolder = examplesFolder().toAbsolutePath().normalize();

        for (val root : roots) {
            val candidate = root.resolve(fileStr);
            val absCandidate = candidate.toAbsolutePath().normalize();

            if (!absCandidate.startsWith(absExamplesFolder)) {
                throw new ParameterException(cmd, "Invalid file path '" + candidate + "'. Must be inside examples folder: " + examplesFolder());
            }

            if (Files.exists(candidate)) {
                return absCandidate;
            }

            triedPaths.add(absCandidate.toString());
        }

        throw new ParameterException(cmd, "Could not resolve '" + fileStr + "'. Tried: " + String.join(", ", triedPaths));
    }

    private Map<ClientDriver, Set<Path>> associateDriversToFiles(Set<Path> files, RunArgs args) {
        val langsToFiles = new HashMap<ClientLanguage, Set<Path>>() {{
            for (val file : files) {
                computeIfAbsent(resolveLanguageForFile(file), _ -> new HashSet<>()).add(file);
            }
        }};

        return new HashMap<>() {{
            langsToFiles.forEach((lang, paths) -> {
                put(mkDriverForLanguage(cmd, lang, args), paths);
            });
        }};
    }

    private ClientLanguage resolveLanguageForFile(Path path) {
        val fileName = path.getFileName().toString().toLowerCase();

        for (val lang : ClientLanguage.values()) {
            if (fileName.endsWith(lang.extension())) {
                return lang;
            }
        }

        throw new ParameterException(cmd, "Unknown language extension for file '" + path + "'");
    }

    private ScriptReporter mkReporter(RunArgs args) {
        if (args.$plain) {
            if (args.$scripts.size() > 1) {
                throw new ParameterException(cmd, "The --plain option can only be used when running a single script file.");
            }
            return new PlainScriptReporter();
        }
        return new DetailedScriptReporter();
    }
}
