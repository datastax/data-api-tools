package com.dtsx.docs.config;

import com.datastax.astra.client.DataAPIDestination;
import com.dtsx.docs.config.args.BaseConnectedArgs;
import com.dtsx.docs.core.runner.RunException;
import lombok.Getter;
import lombok.val;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;

import java.util.Base64;
import java.util.Optional;

/// Allows for abstraction over Astra vs HCD connections.
@Getter
public class ConnectionInfo {
    public static final String LOCAL_ENDPOINT = "http://localhost:8181";
    public static final String LOCAL_TOKEN = "Cassandra:Y2Fzc2FuZHJh:Y2Fzc2FuZHJh";

    private final String token;
    private final String endpoint;
    private final DataAPIDestination destination;

    private final Optional<String> username;
    private final Optional<String> password;

    /// Builds a [ConnectionInfo] from the `--local` / `-e` / `-t` flags alone.
    ///
    /// `--local` supplies both the endpoint and the token; `-e`/`-t` override either half
    /// individually. At least one of `--local` or `-e` is required, so that a destructive
    /// command never has to guess its target.
    public static ConnectionInfo fromFlags(CommandLine cmd, BaseConnectedArgs<?> args) {
        if (!args.$local && args.$apiEndpoint.isEmpty()) {
            throw new ParameterException(cmd, "Missing target database; provide '--local' to target the local startgate stack, or '-e/--api-endpoint' to target a real one.");
        }

        val endpoint = args.$apiEndpoint.orElse(LOCAL_ENDPOINT);

        val token = args.$astraToken.or(() -> args.$local ? Optional.of(LOCAL_TOKEN) : Optional.empty()).orElseThrow(() ->
            new ParameterException(cmd, "Missing required option astra token; please provide it via the '-t/--astra-token' command line flag, or use '--local'.")
        );

        return new ConnectionInfo(token, endpoint);
    }

    /// Builds a [ConnectionInfo] from the `-t`/`--astra-token` and `-e`/`--api-endpoint` flags,
    /// falling back to `--local`'s well-known values, then to the `ASTRA_TOKEN`/`API_ENDPOINT`
    /// env vars.
    public static ConnectionInfo fromFlagsOrEnv(CommandLine cmd, BaseConnectedArgs<?> args) {
        val apiEndpoint = resolveConnectionValue(args.$apiEndpoint, "API_ENDPOINT", args.$local, LOCAL_ENDPOINT);
        val token = resolveConnectionValue(args.$astraToken, "ASTRA_TOKEN", args.$local, LOCAL_TOKEN);

        return new ConnectionInfo(
            ArgUtils.requireFlag(cmd, token, "astra token", "-t", "ASTRA_TOKEN"),
            ArgUtils.requireFlag(cmd, apiEndpoint, "API endpoint", "-e", "API_ENDPOINT")
        );
    }

    /// Resolves a connection value from, in order, the explicit flag, `--local`'s well-known
    /// value, then the env var - checked as a system property first, since `HelperCli` loads
    /// dotenv files into system properties.
    private static Optional<String> resolveConnectionValue(Optional<String> flag, String envVar, boolean local, String localValue) {
        return flag
            .or(() -> local ? Optional.of(localValue) : Optional.empty())
            .or(() -> Optional.ofNullable(System.getProperty(envVar)))
            .or(() -> Optional.ofNullable(System.getenv(envVar)));
    }

    public ConnectionInfo(String token, String endpoint) {
        this.token = token;
        this.endpoint = endpoint;

        this.destination =
            (endpoint.contains("astra.datastax.com"))
                ? DataAPIDestination.ASTRA :
            (endpoint.contains("astra-dev.datastax.com"))
                ? DataAPIDestination.ASTRA_DEV :
            (endpoint.contains("astra-test.datastax.com"))
                ? DataAPIDestination.ASTRA_TEST
                : DataAPIDestination.HCD;

        if (token.startsWith("Cassandra:")) {
            val parts = token.split(":");

            if (parts.length != 3) {
                throw new RunException("Invalid Cassandra:... token format; expected 3 parts but got " + parts.length + " parts");
            }

            val decoder = Base64.getDecoder();

            this.username = Optional.of(new String(decoder.decode(parts[1])));
            this.password = Optional.of(new String(decoder.decode(parts[2])));
        } else {
            this.username = Optional.empty();
            this.password = Optional.empty();
        }
    }
}
