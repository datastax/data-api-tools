package com.dtsx.dh.core.docs.planner.fixtures;

import com.dtsx.dh.commands.docs.test.DocsTestCtx;
import com.dtsx.dh.core.docs.planner.fixtures.BaseFixturePool.FixtureIndex;
import com.dtsx.dh.core.docs.runner.PlaceholderResolver;
import com.dtsx.dh.core.common.CliException;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.lib.CliLogger;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.dh.lib.ExternalPrograms.RunResult;
import com.dtsx.dh.lib.ExternalPrograms.StderrLine;
import com.dtsx.dh.lib.JacksonUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
public final class JSFixtureImpl extends JSFixture {
    private final DocsTestCtx ctx;
    private final Path path;
    private final boolean dryRun;

    @Override
    public String fixtureName() {
        return path.getFileName().toString();
    }

    @Override
    public FixtureMetadata meta(ExternalProgram tsx, FixtureIndex index) {
        val emptyMd = FixtureMetadata.emptyForIndex(index);

        val maybeOutput = tryCallJsFunction(tsx, emptyMd, "Meta", null);

        if (maybeOutput.isEmpty()) {
            return emptyMd;
        }

        val output = maybeOutput.get().stdout();

        try {
            return JacksonUtils.parseJson(output, FixtureMetadata.class).withIndex(index);
        } catch (Exception e) {
            throw new CliException("Failed to parse fixture metadata JSON from " + path + ":\n" + output, e);
        }
    }

    @Override
    public void setup(ExternalProgram tsx, FixtureMetadata md) {
        if (!dryRun) {
            tryCallJsFunction(tsx, md, "Setup", null);
        }
    }

    @Override
    public void beforeEach(ExternalProgram tsx, FixtureMetadata md, @Nullable ClientLanguage lang) {
        if (!dryRun) {
            tryCallJsFunction(tsx, md, "BeforeEach", lang);
        }
    }

    @Override
    public void afterEach(ExternalProgram tsx, FixtureMetadata md, @Nullable ClientLanguage lang) {
        if (!dryRun) {
            tryCallJsFunction(tsx, md, "AfterEach", lang);
        }
    }

    @Override
    public void teardown(ExternalProgram tsx, FixtureMetadata md) {
        if (!dryRun) {
            tryCallJsFunction(tsx, md, "Teardown", null);
        }
    }

    private final Set<String> nonexistentFunctions = new HashSet<>();

    @SneakyThrows
    private Optional<RunResult> tryCallJsFunction(ExternalProgram tsx, FixtureMetadata md, String function, @Nullable ClientLanguage lang) {
        if (nonexistentFunctions.contains(function)) {
            return Optional.empty();
        }

        if (!Files.readString(path).contains(function)) {
            nonexistentFunctions.add(function);
            return Optional.empty();
        }

        val displayPath = ctx.examplesFolder().relativize(path);

        val envVars = PlaceholderResolver.mkEnvVars(ctx, md, Optional.ofNullable(lang));
        envVars.put("NAME_ROOT", md.index().toNameRoot());

        val code = """
          import * as m from '%s';
        
          const fn = m.%s;
        
          (async () => {
            if (fn) {
              console.log(await fn());
            } else {
              console.error("function_not_found");
            }
          })();
        """.formatted(path.toAbsolutePath(), function);

        // Calls the function if it exists
        val res = CliLogger.loading("Calling @!%s!@ in @!%s!@".formatted(function, displayPath), (_) -> {
            return tsx.run(null, envVars, "-e", code);
        });

        if (res.exitCode() != 0) {
            throw new CliException("Failed to call " + function + " in " + path + ":\nSTDOUT:\n" + res.stdout() + "\nSTDERR:\n" + res.stderr());
        }

        if (res.stdout().contains("function_not_found")) {
            nonexistentFunctions.add(function);
            return Optional.empty();
        }

        for (val line : res.outputLines()) {
            if (line instanceof StderrLine(String unwrap)) {
                CliLogger.debug("[%s/%s] %s".formatted(displayPath, function, unwrap));
            }
        }

        return Optional.of(res);
    }
}
