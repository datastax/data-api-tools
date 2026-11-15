package com.dtsx.dh.core.docs.runner.tests;

import com.dtsx.dh.commands.docs.test.DocsTestCtx;
import com.dtsx.dh.core.common.CliException;
import com.dtsx.dh.core.docs.runner.tests.strategies.test.CompilesTestStrategy;
import org.approvaltests.core.Options;
import org.approvaltests.inline.InlineOptions;
import org.lambda.functions.Function1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/// Verification mode controlling how test output is compared against snapshots using ApprovalTests.
///
/// Modes:
/// - `NORMAL` - Standard ApprovalTests behavior: creates a "diff" file on mismatch
/// - `VERIFY_ONLY` - Read-only mode: compares against approved files but does not create "diff" files
/// - `DRY_RUN` - Prints what would run without executing anything
public enum VerifyMode {
    /// Compares the snapshot output against approved files on disk, creating a "diff" file if they differ.
    ///
    /// Only works against a single language at a time.
    NORMAL,

    /// Compares the snapshot output against approved files on disk, but does not create a "diff" file on mismatch (read-only mode).
    ///
    /// Intended for multi-language test runs creating multiple diff files in a single run is cumbersome.
    VERIFY_ONLY,

    /// Prints what would run without executing tests or verifying snapshots.
    ///
    /// Useful for debugging test discovery and planning.
    ///
    /// This also stops {@linkplain com.dtsx.dh.core.docs.planner.fixtures.JSFixture fixtures} from running, but still sets up the {@linkplain com.dtsx.dh.core.docs.runner.ExecutionEnvironment execution environment}.
    DRY_RUN,

    /// Skips compile-only tests
    NO_COMPILE_ONLY,

    /// Forces tests to use {@link CompilesTestStrategy CompilesTestStrategy}.
    ///
    /// Dependent on the invariant that all snapshot tests can be run as compilation tests.
    COMPILE_ONLY;

    private static final InlineOptions INLINE_OPTIONS = InlineOptions.showCode(false);

    /// Applies verification options based on the mode.
    ///
    /// @param ctx the verifier context
    /// @param approvedFile the path to the approved snapshot file
    /// @return a function that configures ApprovalTests options
    /// @throws CliException if mode is invalid for the configuration
    public Function1<Options, Options> applyOptions(DocsTestCtx ctx, Path approvedFile) {
        return switch (ctx.verifyMode()) {
            case NORMAL, NO_COMPILE_ONLY -> {
                yield (o) -> o;
            }
            case VERIFY_ONLY -> (o) -> {
                if (!Files.exists(approvedFile)) {
                    return o.inline("", INLINE_OPTIONS);
                }

                try {
                    return o.inline(Files.readString(approvedFile), INLINE_OPTIONS);
                } catch (IOException e) {
                    throw new CliException("Failed to read example file for VERIFY_ONLY verification mode: " + approvedFile, e);
                }
            };
            case DRY_RUN -> {
                throw new CliException("DRY_RUN mode should not apply verification options; it should've skipped verification entirely.");
            }
            case COMPILE_ONLY -> {
                throw new CliException("COMPILE_ONLY mode should not apply verification options; it should've used a different test strategy.");
            }
        };
    }

    public String displayName() {
        return name().toLowerCase().replace("_", " ");
    }
}
