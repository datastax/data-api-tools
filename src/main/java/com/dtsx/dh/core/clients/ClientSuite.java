package com.dtsx.dh.core.clients;

import com.dtsx.dh.config.ConnectionInfo;
import com.dtsx.dh.config.ctx.BaseCtx;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import com.dtsx.dh.lib.ExternalPrograms.RunResult;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/// A pluggable suite for cloning a client's own repo and running its own integration test harness
/// against a target database.
public interface ClientSuite {
    ClientLanguage language();

    /// The bare repo name this suite clones by default (e.g. `astra-db-go`), qualified with the
    /// datastax org by [com.dtsx.dh.core.clients.ClientArtifactSpec].
    String defaultRepo();

    /// The default branch, tag or commit checked out when no `-R` override names one.
    String defaultRef();

    /// External programs this suite needs beyond `git`, which every suite needs for the clone.
    List<Function<BaseCtx, ExternalProgram>> requiredPrograms();

    /// The embedding provider this suite requires a key for when vectorize is on, or `null` if it
    /// self-skips per provider.
    @Nullable String requiredVectorizeProvider();

    /// Whether this suite needs its target keyspaces wiped of content before it runs.
    boolean needsWipe();

    /// Installs dependencies and otherwise prepares `repoDir` for [#invocation].
    void setup(BaseCtx ctx, Path repoDir);

    /// Builds the exact invocation this suite runs: its working directory, its argv, and the
    /// environment variables it needs. Secrets go in `env` only, never in `cmd`.
    ///
    /// The argv must start from the [ExternalProgram#cmd] of the runner resolved off `ctx`, so that
    /// `-C`/`<NAME>_COMMAND` overrides reach the suite itself.
    Invocation invocation(BaseCtx ctx, Path repoDir, ConnectionInfo connectionInfo, ClientToggles toggles, ProviderCredentials credentials);

    /// Whether a finished run counts as a pass. Most suites can just check the exit code; go's
    /// harness exits 0 even when zero tests ran, so its suite overrides this.
    default boolean isSuccess(RunResult result) {
        return result.ok();
    }

    record Invocation(Path cwd, List<String> cmd, Map<String, String> env) {}
}
