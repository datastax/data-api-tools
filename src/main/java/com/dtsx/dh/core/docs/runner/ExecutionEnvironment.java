package com.dtsx.dh.core.docs.runner;

import com.dtsx.dh.config.ctx.BaseScriptRunnerCtx;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;
import com.dtsx.dh.lib.CliLogger;
import com.dtsx.dh.lib.CliLogger.MessageUpdater;
import lombok.*;
import org.apache.commons.io.file.PathUtils;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/// An isolated execution environment for running example code in a specific client language.
///
/// Each environment is a temporary directory containing:
/// - Language-specific project structure (e.g., `build.gradle` for Java, `package.json` for TypeScript)
/// - Installed dependencies
/// - A location for the test file to be copied and executed
///
/// Example structure:
/// ```text
/// .dh_temp/execution_environments/
///   typescript/
///     package.json
///     node_modules/
///     example.ts      <- test file copied here
///   java/
///     build.gradle
///     src/main/java/
///       Example.java  <- test file copied here
/// ```
///
/// The environment handles:
/// - Dependency installation
/// - Copying and pre-processing test files based on the {@linkplain ClientDriver client language}
/// - Cleanup after test execution
///
/// The files themselves are derived from a template directory at `./resources/environments/` containing
/// the base structure for each language.
///
/// @see ExecutionEnvironments
/// @see ClientDriver
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ExecutionEnvironment {
    private final BaseScriptRunnerCtx ctx;
    private final Path execEnvPath;

    @With(AccessLevel.PRIVATE)
    private final Path testFileCopyPath;

    /// Does what it says on the tin ([ExecutionEnvironment])
    ///
    /// Example usage:
    /// ```java
    /// try (val execEnvs = ExecutionEnvironment.setup(ctx, drivers)) {
    ///     useItHere(execEnvs);
    /// }
    /// ```
    ///
    /// @param ctx the verifier context
    /// @param drivers the client drivers to create environments for
    /// @return a collection of execution environments, one per language
    public static ExecutionEnvironments setup(BaseScriptRunnerCtx ctx, Collection<ClientDriver> drivers, @Nullable Runnable extraSetup) {
        return new Builder(ctx).setup(drivers, Optional.ofNullable(extraSetup).orElse(() -> {}));
    }

    public static class TestFileModifiers {
        public static final int NONE = 0;
        public static final int JSONIFY_OUTPUT = 1;
    }

    @MagicConstant(flagsFromClass = TestFileModifiers.class)
    public @interface TestFileModifierFlags {}

    /// Copies a test file into the execution environment, runs the test, then cleans up.
    ///
    /// The test file is preprocessed to replace placeholders (e.g., `{{API_ENDPOINT}}`)
    /// and apply language-specific transformations before execution.
    ///
    /// Example usage:
    /// ```java
    /// val result = execEnv.withTestFileCopied(driver, sourceFile, () -> {
    ///     return insertTestLogicHere();
    /// });
    /// ```
    ///
    /// @param driver the client driver for the test
    /// @param sourceFile the original test file to copy
    /// @param test the test to run with the copied file
    /// @return the result of the test execution
    public <T> T withTestFileCopied(ClientDriver driver, Path sourceFile, Placeholders placeholders, @TestFileModifierFlags int mods, Supplier<T> test) {
        val testFile = setupFileForTesting(driver, sourceFile, placeholders, mods);
        try {
            return test.get();
        } finally {
            cleanupFileAfterTesting(testFile);
        }
    }

    /// Returns the absolute path to the language's execution environment directory.
    ///
    /// @return the environment directory path
    public Path envDir() {
        return execEnvPath.toAbsolutePath();
    }

    /// Returns the absolute path to the copied test script file.
    ///
    /// @return the test script path
    public String scriptPath() {
        return testFileCopyPath.toAbsolutePath().toString();
    }

    @SneakyThrows
    private Path setupFileForTesting(ClientDriver driver, Path sourceFile, Placeholders placeholders, @TestFileModifierFlags int mods) {
        var content = Files.readString(sourceFile);
        content = PlaceholderResolver.replacePlaceholders(ctx, placeholders, driver.language(), content);
        content = driver.preprocessScript(ctx, content, mods);

        Files.createDirectories(testFileCopyPath.getParent());
        Files.writeString(testFileCopyPath, content);
        return testFileCopyPath;
    }

    @SneakyThrows
    private void cleanupFileAfterTesting(Path file) {
        Files.deleteIfExists(file);
    }

    /// Internal builder for setting up execution environments.
    ///
    /// Setup process:
    /// 1. Creates root folder at `.dh_temp/execution_environments/`
    /// 2. Installs JavaScript dependencies (for fixtures)
    ///    - Creating `package.json` if necessary
    /// 3. For each client language:
    ///    - Copies template from `resources/environments/<language>/`
    ///    - Lets the driver set up language-specific dependencies
    ///    - Determines where test files will be copied
    @RequiredArgsConstructor
    private static class Builder {
        private final BaseScriptRunnerCtx ctx;

        public ExecutionEnvironments setup(Collection<ClientDriver> drivers, Runnable extraSetup) {
            val rootDir = mkRootFolder();
            extraSetup.run();
            return mkExecEnvs(drivers, rootDir);
        }

        private Path mkRootFolder() {
            val rootFolder = ctx.tmpFolder().resolve("execution_environments");

            try {
                Files.createDirectories(rootFolder);
                return rootFolder;
            } catch (Exception e) {
                throw new RunException("Failed to create execution environments root folder", e);
            }
        }

        private ExecutionEnvironments mkExecEnvs(Collection<ClientDriver> drivers, Path rootDir) {
            CliLogger.println(true, "@|bold Setting up execution environments...|@");

            val execEnvs = drivers.stream().collect(Collectors.toMap(
                ClientDriver::language,
                (driver) -> mkExecEnv(rootDir, driver)
            ));

            CliLogger.println(true);
            return new ExecutionEnvironments(execEnvs);
        }

        private ExecutionEnvironment mkExecEnv(Path rootDir, ClientDriver driver) {
            val languageName = driver.language().name().toLowerCase();

            return CliLogger.loading("Setting up @!%s!@ execution environment".formatted(languageName), (msgUpdater) -> {
                val srcExecEnv = ctx.executionEnvironmentTemplate(driver.language());
                val destExecEnv = rootDir.resolve(driver.language().name().toLowerCase());

                val execEnv = new ExecutionEnvironment(ctx, destExecEnv, null);
                cleanIfNeeded(srcExecEnv, destExecEnv, msgUpdater);

                try {
                    if (!Files.exists(destExecEnv)) {
                        CliLogger.debug("Setting up %s execution environment from template".formatted(languageName));
                        Files.createDirectories(destExecEnv);
                        PathUtils.copyDirectory(srcExecEnv, destExecEnv);
                    }
                } catch (Exception e) {
                    throw new RunException("Failed to setup " + languageName + " execution environment", e);
                }

                val testFileCopyPath = driver.setupExecutionEnvironment(ctx, execEnv);

                val clientVersion = driver.extractClientVersion(ctx, execEnv)
                    .map(version -> " @|faint (client " + version + ")|@")
                    .orElse("");

                CliLogger.println(true, "@!✓!@ " + languageName + clientVersion);

                return execEnv.withTestFileCopyPath(testFileCopyPath);
            });
        }

        @SneakyThrows
        private void cleanIfNeeded(Path srcExecEnv, Path destExecEnv, MessageUpdater msgUpdater) {
            if (ctx.clean() || needsCleaning(srcExecEnv, destExecEnv)) {
                CliLogger.debug("Cleaning up execution environments");

                if (Files.exists(destExecEnv)) {
                    msgUpdater.update(m -> m + " (after cleanup)");
                    PathUtils.deleteDirectory(destExecEnv);
                }
            }
        }

        @SuppressWarnings("resource")
        private boolean needsCleaning(Path srcExecEnv, Path destExecEnv) {
            if (!Files.exists(destExecEnv)) {
                return false;
            }

            try {
                val srcFiles = Files.walk(srcExecEnv).filter(Files::isRegularFile).map(srcExecEnv::relativize).collect(Collectors.toSet());
                val destFiles = Files.walk(destExecEnv).filter(Files::isRegularFile).map(destExecEnv::relativize).collect(Collectors.toSet());

                return !destFiles.containsAll(srcFiles);
            } catch (Exception e) {
                throw new RunException("Failed to check if execution environment needs cleaning", e);
            }
        }
    }
}
