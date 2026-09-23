package com.dtsx.dh.core.clients;

import com.datastax.astra.client.DataAPIDestination;
import com.dtsx.dh.config.ConnectionInfo;
import lombok.val;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/// The credentials a [ClientSuite] runs with: embedding provider keys, keyed by the Data API's own
/// provider names case-insensitively, plus the reranking key shared by every suite that needs one.
///
/// Provider credentials resolve from `-K` flags first, then any `EMBEDDING_API_KEY_<PROVIDER>`
/// system property or environment variable, whatever `<PROVIDER>` turns out to be. Bedrock's
/// `access-id`/`secret-id` pair only comes from `-K`, since it's a two-key provider rather than a
/// single one.
///
/// The reranking key resolves from `--reranking-key`, then the `RERANKING_API_KEY` env var (system
/// property checked before the real environment), then - on an Astra target only - the connection's
/// own Astra token. HCD has no fallback.
public record SuiteCredentials(Map<String, String> byProvider, Optional<String> rerankingKey) {
    public static final String RERANKING_API_KEY_ENV_VAR = "RERANKING_API_KEY";

    private static final String EMBEDDING_API_KEY_PREFIX = "EMBEDDING_API_KEY_";

    public Optional<String> get(String provider) {
        return Optional.ofNullable(byProvider.get(provider.toLowerCase()));
    }

    public boolean has(String provider) {
        return get(provider).isPresent();
    }

    public List<String> secretValues() {
        val values = new ArrayList<>(byProvider.values());
        rerankingKey.ifPresent(values::add);
        return values;
    }

    public static SuiteCredentials resolve(Map<String, String> fromFlags, Optional<String> rerankingKeyFlag, ConnectionInfo connectionInfo) {
        val byProvider = new LinkedHashMap<String, String>() {{
            putEmbeddingKeys(this, System.getenv());
            putEmbeddingKeys(this, systemPropertiesAsMap());
        }};

        fromFlags.forEach((provider, key) -> byProvider.put(provider.toLowerCase(), key));

        val rerankingKey = rerankingKeyFlag
            .or(() -> Optional.ofNullable(System.getProperty(RERANKING_API_KEY_ENV_VAR)))
            .or(() -> Optional.ofNullable(System.getenv(RERANKING_API_KEY_ENV_VAR)))
            .or(() -> connectionInfo.destination() != DataAPIDestination.HCD ? Optional.of(connectionInfo.token()) : Optional.empty());

        return new SuiteCredentials(byProvider, rerankingKey);
    }

    /// Copies every `vars` entry named [#EMBEDDING_API_KEY_PREFIX]`<PROVIDER>` into `result`,
    /// deriving each provider from the remainder of the name, lower-cased.
    private static void putEmbeddingKeys(Map<String, String> all, Map<String, String> vars) {
        vars.forEach((name, value) -> {
            if (name.startsWith(EMBEDDING_API_KEY_PREFIX)) {
                all.put(name.substring(EMBEDDING_API_KEY_PREFIX.length()).toLowerCase(), value);
            }
        });
    }

    private static Map<String, String> systemPropertiesAsMap() {
        val result = new LinkedHashMap<String, String>();

        for (val name : System.getProperties().stringPropertyNames()) {
            result.put(name, System.getProperty(name));
        }

        return result;
    }
}
