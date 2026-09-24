package com.dtsx.dh.core.clients.impls;

import com.datastax.astra.client.DataAPIDestination;
import com.dtsx.dh.commands.clients.test.ClientsTestCtx;
import com.dtsx.dh.config.ConnectionInfo;
import com.dtsx.dh.config.ctx.BaseCtx;
import com.dtsx.dh.core.clients.ClientArtifactSpec.Remote;
import com.dtsx.dh.core.clients.ClientSuite;
import com.dtsx.dh.core.clients.SuiteCredentials;
import com.dtsx.dh.core.common.CliException;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.lib.ExternalPrograms;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import lombok.val;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/// Runs `astra-db-java`'s own integration suite (`mvn test -pl astra-db-java -am`) against a target
/// database.
///
/// Surefire runs both `*Test.java` (unit) and `*IT.java` (integration) classes under `mvn test`, so
/// integration selection is a filename exclusion (`-Dtest='!**/*Test'`), not a separate goal.
public class JavaSuite implements ClientSuite {
    /// Maps a [SuiteCredentials] provider key to the env var its matching `*Vectorize*IT` class
    /// reads, per `TestConfig.java`. Passed through whenever vectorize is on.
    private static final Map<String, String> PROVIDER_ENV_VARS = Map.ofEntries(
        Map.entry("openai", "OPENAI_API_KEY"),
        Map.entry("huggingface", "HF_API_KEY"),
        Map.entry("mistral", "MISTRAL_API_KEY"),
        Map.entry("jinaai", "JINA_API_KEY"),
        Map.entry("voyageai", "VOYAGE_API_KEY"),
        Map.entry("upstageai", "UPSTAGE_API_KEY"),
        Map.entry("nvidia", "NVIDIA_API_KEY"),
        Map.entry("cohere", "COHERE_API_KEY"),
        Map.entry("huggingfacededicated", "HUGGINGFACEDED_API_KEY"),
        Map.entry("bedrock.access-id", "BEDROCK_HEADER_AWS_ACCESS_ID"),
        Map.entry("bedrock.secret-id", "BEDROCK_HEADER_AWS_SECRET_ID")
    );

    @Override
    public ClientLanguage language() {
        return ClientLanguage.JAVA;
    }

    @Override
    public String defaultRepo() {
        return "astra-db-java";
    }

    @Override
    public String defaultRef() {
        return "main";
    }

    @Override
    public List<Function<BaseCtx, ExternalProgram>> requiredPrograms() {
        return List.of(ExternalPrograms::java, ExternalPrograms::mvn);
    }

    @Override
    public String requiredVectorizeProvider() {
        return null;
    }

    @Override
    public boolean needsWipe() {
        return false;
    }

    @Override
    public void setup(ClientsTestCtx ctx, Path repoDir) {
        if (ctx.repoSpec(ClientLanguage.JAVA) instanceof Remote) {
            writeLombokConfig(repoDir);
        }
        writeLogbackConfig(ctx);
        ExternalPrograms.mvn(ctx).runOrThrow(repoDir, "-pl", "astra-db-java", "-am", "-B", "dependency:go-offline");
    }

    @Override
    public List<String> buildCmd(ClientsTestCtx ctx) {
        val testFilter = new StringBuilder("!**/*Test");

        if (!ctx.toggles().vectorize()) {
            testFilter.append(",!**/*Vectorize*");
        }

        if (!ctx.toggles().admin()) {
            testFilter.append(",!**/*AdminIT");
        }

        if (!ctx.toggles().reranking()) {
            testFilter.append(",!**/*FindAndRerankIT");
        }

        val profile = switch (ctx.connectionInfo().destination()) {
            case HCD -> "local";
            case ASTRA_DEV -> "astra-dev";
            default -> "astra-prod";
        };

        return new ArrayList<>(Arrays.asList(ExternalPrograms.mvn(ctx).cmd())) {{
            addAll(List.of("clean", "test", "-pl", "astra-db-java", "-am", "-P" + profile, "-Dtest=" + testFilter, "-B"));

            add("-Dtest.vectorize=" + ctx.toggles().vectorize());
            add("-Dtest.reranking=" + ctx.toggles().reranking());
            add("-Dlogback.configurationFile=" + logbackConfigFile(ctx));

            if (ctx.connectionInfo().destination() != DataAPIDestination.HCD) {
                add("-Dastra.db.url=" + ctx.connectionInfo().endpoint());
            }
        }};
    }

    @Override
    public SequencedMap<String, String> buildEnv(ClientsTestCtx ctx, ConnectionInfo conn) {
        val env = new LinkedHashMap<String, String>();

        if (conn.destination() == DataAPIDestination.ASTRA_DEV) {
            env.put("ASTRA_DB_APPLICATION_TOKEN_DEV", conn.token());
        } else if (conn.destination() != DataAPIDestination.HCD) {
            env.put("ASTRA_DB_APPLICATION_TOKEN", conn.token());
        }

        if (ctx.toggles().vectorize()) {
            for (val entry : PROVIDER_ENV_VARS.entrySet()) {
                ctx.credentials().get(entry.getKey()).ifPresent((key) -> env.put(entry.getValue(), key));
            }
        }
        return env;
    }

    /// Writes `lombok.config` at the repo root, with `config.stopBubbling = true` to avoid dh's
    /// lombok config from affecting the suite's build.
    private static void writeLombokConfig(Path repoDir) {
        val configFile = repoDir.resolve("lombok.config");

        try {
            Files.writeString(configFile, "config.stopBubbling = true\n");
        } catch (IOException e) {
            throw new CliException("Failed to write " + configFile, e);
        }
    }

    private static final String LOGBACK_CONFIG = """
        <configuration>
            <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
                <encoder>
                    <pattern>%d{HH:mm:ss.SSS} %-5level %-20logger : %msg%n</pattern>
                </encoder>
            </appender>

            <logger name="com.datastax.astra" level="WARN" />

            <root level="ERROR">
                <appender-ref ref="STDOUT" />
            </root>
        </configuration>
        """;

    /// Writes the logback config [#buildCmd] points surefire at, quieting the three
    /// `com.datastax.astra.*` loggers that `logback-test.xml` pins to `DEBUG`.
    ///
    /// Lives under `.dh_temp/` rather than the repo, since a local checkout's own
    /// `logback-test.xml` is a tracked file.
    private static void writeLogbackConfig(ClientsTestCtx ctx) {
        val configFile = logbackConfigFile(ctx);

        try {
            Files.writeString(configFile, LOGBACK_CONFIG);
        } catch (IOException e) {
            throw new CliException("Failed to write " + configFile, e);
        }
    }

    private static Path logbackConfigFile(ClientsTestCtx ctx) {
        return ctx.tmpFolder().resolve("java-logback-cfg.xml").toAbsolutePath();
    }
}
