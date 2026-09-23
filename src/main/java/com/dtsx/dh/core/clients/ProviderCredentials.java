package com.dtsx.dh.core.clients;

import lombok.val;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/// Embedding provider credentials, keyed by the Data API's own provider names, case-insensitively.
///
/// Resolved from `-K` flags first, then `EMBEDDING_API_KEY_<PROVIDER>` env vars for the known
/// provider set. Bedrock's `access-id`/`secret-id` pair only comes from `-K`, since it's a two-key
/// provider rather than a single one.
public record ProviderCredentials(Map<String, Origin> byProvider) {
    public enum OriginKind { FLAG, ENV }

    public record Origin(String value, OriginKind kind) {}

    public static final List<String> KNOWN_PROVIDERS = List.of(
        "openai", "voyageai", "jinaai", "upstageai", "mistral", "nvidia",
        "huggingface", "huggingfacededicated", "cohere"
    );

    public Optional<String> get(String provider) {
        return Optional.ofNullable(byProvider.get(provider.toLowerCase())).map(Origin::value);
    }

    public boolean has(String provider) {
        return byProvider.containsKey(provider.toLowerCase());
    }

    public List<String> secretValues() {
        return byProvider.values().stream().map(Origin::value).toList();
    }

    public static ProviderCredentials resolve(Map<String, String> fromFlags) {
        val result = new LinkedHashMap<String, Origin>();

        for (val entry : fromFlags.entrySet()) {
            result.put(entry.getKey().toLowerCase(), new Origin(entry.getValue(), OriginKind.FLAG));
        }

        for (val provider : KNOWN_PROVIDERS) {
            if (result.containsKey(provider)) {
                continue;
            }

            val envVar = "EMBEDDING_API_KEY_" + provider.toUpperCase();

            Optional.ofNullable(System.getProperty(envVar))
                .or(() -> Optional.ofNullable(System.getenv(envVar)))
                .ifPresent((value) -> result.put(provider, new Origin(value, OriginKind.ENV)));
        }

        return new ProviderCredentials(result);
    }
}
