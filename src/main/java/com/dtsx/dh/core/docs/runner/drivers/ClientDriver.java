package com.dtsx.dh.core.docs.runner.drivers;

import com.dtsx.dh.config.ctx.BaseCtx;
import com.dtsx.dh.config.ctx.BaseScriptRunnerCtx;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.core.common.CliException;
import com.dtsx.dh.core.docs.planner.meta.snapshot.meta.OutputJsonifySourceMeta;
import com.dtsx.dh.core.docs.runner.ExecutionEnvironment;
import com.dtsx.dh.core.docs.runner.ExecutionEnvironment.TestFileModifierFlags;
import com.dtsx.dh.core.docs.runner.drivers.impls.*;
import com.dtsx.dh.core.docs.runner.tests.snapshots.reducers.DefaultSnapshotsReducer;
import com.dtsx.dh.core.docs.runner.tests.snapshots.reducers.SnapshotsReducer;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.dh.lib.ExternalPrograms.RunResult;
import lombok.AllArgsConstructor;
import lombok.val;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

///  A pluggable driver for setting up and running clients of different languages.
///
/// Working with [ExecutionEnvironment], each driver can:
/// - Has external programs it depends on (e.g., npm, java, python)
/// - Set up its execution environment + dependencies (e.g., `npm install`, `./gradlew build`)
/// - Preprocess test scripts (e.g., add imports, replace placeholders)
/// - Execute test code and capture output
///
/// Implementations:
/// - [TypeScriptDriver] - Runs TypeScript via `tsx`, using `npm` for dependencies
/// - [JavaDriver] - Compiles and runs Java via Gradle
/// - [PythonDriver] - Runs Python scripts, using `pip` via `python` in a venv for dependencies
/// - [BashDriver] - Runs bash scripts directly
/// - [CSharpDriver] - Runs C# scripts via `dotnet@9`
/// - [GoDriver] - TODO
///
/// @see ClientLanguage
/// @see ExecutionEnvironment
@AllArgsConstructor
public abstract class ClientDriver {
    /// The "artifact" representing the client library to install/use. `null` for drivers which don't use a client library (i.e. bash).
    ///
    /// This can be whatever the language allows to identify packages. For example,
    /// - TypeScript: npm package name or path to a tarball
    /// - Java: Maven artifact coordinates
    /// - etc.
    private final String artifact;

    /// Returns the language this driver handles.
    ///
    /// @return the client language (e.g., {@link ClientLanguage#TYPESCRIPT TYPESCRIPT}, {@link ClientLanguage#JAVA JAVA}, etc.)
    public abstract ClientLanguage language();

    /// Returns the {@linkplain com.dtsx.dh.lib.ExternalPrograms external programs} required to run this driver.
    ///
    /// For example:
    /// - TypeScript requires `npm` and `tsx`
    /// - Java requires `java` (Gradle is bundled via `gradlew`)
    /// - Python requires `python3`
    /// - etc.
    ///
    /// These are checked at startup to ensure they're installed.
    ///
    /// @return list of required {@linkplain com.dtsx.dh.lib.ExternalPrograms external programs}
    public abstract List<Function<BaseCtx, ExternalProgram>> requiredPrograms();

    /// Sets up the {@linkplain com.dtsx.dh.core.docs.runner.ExecutionEnvironment execution environment} by installing dependencies and preparing the project.
    ///
    /// For example:
    /// - TypeScript: Runs `npm install <artifact>` to install the client library
    /// - Java: Updates `build.gradle` with the artifact version, then runs `./gradlew build`
    /// - etc.
    ///
    /// @param ctx the verifier context
    /// @param execEnv the execution environment to set up
    /// @return the path where test files should be copied (e.g., `main.ts`, `src/main/java/Main.java`)
    /// @throws CliException if setup fails
    public abstract Path setupExecutionEnvironment(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv);

    /// Preprocesses the test script before execution if necessary (e.g. adding imports, prelude code, etc.).
    ///
    /// @param ctx     the verifier context
    /// @param content the original script content
    /// @param mods
    /// @return the preprocessed script content
    public abstract String preprocessScript(BaseScriptRunnerCtx ctx, String content, @TestFileModifierFlags int mods);

    /// Interprets the test script output into JSON for snapshotting.
    ///
    /// @param ctx     the verifier context
    /// @param meta    the snapshot source's meta information
    /// @param content the original script output content
    /// @return the content as JSON
    public abstract List<?> preprocessToJson(BaseScriptRunnerCtx ctx, OutputJsonifySourceMeta meta, String content);

    /// Executes the test script and returns the result.
    ///
    /// @param ctx the verifier context
    /// @param execEnv the execution environment containing the test script
    /// @return the execution result with exit code and output
    public abstract RunResult compileScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv);

    /// Executes the test script and returns the result.
    ///
    /// @param ctx the verifier context
    /// @param execEnv the execution environment containing the test script
    /// @return the execution result with exit code and output
    public abstract RunResult executeScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv, Map<String, String> envVars);

    /// Extracts the client library version from the execution environment, if applicable.
    public abstract Optional<String> extractClientVersion(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv);

    /// The [SnapshotsReducer] used to reconcile multiple snapshots recorded for this driver's
    /// language into one.
    public SnapshotsReducer snapshotsReducer() {
        return DefaultSnapshotsReducer.INSTANCE;
    }

    protected final void replaceArtifactPlaceholder(ExecutionEnvironment execEnv, String file) {
        val path = execEnv.envDir().resolve(file);

        try {
            val content = Files.readString(path);
            val updatedContent = content.replace("${CLIENT_ARTIFACT}", artifact());
            Files.writeString(path, updatedContent);
        } catch (Exception e) {
            throw new CliException("Failed to update " + file + " with client version", e);
        }
    }

    protected final String artifact() {
        if (artifact == null) {
            throw new CliException("Attempted to access artifact for driver that does not use one: " + language() + ". Did *someone* forget to set a default artifact in ClientLanguage?");
        }
        return artifact;
    }
}
