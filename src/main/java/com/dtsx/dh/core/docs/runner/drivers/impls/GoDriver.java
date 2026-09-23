package com.dtsx.dh.core.docs.runner.drivers.impls;

import com.dtsx.dh.config.ctx.BaseCtx;
import com.dtsx.dh.config.ctx.BaseScriptRunnerCtx;
import com.dtsx.dh.core.docs.planner.meta.snapshot.meta.OutputJsonifySourceMeta;
import com.dtsx.dh.core.docs.runner.ExecutionEnvironment;
import com.dtsx.dh.core.docs.runner.ExecutionEnvironment.TestFileModifierFlags;
import com.dtsx.dh.core.docs.runner.ExecutionEnvironment.TestFileModifiers;
import com.dtsx.dh.core.common.CliException;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.lib.ExternalPrograms;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.dh.lib.ExternalPrograms.RunResult;
import com.dtsx.dh.lib.JacksonUtils;
import lombok.val;
import tools.jackson.databind.JsonNode;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GoDriver extends ClientDriver {
    public GoDriver(String artifact) {
        super(artifact);
    }

    @Override
    public ClientLanguage language() {
        return ClientLanguage.GO;
    }

    @Override
    public List<Function<BaseCtx, ExternalProgram>> requiredPrograms() {
        return List.of(ExternalPrograms::go);
    }

    @Override
    public Path setupExecutionEnvironment(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        val go = ExternalPrograms.go(ctx);

        val getClient = go.run(execEnv.envDir(), "get", artifact());

        if (getClient.notOk()) {
            throw new CliException("Failed to get Go client dependency:\n" + getClient.output());
        }

        return execEnv.envDir().resolve("example.go");
    }

    private static final Pattern IMPORTS_PATTERN = Pattern.compile("(?s)import\\s+\\((.*?)\\)");

    @Override
    public String preprocessScript(BaseScriptRunnerCtx ignoredCtx, String content, @TestFileModifierFlags int mods) {
        if ((mods & TestFileModifiers.JSONIFY_OUTPUT) != 0) {
            content = content.replaceFirst("(?s)^\\s*package\\s+\\w+\\s*", "");

            val matcher = IMPORTS_PATTERN.matcher(content);
            val found = matcher.find();

            if (found) {
                content = content.replace(matcher.group(0), "");
            }

            val imports = Stream.concat(
                (found) ? matcher.group(1).lines().map(String::trim) : Stream.empty(),
                Stream.of("\"fmt\"", "\"os\"")
            ).distinct();

            content = """
                package main

                import (
                    %s
                    "github.com/datastax/astra-db-go/v2/astra/serdes"
                )
            
                func _printlnJson(a ...any) (int, error) {
                    for _, v := range a {
                        jsonBytes, err := serdes.Serialize(v, serdes.TargetNone, serdes.UseJSONMarshal)
                        if err != nil {
                            fmt.Fprintln(os.Stdout, err)
                            continue
                        }
                        fmt.Fprintln(os.Stdout, string(jsonBytes))
                    }
                    return len(a), nil
                }
            
                %s
            """.formatted(
                imports.collect(Collectors.joining("\n")),
                content.replace("fmt.Println", "_printlnJson")
            );
        }

        return content;
    }

    @Override
    public List<?> preprocessToJson(BaseScriptRunnerCtx ctx, OutputJsonifySourceMeta meta, String content) {
        return JacksonUtils.parseJsonLines(content, Object.class);
    }

    @Override
    public RunResult compileScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        return ExternalPrograms.go(ctx).run(execEnv.envDir(), "build", execEnv.scriptPath());
    }

    @Override
    public RunResult executeScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv, Map<String, String> envVars) {
        return ExternalPrograms.go(ctx).run(execEnv.envDir(), envVars, "run", execEnv.scriptPath());
    }

    @Override
    public Optional<String> extractClientVersion(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        val result = ExternalPrograms.go(ctx).run(execEnv.envDir(), "list", "-m", "-json", "all");

        if (result.notOk()) {
            throw new CliException("Failed to extract Go client version: " + result.output());
        }

        try {
            val it = JacksonUtils.parseJsonLines(result.stdout(), JsonNode.class);

            for (val node : it) {
                val path = node.path("Path").asString();

                if (path.contains("astra-db-go/v2") && node.has("Version")) {
                    val version = node.path("Version").asString();

                    if (!version.isEmpty()) {
                        return Optional.of(version);
                    }
                }
            }

        } catch (Exception e) {
            return Optional.of("unknown");
        }

        return Optional.of("unknown");
    }
}
