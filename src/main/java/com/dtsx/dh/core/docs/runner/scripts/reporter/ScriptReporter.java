package com.dtsx.dh.core.docs.runner.scripts.reporter;

import com.dtsx.dh.lib.ExternalPrograms.RunResult;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ScriptReporter {
    public abstract void printResult(String scriptName, RunResult result);
    public abstract void printBailMessage();
}
