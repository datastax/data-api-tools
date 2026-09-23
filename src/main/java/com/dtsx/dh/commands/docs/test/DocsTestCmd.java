package com.dtsx.dh.commands.docs.test;

import com.dtsx.dh.commands.BaseCmd;
import com.dtsx.dh.lib.CliLogger;
import com.dtsx.dh.core.docs.planner.TestPlanBuilder;
import com.dtsx.dh.core.docs.runner.tests.TestRunner;
import lombok.Getter;
import lombok.val;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(
    name = "test",
    description = "Run script files and verify them."
)
public class DocsTestCmd extends BaseCmd<DocsTestCtx> {
    @Mixin @Getter
    private DocsTestArgs $args;

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
