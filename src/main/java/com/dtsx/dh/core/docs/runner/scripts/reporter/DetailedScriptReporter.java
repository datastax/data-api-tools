package com.dtsx.dh.core.docs.runner.scripts.reporter;

import com.dtsx.dh.lib.CliLogger;
import com.dtsx.dh.lib.ExternalPrograms.RunResult;
import com.dtsx.dh.lib.ExternalPrograms.StderrLine;
import com.dtsx.dh.lib.ExternalPrograms.StdoutLine;

public class DetailedScriptReporter extends ScriptReporter {
    @Override
    public void printResult(String scriptName, RunResult result) {
        if (result.ok()) {
            printSuccessInfo(scriptName, result);
        } else {
            printFailureInfo(scriptName, result);
        }
    }

    @Override
    public void printBailMessage() {
        CliLogger.println(true, "@|red Bailing due to script failure|@");
    }

    private void printSuccessInfo(String scriptName, RunResult result) {
        CliLogger.println(true, "@|green ✓|@ @|bold Script @!" + scriptName + "!@ executed successfully.|@");
        printOutput(result);
    }

    private void printFailureInfo(String scriptName, RunResult result) {
        CliLogger.println(true, "@|red ✗|@ @|bold Script @!" + scriptName + "!@ failed with exit code @!" + result.exitCode() + "!@.|@");
        printOutput(result);
    }

    private void printOutput(RunResult result) {
        if (result.outputLines().isEmpty()) {
            CliLogger.println(true, "@!>!@ No output.");
        } else {
            result.outputLines().stream()
                .map((line) -> switch (line) {
                    case StdoutLine(var str) -> "@|green ▶|@ " + str;
                    case StderrLine(var str) -> "@|red ▶|@ " + str;
                })
                .forEach((str) -> {
                    CliLogger.println(true, stripNewline(str));
                });
        }

        CliLogger.println(false);
    }

    private String stripNewline(String str) {
        if (str.endsWith("\n")) {
            return str.substring(0, str.length() - 1);
        }
        return str;
    }
}
