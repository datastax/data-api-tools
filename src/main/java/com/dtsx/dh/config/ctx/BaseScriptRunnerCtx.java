package com.dtsx.dh.config.ctx;

import com.dtsx.dh.config.ConnectionInfo;
import com.dtsx.dh.config.args.BaseScriptRunnerArgs;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.lib.ExternalPrograms;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static com.dtsx.dh.HelperCli.CLI_DIR;

@Getter
public abstract class BaseScriptRunnerCtx extends BaseConnectedCtx {
    /// Resolved from `EXAMPLES_FOLDER` env var, with fallback to `./resources/mock_examples`.
    ///
    /// If the provided path doesn't have `_base/` and `_fixtures/` subdirectories,
    /// automatically tries `<path>/modules/api-reference/examples` (for docs repo structure).
    private final Path examplesFolder;

    @Getter(AccessLevel.NONE)
    private final Path execEnvTemplatesFolder;

    /// Set by `--clean` flag or `CLEAN` env var (default: false).
    ///
    /// When true, deletes `.dh_temp/` after all tests complete.
    private final boolean clean;
    /// Set by `--bail` flag or `BAIL` env var (default: false).
    ///
    /// When true, stops execution upon the first failure.
    private final boolean bail;

    /// Returns `resources/environments/<language>/` containing the base project structure.
    ///
    /// For example, `typescript/` contains `package.json`, `java/` contains `build.gradle`.
    /// These are copied to `.dh_temp/execution_environments/<language>/` at runtime.
    public Path executionEnvironmentTemplate(ClientLanguage lang) {
        return execEnvTemplatesFolder.resolve(lang.name().toLowerCase());
    }

    public BaseScriptRunnerCtx(BaseScriptRunnerArgs<?> args, CommandSpec spec) {
        super(args, spec, ConnectionInfo.fromFlagsOrEnv(spec.commandLine(), args));
        this.examplesFolder = args.$examplesFolder.resolve();
        this.execEnvTemplatesFolder = CLI_DIR.resolve("resources/environments/");
        this.clean = args.$clean;
        this.bail = args.$bail.unwrap();
    }

    @Override
    @MustBeInvokedByOverriders
    protected Set<Function<BaseCtx, ExternalProgram>> requiredPrograms() {
        return new HashSet<>(super.requiredPrograms()) {{
            add(ExternalPrograms::bash);
            add(ExternalPrograms::tsx);
        }};
    }

    protected ClientDriver mkDriverForLanguage(CommandLine cmd, ClientLanguage lang, BaseScriptRunnerArgs<?> args) {
        val usesArtifact = lang.defaultArtifact() != null;

        val resolvedArtifact = languageOverride(lang, args.$artifactOverrides, "ARTIFACT");

        if (resolvedArtifact.isPresent() && !usesArtifact) {
            throw new ParameterException(cmd, lang.name() + " does not support artifact overrides.");
        }

        return lang.mkDocsDriver().apply(resolvedArtifact.orElse(lang.defaultArtifact()));
    }
}
