package com.dtsx.dh.core.clients;

import com.dtsx.dh.commands.clients.test.ClientsTestCtx;
import com.dtsx.dh.core.clients.ClientArtifactSpec.LocalPath;
import com.dtsx.dh.core.clients.ClientSuite.Invocation;
import com.dtsx.dh.core.common.ClientLanguage;
import lombok.val;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The fully-resolved plan for a `dh clients test` run, built once up front from `ctx` so that
/// what gets printed and what gets executed can never drift apart.
public class ClientsPlan {
    /// One selected language's resolved plan: its suite, the repo spec it was resolved from, the
    /// directory its repo lives (or will live) in, the exact invocation that'll run it, and whether
    /// its target keyspaces need wiping first.
    public record Entry(ClientSuite suite, ClientArtifactSpec repoSpec, Path repoDir, Invocation invocation, boolean needsWipe) {}

    private final Map<ClientLanguage, Entry> entries;

    private ClientsPlan(Map<ClientLanguage, Entry> entries) {
        this.entries = entries;
    }

    public static ClientsPlan build(ClientsTestCtx ctx) {
        val connInfo = ctx.connectionInfo();
        val entries = new LinkedHashMap<ClientLanguage, Entry>();

        for (val lang : ctx.languages()) {
            val suite = ctx.suite(lang);
            val repoSpec = ctx.repoSpec(lang);
            val repoDir = resolveRepoDir(ctx, lang, repoSpec);
            val invocation = suite.invocation(ctx, repoDir, connInfo, ctx.toggles(), ctx.credentials());

            entries.put(lang, new Entry(suite, repoSpec, repoDir, invocation, suite.needsWipe()));
        }

        return new ClientsPlan(entries);
    }

    public List<ClientLanguage> languages() {
        return List.copyOf(entries.keySet());
    }

    public Entry entry(ClientLanguage lang) {
        return entries.get(lang);
    }

    private static Path resolveRepoDir(ClientsTestCtx ctx, ClientLanguage lang, ClientArtifactSpec spec) {
        if (spec instanceof LocalPath localPath) {
            return localPath.path().toAbsolutePath().normalize();
        }

        return ctx.tmpFolder().resolve("client_repos").resolve(lang.name().toLowerCase());
    }
}
