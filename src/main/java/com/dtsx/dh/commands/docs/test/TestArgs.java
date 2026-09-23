package com.dtsx.dh.commands.docs.test;

import com.dtsx.dh.config.args.BaseScriptRunnerArgs;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;
import com.dtsx.dh.core.docs.runner.tests.VerifyMode;
import com.dtsx.dh.core.docs.runner.tests.reporter.TestReporters;
import lombok.ToString;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.List;

@ToString
public class TestArgs extends BaseScriptRunnerArgs<TestCtx> {
    @Parameters(
        description = "Client drivers to use (e.g., 'java', 'typescript').",
        defaultValue = "${CLIENT_DRIVERS}",
        completionCandidates = ClientDriver.Completions.class,
        paramLabel = "DRIVER",
        split = ","
    )
    public List<String> $drivers;

    @Option(
        names = { "-r", "--test-reporter" },
        description = "Test reporter type (e.g., 'only_failures', 'all_tests').",
        defaultValue = "${TEST_REPORTER:-all_tests}",
        paramLabel = "TYPE"
    )
    public TestReporters $reporter;

    @Option(
        names = { "-m", "--verify-mode" },
        description = "Verification mode to use (normal, verify_only, compile_only, dry_run).",
        defaultValue = "${VERIFY_MODE:-normal}",
        paramLabel = "MODE"
    )
    public VerifyMode $verifyMode;

    @Option(
        names = { "-D", "--dry-run" },
        description = "Short for `--verify-mode dry_run`."
    )
    public boolean $dryRun;

    @Option(
        names = { "-f", "--filter" },
        description = "Comma-separated regex filters to select specific tests to run. Can be used multiple times.",
        defaultValue = "${FILTERS:-}",
        paramLabel = "FILTER",
        split = ","
    )
    public List<String> $filters;

    @Option(
        names = { "-F", "--filter-not" },
        description = "Comma-separated regex filters to select specific tests to exclude. Can be used multiple times.",
        defaultValue = "${INVERSE_FILTERS:-}",
        paramLabel = "FILTER",
        split = ","
    )
    public List<String> $inverseFilters;

    @Option(
        names = { "-fand" },
        description = "Changes filters to be ANDed instead of ORed (default)."
    )
    public boolean $fand;

    @Option(
        names = { "-n", "--max-fixture-instances" },
        description = "Maximum number of base fixture instances to create for isolated execution.",
        defaultValue = "5",
        paramLabel = "N"
    )
    public int $maxFixtureInstances;

    @Override
    public TestCtx toCtx(CommandSpec spec) {
        return new TestCtx(this, spec);
    }
}
