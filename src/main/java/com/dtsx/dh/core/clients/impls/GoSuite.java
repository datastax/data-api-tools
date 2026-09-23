package com.dtsx.dh.core.clients.impls;

import com.datastax.astra.client.DataAPIDestination;
import com.dtsx.dh.config.ConnectionInfo;
import com.dtsx.dh.config.ctx.BaseCtx;
import com.dtsx.dh.core.clients.ClientSuite;
import com.dtsx.dh.core.clients.ClientToggles;
import com.dtsx.dh.core.clients.ProviderCredentials;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.lib.ExternalPrograms;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.dh.lib.ExternalPrograms.RunResult;
import lombok.val;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

/// Runs `astra-db-go`'s own integration harness (`go run ./integration`) against a target database.
///
/// Always passes `-L` to skip its legacy suite, which only runs after the new harness fully
/// succeeds and has its own separate failure semantics. Filter exclusions (`-F`) are ORed together,
/// which is what's wanted here: excluding tests tagged `(VECTORIZE)` or `(ADMIN)` independently.
public class GoSuite implements ClientSuite {
    public static final String NO_TESTS_RAN_MARKER = "No tests were run";

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
    public void setup(BaseCtx ctx, Path repoDir) {
        ExternalPrograms.go(ctx).runOrThrow(repoDir, "mod", "download");
    }

    @Override
    public Invocation invocation(BaseCtx ctx, Path repoDir, ConnectionInfo connectionInfo, ClientToggles toggles, ProviderCredentials credentials) {
        val cmd = new ArrayList<>(Arrays.asList(ExternalPrograms.go(ctx).cmd()));
        cmd.addAll(List.of("run", "./integration", "-L"));

        if (!toggles.vectorize()) {
            cmd.add("-F");
            cmd.add("(VECTORIZE)");
        }

        if (!toggles.admin()) {
            cmd.add("-F");
            cmd.add("(ADMIN)");
        }

        val env = new LinkedHashMap<String, String>();
        env.put("BACKEND", connectionInfo.destination() == DataAPIDestination.HCD ? "hcd" : "astra");
        env.put("API_ENDPOINT", connectionInfo.endpoint());
        env.put("APPLICATION_TOKEN", connectionInfo.token());

        if (toggles.vectorize()) {
            credentials.get("openai").ifPresent((key) -> env.put("EMBEDDING_API_KEY", key));
        }

        return new Invocation(repoDir, cmd, env);
    }

    @Override
    public boolean isSuccess(RunResult result) {
        return result.ok() && !result.output().contains(NO_TESTS_RAN_MARKER);
    }
}
