package com.dtsx.docs.commands.docs.test;

import com.dtsx.docs.commands.BaseCmd;
import com.dtsx.docs.lib.CliLogger;
import com.dtsx.docs.core.planner.TestPlanBuilder;
import com.dtsx.docs.core.runner.tests.TestRunner;
import lombok.Getter;
import lombok.val;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(
    name = "test",
    description = "Run script files and verify them."
)
public class TestCmd extends BaseCmd<TestCtx> {
    @Mixin @Getter
    private TestArgs $args;

    private static final int EXIT_CODE_TESTS_FAILED = 100;

    @Override
    public int run() {
        CliLogger.println(false, "@|bold Starting verifier in @!" + ctx.verifyMode().displayName() + "!@ mode.|@");
        CliLogger.println(false);
        CliLogger.println(false, "@|bold View logs:|@");
        CliLogger.println(false, "@!$!@ open " + CliLogger.logFilePath(ctx));
        CliLogger.println(false);

        val ok = TestRunner.runTests(ctx, TestPlanBuilder.buildPlan(ctx));

        return (ok) ? 0 : EXIT_CODE_TESTS_FAILED;
    }
}
