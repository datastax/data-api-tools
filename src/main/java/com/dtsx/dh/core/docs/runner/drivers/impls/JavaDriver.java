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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

public class JavaDriver extends ClientDriver {
    public JavaDriver(String artifact) {
        super(artifact);
    }

    @Override
    public ClientLanguage language() {
        return ClientLanguage.JAVA;
    }

    @Override
    public List<Function<BaseCtx, ExternalProgram>> requiredPrograms() {
        return List.of(ExternalPrograms::java);
    }

    @Override
    public Path setupExecutionEnvironment(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        replaceArtifactPlaceholder(execEnv, "build.gradle");

        val build = ExternalPrograms.custom().run(execEnv.envDir(), "./gradlew", "build");
        if (build.notOk()) {
            throw new CliException("Failed to build Java client:\n" + build.output());
        }

        return execEnv.envDir().resolve("src/main/java/Example.java");
    }

    @Override
    public String preprocessScript(BaseScriptRunnerCtx ignoredCtx, String content, @TestFileModifierFlags int mods) {
        if ((mods & TestFileModifiers.JSONIFY_OUTPUT) != 0) {
            content = """
               import com.fasterxml.jackson.databind.json.JsonMapper;
               import com.fasterxml.jackson.annotation.JsonInclude;
               import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
               import com.dtsx.astra.sdk.utils.JsonUtils;
               import java.io.PrintStream;
            """ + content;

            val target = "public static void main(String[] args) {";
            val mainMethodIdx = content.indexOf(target);

            if (mainMethodIdx == -1) {
                throw new CliException("main method not found");
            }

            val insertPos = mainMethodIdx + target.length();

            content =
                content.substring(0, insertPos)+
                """
                System.setOut(new PrintStream(System.out) {
                    @Override
                    public void println(Object obj) {
                        try {
                            var mapper = JsonUtils.getObjectMapper()
                                .setDefaultPropertyInclusion(JsonInclude.Include.ALWAYS)
                                .registerModule(new Jdk8Module());

                            super.println(mapper.writeValueAsString(obj));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                """
                + content.substring(insertPos);
        }

        return Pattern.compile("^public\\s+class\\s+\\w+", Pattern.MULTILINE)
            .matcher(content)
            .replaceFirst("public class Example");
    }

    @Override
    public List<?> preprocessToJson(BaseScriptRunnerCtx ctx, OutputJsonifySourceMeta meta, String content) {
        return JacksonUtils.parseJsonLines(content, Object.class);
    }

    @Override
    public RunResult compileScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
        return ExternalPrograms.custom().run(execEnv.envDir(), "./gradlew", "build");
    }

    @Override
    public RunResult executeScript(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv, Map<String, String> envVars) {
        return ExternalPrograms.custom().run(execEnv.envDir(), envVars, "./gradlew", "run", "--quiet");
    }

    @Override
    public Optional<String> extractClientVersion(BaseScriptRunnerCtx ctx, ExecutionEnvironment execEnv) {
       val result = ExternalPrograms.custom().run(execEnv.envDir(), "./gradlew", "dependencies", "--configuration", "runtimeClasspath");

       if (result.notOk()) {
           throw new CliException("Failed to extract Java client version: " + result.output());
       }

       val output = result.stdout();
       val pattern = Pattern.compile("com\\.datastax\\.astra:astra-db-java:([\\d.]+(?:-[\\w.]+)?)");
       val matcher = pattern.matcher(output);

       if (matcher.find()) {
           return Optional.of("v" + matcher.group(1));
       }

       return Optional.of("unknown");
    }
}
