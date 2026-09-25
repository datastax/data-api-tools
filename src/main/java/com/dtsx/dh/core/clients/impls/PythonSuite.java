package com.dtsx.dh.core.clients.impls;

import com.datastax.astra.client.DataAPIDestination;
import com.dtsx.dh.commands.clients.test.ClientsTestCtx;
import com.dtsx.dh.config.ConnectionInfo;
import com.dtsx.dh.config.ctx.BaseCtx;
import com.dtsx.dh.core.clients.ClientSuite;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.lib.ExternalPrograms;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import lombok.val;

import java.net.URI;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/// Runs `astrapy`'s own integration suite (`uv run pytest tests/base/integration`) against a
/// target database, additionally targeting `tests/admin/integration` when admin is on - the only
/// place astrapy's own database-creation tests live, self-gated per backend.
///
/// There's no exhaustive-vectorize path: `tests/vectorize/integration`'s provider matrix is empty
/// unless `TEST_EXTENDED_VECTORIZE` is set, and setting it makes collection fail outright without
/// all 13 of the `AZURE_OPENAI_*`/`OPENAI_*`/`BEDROCK_*`/`HUGGINGFACEDED_*` deployment parameters
/// that `tests/vectorize/vectorize_models.py` reads at import time.
///
/// Hard-requires an empty database - see [#needsWipe].
public class PythonSuite implements ClientSuite {
    public static final String LOCAL_CASSANDRA_CONTACT_POINT_VAR = "LOCAL_CASSANDRA_CONTACT_POINT";

    @Override
    public ClientLanguage language() {
        return ClientLanguage.PYTHON;
    }

    @Override
    public String defaultRepo() {
        return "astrapy";
    }

    @Override
    public String defaultRef() {
        return "main";
    }

    @Override
    public List<Function<BaseCtx, ExternalProgram>> requiredPrograms() {
        return List.of(ExternalPrograms::uv);
    }

    @Override
    public String requiredVectorizeProvider() {
        return "voyageAI";
    }

    @Override
    public boolean needsWipe() {
        return true;
    }

    @Override
    public boolean needsRerankingKey() {
        return true;
    }

    @Override
    public void setup(ClientsTestCtx ctx, Path repoDir) {
        ExternalPrograms.uv(ctx).runOrThrow(repoDir, "sync", "--dev");
    }

    @Override
    public List<String> buildCmd(ClientsTestCtx ctx) {
        val filters = new ArrayList<String>() {{
            if (!ctx.toggles().vectorize()) {
                add("not vectorize");
            }

            if (!ctx.toggles().reranking()) {
                add("not rerank and not farr");
            }
        }};

        return new ArrayList<>() {{
            addAll(Arrays.asList(ExternalPrograms.uv(ctx).cmd()));
            addAll(List.of("run", "pytest", "tests/base/integration"));

            add("--log-cli-level=WARNING");
            add("--log-level=INFO");

            addAll(List.of("-k", String.join(" and ", filters)));

//            TODO needs more env vars
//            if (ctx.toggles().admin()) {
//                add("tests/admin/integration");
//            }

//            if (ctx.toggles().exhaustiveVectorize()) {
//                add("tests/vectorize/integration");
//            }
        }};
    }

    @Override
    public SequencedMap<String, String> buildEnv(ClientsTestCtx ctx, ConnectionInfo conn) {
        val env = new LinkedHashMap<String, String>();

        val isAstra = ctx.connectionInfo().destination() != DataAPIDestination.HCD;

        if (isAstra) {
            env.put("ASTRA_DB_API_ENDPOINT", conn.endpoint());
            env.put("ASTRA_DB_APPLICATION_TOKEN", conn.token());
        } else {
            env.put("LOCAL_DATA_API_ENDPOINT", conn.endpoint());
            env.put("LOCAL_DATA_API_USERNAME", conn.username().orElseThrow());
            env.put("LOCAL_DATA_API_PASSWORD", conn.password().orElseThrow());
            env.put(LOCAL_CASSANDRA_CONTACT_POINT_VAR, resolveCassandraContactPoint(conn, ctx.cassandraContactPoint()));
        }

        env.put("RUN_SHARED_SECRET_VECTORIZE_TESTS", "no");
//        env.put("ALLOW_MISSING_VECTORIZE_TESTS", "yes");

        if (ctx.toggles().vectorize()) {
            ctx.credentials().get("voyageAI").ifPresent((key) -> env.put("HEADER_EMBEDDING_API_KEY_VOYAGEAI", key));
        }

        if (ctx.toggles().reranking()) {
            ctx.credentials().rerankingKey().ifPresent((key) -> env.put("HEADER_RERANKING_API_KEY_NVIDIA", key));
        }

//        TODO
//        if (ctx.toggles().exhaustiveVectorize()) {
//            env.put("TEST_EXTENDED_VECTORIZE", "yes");
//        }

        return env;
    }

    private static String resolveCassandraContactPoint(ConnectionInfo connectionInfo, Optional<String> cassandraContactPoint) {
        return cassandraContactPoint.orElseGet(() -> hostOf(connectionInfo.endpoint()));
    }

    private static String hostOf(String endpoint) {
        val host = URI.create(endpoint).getHost();

        return switch (host) {
            case "localhost", "::1", "[::1]" -> "127.0.0.1";
            default -> host;
        };
    }
}
