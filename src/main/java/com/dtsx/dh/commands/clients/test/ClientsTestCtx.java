package com.dtsx.dh.commands.clients.test;

import com.datastax.astra.client.DataAPIDestination;
import com.dtsx.dh.config.ConnectionInfo;
import com.dtsx.dh.config.ctx.BaseConnectedCtx;
import com.dtsx.dh.config.ctx.BaseCtx;
import com.dtsx.dh.core.clients.ClientArtifactSpec;
import com.dtsx.dh.core.clients.ClientSuite;
import com.dtsx.dh.core.clients.ClientToggles;
import com.dtsx.dh.core.clients.SuiteCredentials;
import com.dtsx.dh.core.clients.impls.PythonSuite;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.lib.ExternalPrograms;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.MustBeInvokedByOverriders;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/// The runtime context for `dh clients test`, resolving the target connection, the selected
/// client suites, the test toggles, provider credentials and per-client repo specs.
@Getter
public class ClientsTestCtx extends BaseConnectedCtx {
    private final Map<ClientLanguage, ClientSuite> suites;
    private final Map<ClientLanguage, ClientArtifactSpec> repoSpecs;
    private final ClientToggles toggles;
    private final SuiteCredentials credentials;
    private final Optional<String> cassandraContactPoint;
    private final boolean yes;
    private final boolean bail;
    private final boolean clean;

    public List<ClientLanguage> languages() {
        return new ArrayList<>(suites.keySet());
    }

    public ClientSuite suite(ClientLanguage lang) {
        return suites.get(lang);
    }

    public ClientArtifactSpec repoSpec(ClientLanguage lang) {
        return repoSpecs.get(lang);
    }

    public ClientsTestCtx(ClientsTestArgs args, CommandSpec spec) {
        super(args, spec, resolveConnectionInfo(spec.commandLine(), args));
        this.suites = mkSuites(cmd, args);
        this.toggles = mkToggles(cmd, args);
        this.credentials = SuiteCredentials.resolve(args.$embeddingKeys, args.$rerankingKey, connectionInfo());
        this.cassandraContactPoint = resolveCassandraContactPoint(args);
        this.repoSpecs = mkRepoSpecs(args, suites);
        this.yes = args.$yes.unwrap();
        this.bail = args.$bail.unwrap();
        this.clean = args.$clean;

        verifyRequiredCredentials(cmd, suites, toggles, credentials);
        verifyRerankingCredential(cmd, suites, toggles, credentials);
    }

    @Override
    @MustBeInvokedByOverriders
    protected Set<Function<BaseCtx, ExternalProgram>> requiredPrograms() {
        return new HashSet<>(super.requiredPrograms()) {{
            add(ExternalPrograms::git);

            for (val suite : suites.values()) {
                addAll(suite.requiredPrograms());
            }
        }};
    }

    private static ConnectionInfo resolveConnectionInfo(CommandLine cmd, ClientsTestArgs args) {
        val info = ConnectionInfo.fromFlagsOrEnv(cmd, args);

        if (info.destination() == DataAPIDestination.ASTRA_TEST) {
            throw new ParameterException(cmd, "astra-test endpoints aren't supported by `dh clients test`; only Astra dev/prod and HCD are.");
        }

        return info;
    }

    /// Resolves astrapy's CQL contact point from, in order, `--cassandra-contact-point` then the
    /// `LOCAL_CASSANDRA_CONTACT_POINT` env var (checked as a system property first). Empty means
    /// [com.dtsx.dh.core.clients.impls.PythonSuite] falls back to its own local-stack default.
    private static Optional<String> resolveCassandraContactPoint(ClientsTestArgs args) {
        return args.$cassandraContactPoint
            .or(() -> Optional.ofNullable(System.getProperty(PythonSuite.LOCAL_CASSANDRA_CONTACT_POINT_VAR)))
            .or(() -> Optional.ofNullable(System.getenv(PythonSuite.LOCAL_CASSANDRA_CONTACT_POINT_VAR)));
    }

    private Map<ClientLanguage, ClientSuite> mkSuites(CommandLine cmd, ClientsTestArgs args) {
        val result = new LinkedHashMap<ClientLanguage, ClientSuite>();

        for (val lang : args.$drivers.resolve()) {
            if (lang.mkClientSuite() == null) {
                throw new ParameterException(cmd, lang.name() + " does not have a client suite implemented yet.");
            }

            result.put(lang, lang.mkClientSuite().get());
        }

        return result;
    }

    private ClientToggles mkToggles(CommandLine cmd, ClientsTestArgs args) {
        val vectorize = resolveToggle(args.$vectorize, "VECTORIZE", true);
        val exhaustiveVectorize = resolveToggle(args.$exhaustiveVectorize, "EXHAUSTIVE_VECTORIZE", false);
        val reranking = resolveToggle(args.$reranking, "RERANKING", false);
        val admin = resolveToggle(args.$admin, "ADMIN", false);

        if (!vectorize && exhaustiveVectorize) {
            throw new ParameterException(cmd, "--no-vectorize and --exhaustive-vectorize can't be used together; exhaustive vectorize requires vectorize to be on.");
        }

        return new ClientToggles(vectorize, exhaustiveVectorize, reranking, admin);
    }

    /// Resolves a tri-state toggle flag: the flag's value if it was passed, else `envVar` (checked
    /// as a system property first, then a real environment variable, parsed case-insensitively),
    /// else `defaultValue`.
    private static boolean resolveToggle(Optional<Boolean> flag, String envVar, boolean defaultValue) {
        if (flag.isPresent()) {
            return flag.get();
        }

        val fromEnv = Optional.ofNullable(System.getProperty(envVar)).or(() -> Optional.ofNullable(System.getenv(envVar)));

        return fromEnv.map(Boolean::parseBoolean).orElse(defaultValue);
    }

    private Map<ClientLanguage, ClientArtifactSpec> mkRepoSpecs(ClientsTestArgs args, Map<ClientLanguage, ClientSuite> suites) {
        val result = new LinkedHashMap<ClientLanguage, ClientArtifactSpec>();

        for (val entry : suites.entrySet()) {
            val lang = entry.getKey();
            val suite = entry.getValue();

            val raw = languageOverride(lang, args.$repoOverrides, "REPO");

            val resolved = ClientArtifactSpec.resolve(raw, suite.defaultRepo(), suite.defaultRef());

            result.put(lang, resolved);
        }

        return result;
    }

    private void verifyRequiredCredentials(CommandLine cmd, Map<ClientLanguage, ClientSuite> suites, ClientToggles toggles, SuiteCredentials credentials) {
        if (!toggles.vectorize()) {
            return;
        }

        for (val suite : suites.values()) {
            val provider = suite.requiredVectorizeProvider();

            if (provider != null && !credentials.has(provider)) {
                throw new ParameterException(cmd, suite.language().name() + " requires an embedding key for provider '" + provider + "' when vectorize is on; " +
                    "pass `-K " + provider + "=<key>`, set the `EMBEDDING_API_KEY_" + provider.toUpperCase() + "` env var, or run with `--no-vectorize`."
                );
            }
        }
    }

    /// On HCD there's no Astra token for [SuiteCredentials] to fall back to, so a selected suite
    /// that needs the reranking key must fail up front rather than run with it silently unset.
    private void verifyRerankingCredential(CommandLine cmd, Map<ClientLanguage, ClientSuite> suites, ClientToggles toggles, SuiteCredentials credentials) {
        if (!toggles.reranking() || connectionInfo().destination() != DataAPIDestination.HCD || credentials.rerankingKey().isPresent()) {
            return;
        }

        for (val suite : suites.values()) {
            if (suite.needsRerankingKey()) {
                throw new ParameterException(cmd, suite.language().name() + " requires a reranking key on HCD when --reranking is on; " +
                    "pass `--reranking-key <key>`, set the `RERANKING_API_KEY` env var, or run with `--no-reranking`."
                );
            }
        }
    }
}
