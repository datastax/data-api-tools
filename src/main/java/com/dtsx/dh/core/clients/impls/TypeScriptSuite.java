package com.dtsx.dh.core.clients.impls;

import com.datastax.astra.client.DataAPIDestination;
import com.dtsx.dh.commands.clients.test.ClientsTestCtx;
import com.dtsx.dh.config.ConnectionInfo;
import com.dtsx.dh.config.ctx.BaseCtx;
import com.dtsx.dh.core.clients.ClientArtifactSpec.LocalPath;
import com.dtsx.dh.core.clients.ClientSuite;
import com.dtsx.dh.core.clients.SuiteCredentials;
import com.dtsx.dh.core.common.CliException;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.lib.ExternalPrograms;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.dh.lib.JacksonUtils;
import lombok.val;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/// Runs `astra-db-ts`'s integration suite through its own `npm run test -- <args>` wrapper
/// (`scripts/test.ts`), rather than invoking mocha directly.
///
/// The wrapper's `RunTests.{Vectorize,LongRunning,Admin}` all derive from one `TestType` knob,
/// which always resolves to `all` here, so the four toggles are instead expressed as `-F <TAG>`
/// filters excluding tests tagged `VECTORIZE`/`LONG`/`ADMIN`/`RERANKING`. Since `TestType` stays
/// `all`, `RunTests.Vectorize` is always true, so `CLIENT_EMBEDDING_API_KEY` must always be set:
/// the real openai key when vectorize is on, a dummy value that only has to satisfy the wrapper
/// when it's filtered out.
public class TypeScriptSuite implements ClientSuite {
    private static final String DUMMY_EMBEDDING_API_KEY = "dummy-embedding-api-key";

    /// Correctly-cased Data API provider names, keyed by [SuiteCredentials]'s lowercase keys,
    /// for the `headers` entries in a generated `vectorize_test_spec.json`.
    private static final Map<String, String> PROVIDER_CASING = Map.ofEntries(
        Map.entry("openai", "openai"),
        Map.entry("voyageai", "voyageAI"),
        Map.entry("jinaai", "jinaAI"),
        Map.entry("upstageai", "upstageAI"),
        Map.entry("mistral", "mistral"),
        Map.entry("nvidia", "nvidia"),
        Map.entry("huggingface", "huggingface"),
        Map.entry("huggingfacededicated", "huggingfaceDedicated"),
        Map.entry("cohere", "cohere")
    );

    @Override
    public ClientLanguage language() {
        return ClientLanguage.TYPESCRIPT;
    }

    @Override
    public String defaultRepo() {
        return "astra-db-ts";
    }

    @Override
    public String defaultRef() {
        return "master";
    }

    @Override
    public List<Function<BaseCtx, ExternalProgram>> requiredPrograms() {
        return List.of(ExternalPrograms::node, ExternalPrograms::npm);
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
        ExternalPrograms.npm(ctx).runOrThrow(repoDir, "i");

        if (ctx.toggles().exhaustiveVectorize()) {
            if (ctx.repoSpec(language()) instanceof LocalPath) {
                throw new CliException("Cannot create vectorize_test_spec.json for a local repo; Use a git ref instead of `-R local`.");
            }
            writeVectorizeSpec(repoDir, ctx.credentials());
        }
    }

    @Override
    public List<String> buildCmd(ClientsTestCtx ctx) {
        val cmd = new ArrayList<>(Arrays.asList(ExternalPrograms.npm(ctx).cmd()));
        cmd.addAll(List.of("run", "test", "--"));
        cmd.add("-f");
        cmd.add("integration.");

        if (!ctx.toggles().reranking()) {
            cmd.add("-F");
            cmd.add("RERANKING");
        }

        if (!ctx.toggles().vectorize()) {
            cmd.add("-F");
            cmd.add("VECTORIZE");
        }

        if (!ctx.toggles().admin()) {
            cmd.add("-F");
            cmd.add("ADMIN");
        }

        if (!ctx.toggles().exhaustiveVectorize()) {
            cmd.add("-F");
            cmd.add("integration.documents.vectorize");
        }

        cmd.add("-R");
        return cmd;
    }

    @Override
    public SequencedMap<String, String> buildEnv(ClientsTestCtx ctx, ConnectionInfo conn) {
        val env = new LinkedHashMap<String, String>();

        env.put("CLIENT_DB_URL", conn.endpoint());
        env.put("CLIENT_DB_TOKEN", conn.token());
        env.put("CLIENT_DB_ENVIRONMENT", conn.destination() == DataAPIDestination.HCD ? "hcd" : "astra");

        env.put("CLIENT_EMBEDDING_API_KEY", ctx.toggles().vectorize()
            ? ctx.credentials().get("openai").orElseThrow(() -> new CliException(
            "typescript requires an embedding key for provider 'openai' when vectorize is on; " +
                "pass `-K openai=<key>` or set the `EMBEDDING_API_KEY_OPENAI` env var."
        ))
            : DUMMY_EMBEDDING_API_KEY);

        return env;
    }

    /// Writes `vectorize_test_spec.json` at the repo root, with a `headers` entry per single-key
    /// provider credential. No `sharedSecret` entries - shared-secret/KMS is out of scope.
    private static void writeVectorizeSpec(Path repoDir, SuiteCredentials credentials) {
        val spec = new LinkedHashMap<String, Object>();

        for (val entry : credentials.byProvider().entrySet()) {
            val provider = PROVIDER_CASING.get(entry.getKey());

            if (provider == null) {
                continue;
            }

            spec.put(provider, Map.of("headers", Map.of("x-embedding-api-key", entry.getValue())));
        }

        val specFile = repoDir.resolve("vectorize_test_spec.json");

        try {
            Files.writeString(specFile, JacksonUtils.formatJsonPretty(spec));
        } catch (IOException e) {
            throw new CliException("Failed to write " + specFile, e);
        }
    }
}
