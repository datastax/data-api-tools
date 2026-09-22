package com.dtsx.docs.config.ctx;

import com.dtsx.docs.config.args.BaseArgs;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgramType;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

import static com.dtsx.docs.HelperCli.CLI_DIR;

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

            if (!program.exists()) {
                throw new ParameterException(cmd, program.name() + " could not be found. Please install it or set the " + program.envVar() + " environment variable.");
            }
        }
    }
}
