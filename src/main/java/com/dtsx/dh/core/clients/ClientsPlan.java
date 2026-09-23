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

/// The fully-resolved plan for a `dh clients test` run, built once from `ctx` after each selected
/// language's repo has been prepared, so what gets printed and what gets executed can never drift
/// apart.
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
        val entries = new LinkedHashMap<ClientLanguage, Entry>();

        for (val lang : ctx.languages()) {
            val suite = ctx.suite(lang);
            val repoSpec = ctx.repoSpec(lang);
            val repoDir = resolveRepoDir(ctx, lang, repoSpec);
            val invocation = suite.invocation(ctx, repoDir);

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

    /// Resolves the directory `lang`'s repo lives (or will live) in: the local path as-is for a
    /// [LocalPath] spec, otherwise the managed clone directory under `ctx`'s tmp folder. Shared by
    /// [#build] and repo preparation so both agree on where a given language's repo sits.
    public static Path resolveRepoDir(ClientsTestCtx ctx, ClientLanguage lang, ClientArtifactSpec spec) {
        if (spec instanceof LocalPath(var path)) {
            return path.toAbsolutePath().normalize();
        }

        return ctx.tmpFolder().resolve("client_repos").resolve(lang.name().toLowerCase());
    }
}
