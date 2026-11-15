package com.dtsx.dh.config.ctx;

import com.dtsx.dh.config.args.BaseArgs;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgramType;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static com.dtsx.dh.HelperCli.CLI_DIR;

@Getter
public abstract class BaseCtx {
    protected CommandLine cmd;

    /// Always `.dh_temp/` - contains execution environments and intermediate files.
    ///
    /// Deleted on startup if `--clean` is set, and optionally deleted after tests complete.
    private final Path tmpFolder;

    /// Built from `<PROGRAM>_COMMAND` env vars (e.g., `TSX_COMMAND=npx -y tsx`).
    ///
    /// Allows overriding default program paths when they're not in PATH or you need
    /// a specific version (e.g., `PYTHON_COMMAND=python3.11`).
    private final Map<ExternalProgramType, String[]> commandOverrides;

    public BaseCtx(BaseArgs<?> args, CommandSpec spec) {
        this.cmd = spec.commandLine();
        this.tmpFolder = CLI_DIR.resolve(".dh_temp");
        this.commandOverrides = mkCommandOverrides(args);
    }

    private Map<ExternalProgramType, String[]> mkCommandOverrides(BaseArgs<?> args) {
        val overrides = new HashMap<ExternalProgramType, String[]>();

        for (val programType : ExternalProgramType.values()) {
            val envVarName = programType.name().toUpperCase() + "_COMMAND";

            Optional.ofNullable(System.getenv(envVarName)).or(() -> Optional.ofNullable(System.getProperty(envVarName))).ifPresent((value) -> {
                overrides.put(programType, value.split(" "));
            });
        }

        for (val override : args.$commandOverrides.entrySet()) {
            overrides.put(override.getKey(), override.getValue().split(" "));
        }

        return overrides;
    }

    @MustBeInvokedByOverriders
    protected Set<Function<BaseCtx, ExternalProgram>> requiredPrograms() {
        return new HashSet<>();
    }

    public final void verifyRequiredProgramsAvailable(CommandLine cmd) {
        for (val mkProgram : requiredPrograms()) {
            val program = mkProgram.apply(this);

            program.problem().ifPresent((problem) -> {
                throw new ParameterException(cmd, program.name() + " " + problem + "; please install or upgrade it, or set the " + program.envVar() + " environment variable.");
            });
        }
    }

    /// Resolves a per-language override: an explicit flag value from `overrides`, else the
    /// `<LANG>_<envSuffix>` system property, else empty.
    protected Optional<String> languageOverride(ClientLanguage lang, Map<ClientLanguage, String> overrides, String envSuffix) {
        return Optional.ofNullable(overrides.get(lang))
            .or(() -> Optional.ofNullable(System.getProperty(lang.name().toUpperCase() + "_" + envSuffix)));
    }
}
