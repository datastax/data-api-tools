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

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/// Runs `astra-db-go`'s own integration harness (`go run ./integration`) against a target database.
///
/// Always passes `-L` to skip its legacy suite, which only runs after the new harness fully
/// succeeds and has its own separate failure semantics. Filter exclusions (`-F`) are ORed together,
/// which is what's wanted here: excluding tests tagged `(VECTORIZE)` or `(ADMIN)` independently.
public class GoSuite implements ClientSuite {
    @Override
    public ClientLanguage language() {
        return ClientLanguage.GO;
    }

    @Override
    public String defaultRepo() {
        return "astra-db-go";
    }

    @Override
    public String defaultRef() {
        return "main";
    }

    @Override
    public List<Function<BaseCtx, ExternalProgram>> requiredPrograms() {
        return List.of(ExternalPrograms::go);
    }

    @Override
    public String requiredVectorizeProvider() {
        return "openai";
    }

    @Override
    public boolean needsWipe() {
        return false;
    }

    @Override
    public void setup(ClientsTestCtx ctx, Path repoDir) {
        ExternalPrograms.go(ctx).runOrThrow(repoDir, "mod", "download");
    }

    @Override
    public List<String> buildCmd(ClientsTestCtx ctx) {
        return new ArrayList<>() {{
            addAll(Arrays.asList(ExternalPrograms.go(ctx).cmd()));
            addAll(List.of("run", "./integration", "-L"));

            if (!ctx.toggles().vectorize()) {
                add("-F");
                add("(VECTORIZE)");
            }

            if (!ctx.toggles().admin()) {
                add("-F");
                add("(ADMIN)");
            }
        }};
    }

    @Override
    public SequencedMap<String, String> buildEnv(ClientsTestCtx ctx, ConnectionInfo conn) {
        val env = new LinkedHashMap<String, String>();

        env.put("BACKEND", conn.destination() == DataAPIDestination.HCD ? "hcd" : "astra");
        env.put("API_ENDPOINT", conn.endpoint());
        env.put("APPLICATION_TOKEN", conn.token());

        if (ctx.toggles().vectorize()) {
            ctx.credentials().get("openai").ifPresent((key) -> env.put("EMBEDDING_API_KEY", key));
        }

        return env;
    }
}
