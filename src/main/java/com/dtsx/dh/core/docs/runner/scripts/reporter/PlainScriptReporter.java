package com.dtsx.dh.core.docs.runner.scripts.reporter;

import com.dtsx.dh.lib.CliLogger;
import com.dtsx.dh.lib.ExternalPrograms.RunResult;

public class PlainScriptReporter extends ScriptReporter {
    @Override
    public void printResult(String scriptName, RunResult result) {
        CliLogger.debug("Script '" + scriptName + "' finished with exit code " + result.exitCode());
        CliLogger.println(true, result.output());
    }

    @Override
    public void printBailMessage() {
        // noop
    }
}
