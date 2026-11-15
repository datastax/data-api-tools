package com.dtsx.dh.core.docs.runner.scripts;

import com.dtsx.dh.commands.docs.run.RunCtx;
import com.dtsx.dh.core.docs.runner.ExecutionEnvironment;
import com.dtsx.dh.core.docs.runner.ExecutionEnvironment.TestFileModifiers;
import com.dtsx.dh.core.docs.runner.PlaceholderResolver;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;
import com.dtsx.dh.core.docs.runner.scripts.reporter.ScriptReporter;
import com.dtsx.dh.lib.CliLogger;
import lombok.val;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ScriptRunner {
    private final RunCtx ctx;
    private final Map<ClientDriver, Set<Path>> scripts;
    private final ScriptReporter reporter;

    private ScriptRunner(RunCtx ctx) {
        this.ctx = ctx;
        this.scripts = ctx.scripts();
        this.reporter = ctx.reporter();
    }

    public static boolean runScripts(RunCtx ctx) {
        return new ScriptRunner(ctx).runAllScripts();
    }

    private boolean runAllScripts() {
        val execEnvs = ExecutionEnvironment.setup(ctx, scripts.keySet(), null);

        var allSucceeded = true;

        for (val entry : ctx.scripts().entrySet()) {
            val driver = entry.getKey();
            val scripts = entry.getValue();

            val execEnv = execEnvs.forLanguage(driver.language());

            for (val script : scripts) {
                val success = runScript(driver, execEnv, script);

                if (success) {
                    continue;
                }

                allSucceeded = false;

                if (ctx.bail()) {
                    reporter.printBailMessage();
                    return false;
                }
            }
        }

        return allSucceeded;
    }

    private boolean runScript(ClientDriver driver, ExecutionEnvironment execEnv, Path script) {
        CliLogger.debug("Running script '" + script + "'");

        val scriptName = resolveScriptDisplayName(script);

        val envVars = PlaceholderResolver.mkEnvVars(ctx, ctx.placeholders(), Optional.of(driver.language()));

        val result = execEnv.withTestFileCopied(driver, script, ctx.placeholders(), TestFileModifiers.NONE, () -> {
            return CliLogger.loading("Running @!" + scriptName + "!@", (_) -> {
                return driver.executeScript(ctx, execEnv, envVars);
            });
        });

        reporter.printResult(scriptName, result);
        return result.ok();
    }

    private String resolveScriptDisplayName(Path script) {
        return ctx.examplesFolder().relativize(script).toString(); // won't work in all cases, but I'll just use it until I notice it doesn't work right
    }
}
