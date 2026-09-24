package com.dtsx.dh.core.clients.reporter;

import com.dtsx.dh.commands.clients.test.ClientsTestCtx;
import com.dtsx.dh.core.clients.ClientArtifactSpec;
import com.dtsx.dh.core.clients.ClientArtifactSpec.LocalPath;
import com.dtsx.dh.core.clients.ClientArtifactSpec.Remote;
import com.dtsx.dh.core.clients.ClientsPlan;
import com.dtsx.dh.core.common.ClientLanguage;
import com.dtsx.dh.core.clients.results.ClientResult;
import com.dtsx.dh.core.clients.results.Outcome;
import com.dtsx.dh.lib.CliLogger;
import com.dtsx.dh.lib.DurationUtils;
import lombok.val;
import picocli.CommandLine.Help.Ansi.Style;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.dtsx.dh.lib.ColorUtils.color;

/// Prints and styles everything `dh clients test` shows: the header, the resolved plan (with
/// secrets masked), progress while repos are prepared and clients run, and the closing summary.
public class ClientsReporter {
    public static void printHeader(ClientsTestCtx ctx) {
        val mode = ctx.yes() ? "LIVE" : "DRY RUN";

        CliLogger.println(false, "@|bold Starting clients test run in @!" + mode + "!@ mode.|@");
        CliLogger.println(false);
        CliLogger.println(false, "@|bold View logs:|@");
        CliLogger.println(false, "@!$!@ open " + CliLogger.logFilePath(ctx));
        CliLogger.println(false);
    }

    public static void printPlan(ClientsTestCtx ctx, ClientsPlan plan) {
        val secrets = collectSecrets(ctx);

        CliLogger.println(false, "@|bold Plan:|@");

        for (val lang : plan.languages()) {
            val entry = plan.entry(lang);

            CliLogger.println(false, "  @!" + lang.name().toLowerCase() + "!@:");
            CliLogger.println(false, "    repo: @|faint " + describeRepoSpec(entry.repoSpec()) + "|@");
            CliLogger.println(false, "    cwd: @|faint " + entry.repoDir() + "|@");
            CliLogger.println(false, "    cmd: @|faint " + String.join(" ", entry.invocation().cmd()) + "|@");
            CliLogger.println(false, "    env:");

            for (val e : entry.invocation().env().entrySet()) {
                CliLogger.println(false, "      " + e.getKey() + ": @|faint " + mask(e.getValue(), secrets) + "|@");
            }
        }
    }

    public static void printDryRunNotice() {
        CliLogger.println(false);
        CliLogger.println(false, "@|bold Dry run|@ - pass -y to actually run these.");
    }

    public static void printRunningHeader() {
        CliLogger.println(false);
        CliLogger.println(false, "@|bold Running:|@");
    }

    /// Prints one client's live result line as it finishes, e.g.:
    /// ```
    ///   ✓ go (30m 34s) - log: /path/to/clients-go.log
    /// ```
    public static void printClientResult(ClientLanguage lang, ClientResult result) {
        val label = lang.name().toLowerCase();

        if (result.outcome() == Outcome.SKIPPED) {
            CliLogger.println(false, "  " + describeOutcome(Outcome.SKIPPED) + " " + label + " " + color(Style.faint, "(skipped)"));
            return;
        }

        val durationPart = color(Style.faint, "(" + DurationUtils.formatDuration(result.duration()) + ")");
        val logPart = color(Style.faint, " - log: " + result.logFile());

        CliLogger.println(false, "  " + describeOutcome(result.outcome()) + " " + label + " " + durationPart + logPart);
    }

    /// Prints the closing summary block, mirroring the docs runner's shape: a bold title followed
    /// by `@!-!@`-prefixed count lines.
    public static void printSummary(Map<ClientLanguage, ClientResult> results) {
        val total = results.size();
        val passed = (int) results.values().stream().filter((r) -> r.outcome() == Outcome.PASS).count();
        val failed = (int) results.values().stream().filter((r) -> r.outcome() == Outcome.FAIL).count();
        val skipped = (int) results.values().stream().filter((r) -> r.outcome() == Outcome.SKIPPED).count();

        CliLogger.println(true, "\n@|bold Client Test Summary:|@");
        CliLogger.println(true, "@!-!@ Total clients: " + total);
        CliLogger.println(true, "@!-!@ Passed clients: " + passed);
        CliLogger.println(true, "@!-!@ Failed clients: " + failed);

        if (skipped > 0) {
            CliLogger.println(true, "@!-!@ Skipped clients: " + skipped);
        }
    }

    private static String describeOutcome(Outcome outcome) {
        return switch (outcome) {
            case PASS -> "@|green ✓|@";
            case FAIL -> "@|red ✗|@";
            case SKIPPED -> "@|faint -|@";
        };
    }

    private static String describeRepoSpec(ClientArtifactSpec spec) {
        return switch (spec) {
            case LocalPath(var path) -> "local: " + path;
            case Remote(var repo, var ref) -> repo + "@" + ref;
        };
    }

    private static List<String> collectSecrets(ClientsTestCtx ctx) {
        return new ArrayList<>() {{
            add(ctx.connectionInfo().token());
            ctx.connectionInfo().username().ifPresent(this::add);
            ctx.connectionInfo().password().ifPresent(this::add);
            addAll(ctx.credentials().secretValues());
        }};
    }

    private static String mask(String value, List<String> secrets) {
        for (val secret : secrets) {
            if (!secret.isBlank()) {
                value = value.replace(secret, "****");
            }
        }
        return value;
    }
}
