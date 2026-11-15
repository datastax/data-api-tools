package com.dtsx.dh.core.docs.runner.drivers.impls;

import com.dtsx.dh.config.ctx.BaseCtx;
import com.dtsx.dh.config.ctx.BaseScriptRunnerCtx;
import com.dtsx.dh.core.docs.planner.meta.snapshot.meta.OutputJsonifySourceMeta;
import com.dtsx.dh.core.docs.runner.ExecutionEnvironment;
import com.dtsx.dh.core.docs.runner.ExecutionEnvironment.TestFileModifierFlags;
import com.dtsx.dh.core.common.CliException;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.core.docs.runner.tests.snapshots.reducers.CSharpSnapshotsReducer;
import com.dtsx.dh.core.docs.runner.tests.snapshots.reducers.SnapshotsReducer;
import com.dtsx.dh.lib.ExternalPrograms;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.dh.lib.ExternalPrograms.RunResult;
import com.dtsx.dh.lib.JacksonUtils;
import lombok.val;
import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class CSharpDriver extends ClientDriver {
    public CSharpDriver(String artifact) {
        super(artifact);
    }

    @Override
    public ClientLanguage language() {
        return ClientLanguage.CSHARP;
    }

    @Override
    public List<Function<BaseCtx, ExternalProgram>> requiredPrograms() {
        return List.of(ExternalPrograms::dotnet);
    }

    @Override
    public Path setupExecutionEnvironment(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        replaceArtifactPlaceholder(execEnv, "Example.csproj");

        val dotnet = ExternalPrograms.dotnet(ctx);

        val restore = dotnet.run(execEnv.envDir(), "restore");
        if (restore.notOk()) {
            throw new CliException("Failed to restore C# dependencies:\n" + restore.output());
        }

        return execEnv.envDir().resolve("Example.cs");
    }

    @Override
    public String preprocessScript(BaseScriptRunnerCtx ignoredCtx, String content, @TestFileModifierFlags int mods) {
        val mainIndex = content.indexOf("Main(");

        if (mainIndex == -1) {
            throw new CliException("Main method not found in C# script");
        }

        val classIndex = content.lastIndexOf("\npublic class ", mainIndex);

        if (classIndex == -1) {
            throw new CliException("Public class declaration not found before Main method in C# script");
        }

        val nameStart = classIndex + "\npublic class ".length();
        var nameEnd = nameStart;

        while (nameEnd < content.length() && Character.isLetterOrDigit(content.charAt(nameEnd))) {
            nameEnd++;
        }

        return content.substring(0, nameStart)
            + "Example"
            + content.substring(nameEnd);
    }

    @Override
    public List<?> preprocessToJson(BaseScriptRunnerCtx ctx, OutputJsonifySourceMeta meta, String content) {
        return JacksonUtils.parseJsonLines(content, Object.class);
    }

    @Override
    public RunResult compileScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        return ExternalPrograms.dotnet(ctx).run(execEnv.envDir(), "build");
    }

    @Override
    public RunResult executeScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv, Map<String, String> envVars) {
        return ExternalPrograms.dotnet(ctx).run(execEnv.envDir(), envVars, "run");
    }

    @Override
    public Optional<String> extractClientVersion(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        val result = ExternalPrograms.dotnet(ctx).run(execEnv.envDir(), "list", "package", "--format", "json");
        
        if (result.notOk()) {
            throw new CliException("Failed to extract C# client version: " + result.output());
        }

        try {
            val json = JacksonUtils.parseJson(result.stdout(), ObjectNode.class);
            val topLevelPackages = json.get("projects").get(0).get("frameworks").get(0).get("topLevelPackages");
            
            for (val pkg : topLevelPackages) {
                if ("DataStax.AstraDB.DataApi".equals(pkg.get("id").asString())) {
                    return Optional.of("v" + pkg.get("resolvedVersion").asString());
                }
            }
        } catch (Exception _) {
            return Optional.of("local");
        }

        return Optional.of("unknown");
    }

    @Override
    public SnapshotsReducer snapshotsReducer() {
        return CSharpSnapshotsReducer.INSTANCE;
    }
}
