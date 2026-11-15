package com.dtsx.dh.core.docs.runner.drivers.impls;

import com.dtsx.dh.config.ctx.BaseCtx;
import com.dtsx.dh.config.ctx.BaseScriptRunnerCtx;
import com.dtsx.dh.core.docs.planner.meta.snapshot.meta.OutputJsonifySourceMeta;
import com.dtsx.dh.core.docs.runner.ExecutionEnvironment;
import com.dtsx.dh.core.docs.runner.ExecutionEnvironment.TestFileModifierFlags;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.lib.ExternalPrograms;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.dh.lib.ExternalPrograms.RunResult;
import com.dtsx.dh.lib.JacksonUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.dtsx.dh.lib.JacksonUtils.runJq;

public class BashDriver extends ClientDriver {
    public BashDriver(String artifact) {
        super(artifact);
    }

    @Override
    public ClientLanguage language() {
        return ClientLanguage.BASH;
    }

    @Override
    public List<Function<BaseCtx, ExternalProgram>> requiredPrograms() {
        return List.of(ExternalPrograms::bash);
    }

    @Override
    public Path setupExecutionEnvironment(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        return execEnv.envDir().resolve("example.sh");
    }

    @Override
    public String preprocessScript(BaseScriptRunnerCtx ignoredCtx, String content, @TestFileModifierFlags int mods) {
        return "#!/bin/bash\n\nset -euo pipefail\n\n" + content;
    }

    @Override
    public List<?> preprocessToJson(BaseScriptRunnerCtx ctx, OutputJsonifySourceMeta meta, String content) {
        if (meta.jqBash().isPresent()) {
            content = runJq(ctx, content, meta.jqBash().get());
        }
        return JacksonUtils.parseJsonLines(content, Object.class);
    }

    @Override
    public RunResult compileScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        return ExternalPrograms.bash(ctx).run(execEnv.envDir(), "-n", execEnv.scriptPath());
    }

    @Override
    public RunResult executeScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv, Map<String, String> envVars) {
        return ExternalPrograms.bash(ctx).run(execEnv.envDir(), envVars, execEnv.scriptPath());
    }

    @Override
    public Optional<String> extractClientVersion(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        return Optional.empty();
    }
}
