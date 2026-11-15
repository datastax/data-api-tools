package com.dtsx.dh.core.clients.impls;

import com.dtsx.dh.commands.clients.test.ClientsTestCtx;
import com.dtsx.dh.config.ConnectionInfo;
import com.dtsx.dh.config.ctx.BaseCtx;
import com.dtsx.dh.core.clients.ClientSuite;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.lib.ExternalPrograms;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import lombok.val;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/// Runs `astra-db-csharp`'s integration test project (`dotnet test test/DataStax.AstraDB.DataApi.IntegrationTests`)
/// against a target database.
///
/// Needs its target keyspaces wiped first, and needs `default_keyspace` to already exist - see
/// [#needsWipe].
///
/// Requires a dotnet carrying an 8.x SDK: the repo pins 8.0.0 via `global.json`'s
/// `rollForward: latestMinor`, which doesn't cross majors, and its integration tests target net8.0.
public class CSharpSuite implements ClientSuite {
    private static final String INTEGRATION_TEST_PROJECT = "test/DataStax.AstraDB.DataApi.IntegrationTests";

    @Override
    public ClientLanguage language() {
        return ClientLanguage.CSHARP;
    }

    @Override
    public String defaultRepo() {
        return "astra-db-csharp";
    }

    @Override
    public String defaultRef() {
        return "main";
    }

    @Override
    public List<Function<BaseCtx, ExternalProgram>> requiredPrograms() {
        return List.of(ExternalPrograms::dotnet8);
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
        ExternalPrograms.dotnet(ctx).runOrThrow(repoDir, "restore", INTEGRATION_TEST_PROJECT);
    }

    @Override
    public List<String> buildCmd(ClientsTestCtx ctx) {
        val cmd = new ArrayList<>(Arrays.asList(ExternalPrograms.dotnet(ctx).cmd()));
        cmd.addAll(List.of("test", INTEGRATION_TEST_PROJECT));

        val filterClauses = new ArrayList<String>() {{
            if (!ctx.toggles().vectorize()) {
                add("FullyQualifiedName!~Vectorize");
            }

            if (!ctx.toggles().admin()) {
                add("FullyQualifiedName!~AdminTests");
            }

            if (!ctx.toggles().reranking()) {
                add("FullyQualifiedName!~CollectionFARRCursorTests");
            }
        }};

        if (!filterClauses.isEmpty()) {
            cmd.add("--filter");
            cmd.add(String.join("&", filterClauses));
        }

        return cmd;
    }

    @Override
    public SequencedMap<String, String> buildEnv(ClientsTestCtx ctx, ConnectionInfo conn) {
        val env = new LinkedHashMap<String, String>();
        env.put("ASTRA_DB_TOKEN", conn.token());
        env.put("ASTRA_DB_URL", conn.endpoint());
        env.put("ASTRA_DB_DESTINATION", conn.isLocal() ? "hcd" : "astra");

        if (ctx.toggles().reranking()) {
            ctx.credentials().rerankingKey().ifPresent((key) -> env.put("HEADER_RERANKING_API_KEY_NVIDIA", key));
        }

        if (ctx.toggles().vectorize()) {
            ctx.credentials().get("voyageAI").ifPresent((key) -> env.put("HEADER_EMBEDDING_API_KEY_VOYAGEAI", key));
        }

        return env;
    }
}
