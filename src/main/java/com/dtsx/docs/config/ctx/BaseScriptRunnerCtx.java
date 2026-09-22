package com.dtsx.docs.config.ctx;

import com.dtsx.docs.config.ArgUtils;
import com.dtsx.docs.config.ConnectionInfo;
import com.dtsx.docs.config.args.BaseScriptRunnerArgs;
import com.dtsx.docs.core.runner.drivers.ClientDriver;
import com.dtsx.docs.core.runner.drivers.ClientLanguage;
import com.dtsx.docs.lib.ExternalPrograms;
import com.dtsx.docs.lib.ExternalPrograms.ExternalProgram;
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

import static com.dtsx.docs.HelperCli.CLI_DIR;

@Getter
public abstract class BaseScriptRunnerCtx extends BaseCtx {
    /// Resolved from `EXAMPLES_FOLDER` env var, with fallback to `./resources/mock_examples`.
    ///
    /// If the provided path doesn't have `_base/` and `_fixtures/` subdirectories,
    /// automatically tries `<path>/modules/api-reference/examples` (for docs repo structure).
    private final Path examplesFolder;

    /// Parsed from `-t`/`--astra-token` flag or `ASTRA_TOKEN` env var, and `-e`/`--api-endpoint` flag or `API_ENDPOINT` env var.
    ///
    /// The token must have read/write access to the test database, and the endpoint
    /// determines whether we're testing against a collection or table API.
    private final ConnectionInfo connectionInfo;

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
        super(args, spec);
        this.examplesFolder = args.$examplesFolder.resolve();
        this.connectionInfo = mkConnectionInfo(cmd, args);
        this.execEnvTemplatesFolder = CLI_DIR.resolve("resources/environments/");
        this.clean = args.$clean;
        this.bail = args.$bail;
    }

    @Override
    @MustBeInvokedByOverriders
    protected Set<Function<BaseCtx, ExternalProgram>> requiredPrograms() {
        return new HashSet<>(super.requiredPrograms()) {{
            add(ExternalPrograms::bash);
            add(ExternalPrograms::tsx);
        }};
    }

    private ConnectionInfo mkConnectionInfo(CommandLine cmd, BaseScriptRunnerArgs<?> args) {
        val token = ArgUtils.requireFlag(cmd, args.$token, "astra token", "-t", "ASTRA_TOKEN");
        val apiEndpoint = ArgUtils.requireFlag(cmd, args.$apiEndpoint, "API endpoint", "-e", "API_ENDPOINT");
        return new ConnectionInfo(token, apiEndpoint);
    }

    protected ClientDriver mkDriverForLanguage(CommandLine cmd, ClientLanguage lang, BaseScriptRunnerArgs<?> args) {
        val usesArtifact = lang.defaultArtifact() != null;

        val envVarName = lang.name().toUpperCase() + "_ARTIFACT";

        val resolvedArtifact = Optional.ofNullable(args.$artifactOverrides.get(lang))
            .or(() -> Optional.ofNullable(System.getProperty(envVarName)));

        if (resolvedArtifact.isPresent() && !usesArtifact) {
            throw new ParameterException(cmd, lang.name() + " does not support artifact overrides.");
        }

        return lang.mkDriver().apply(resolvedArtifact.orElse(lang.defaultArtifact()));
    }
}
