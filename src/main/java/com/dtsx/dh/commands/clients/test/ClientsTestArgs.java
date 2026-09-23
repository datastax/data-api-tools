package com.dtsx.dh.commands.clients.test;

import com.dtsx.dh.config.args.BaseConnectedArgs;
import com.dtsx.dh.config.args.mixins.BailMixin;
import com.dtsx.dh.config.args.mixins.DriversMixin;
import com.dtsx.dh.config.args.mixins.YesMixin;
import com.dtsx.dh.core.common.ClientLanguage;
import lombok.ToString;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;

import java.util.Map;
import java.util.Optional;

@ToString
public class ClientsTestArgs extends BaseConnectedArgs<ClientsTestCtx> {
    @Mixin
    public DriversMixin $drivers = DriversMixin.exclude(ClientLanguage.BASH);

    @Option(
        names = { "-K", "--embedding-key" },
        description = "Embedding provider credentials, keyed by the Data API's own provider name (case-insensitive). Repeatable, e.g. `-K openai=sk-...` `-K bedrock.access-id=...`.",
        paramLabel = "PROVIDER=KEY"
    )
    public Map<String, String> $embeddingKeys = Map.of();

    @Option(
        names = { "--reranking-key" },
        description = "Reranking API key. Defaults to the Astra token on Astra; a DataStax dev Astra token is required on HCD.",
        defaultValue = "${RERANKING_API_KEY:-" + Option.NULL_VALUE + "}",
        paramLabel = "KEY"
    )
    public Optional<String> $rerankingKey;

    @Option(
        names = { "--vectorize" },
        description = "Run vectorize tests. Defaults to on.",
        negatable = true
    )
    public Optional<Boolean> $vectorize;

    @Option(
        names = { "--exhaustive-vectorize" },
        description = "Run the full per-provider vectorize matrix. Defaults to off.",
        negatable = true
    )
    public Optional<Boolean> $exhaustiveVectorize;

    @Option(
        names = { "--reranking" },
        description = "Run reranking tests. Defaults to off.",
        negatable = true
    )
    public Optional<Boolean> $reranking;

    @Option(
        names = { "--admin" },
        description = "Run admin tests. Defaults to off.",
        negatable = true
    )
    public Optional<Boolean> $admin;

    @Option(
        names = { "-R", "--repo" },
        description = "Client repos to check out (e.g., `-Rgo=@my-branch`, `-Rgo=someone/astra-db-go@feature/foo`, or a local path).",
        paramLabel = "LANG=SPEC"
    )
    public Map<ClientLanguage, String> $repoOverrides = Map.of();

    @Mixin
    public YesMixin $yes;

    @Mixin
    public BailMixin $bail;

    @Option(
        names = { "--clean" },
        description = "Delete the managed client repo clones and start over.",
        defaultValue = "${CLEAN:-false}"
    )
    public boolean $clean;

    @Override
    public ClientsTestCtx toCtx(CommandSpec spec) {
        return new ClientsTestCtx(this, spec);
    }
}