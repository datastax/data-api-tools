package com.dtsx.dh.commands.clients.test;

import com.dtsx.dh.commands.BaseCmd;
import com.dtsx.dh.core.clients.ClientsPlan;
import com.dtsx.dh.core.clients.ClientsRunner;
import com.dtsx.dh.core.clients.reporter.ClientsReporter;
import lombok.Getter;
import lombok.val;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(
    name = "test",
    description = "Run each selected client's own integration test suite against a target database."
)
public class ClientsTestCmd extends BaseCmd<ClientsTestCtx> {
    @Mixin @Getter
    private ClientsTestArgs $args;

    @Override
    protected int run() {
        ClientsReporter.printHeader(ctx);

        ClientsRunner.prepareRepos(ctx);

        val plan = ClientsPlan.build(ctx);
        ClientsReporter.printPlan(ctx, plan);

        if (!ctx.yes()) {
            ClientsReporter.printDryRunNotice();
            return 0;
        }

        val ok = ClientsRunner.runSuites(ctx, plan);

        return (ok) ? 0 : EXIT_CODE_TESTS_FAILED;
    }
}
