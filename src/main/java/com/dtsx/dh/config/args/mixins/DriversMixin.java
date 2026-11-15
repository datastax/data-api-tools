package com.dtsx.dh.config.args.mixins;

import com.dtsx.dh.core.common.ClientLanguage;
import lombok.val;
import org.jetbrains.annotations.NotNull;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/// Shared positional `DRIVER` parameter for commands that select one or more [ClientLanguage]s (or
/// `all`) to act on.
///
/// A host that only handles a subset of languages (e.g. `dh clients test` has no client repo behind
/// `bash`) passes the languages it doesn't support as `excludedLanguages`; `all` then expands to
/// every other language, and naming an excluded language explicitly is a parameter error.
public class DriversMixin {
    @Parameters(
        description = "Client drivers to use (e.g., 'java', 'typescript', or 'all').",
        defaultValue = "${CLIENT_DRIVERS}",
        completionCandidates = Completions.class,
        paramLabel = "DRIVER",
        split = ","
    )
    public List<String> $drivers;

    @Spec
    private CommandSpec spec;

    private final Set<ClientLanguage> excludedLanguages;

    private DriversMixin(ClientLanguage... excludedLanguages) {
        this.excludedLanguages = Set.of(excludedLanguages);
    }

    /// Creates a [DriversMixin] excluding the given languages from `all` and rejecting them if named
    /// explicitly. Pass no arguments when a host supports every [ClientLanguage].
    public static DriversMixin exclude(ClientLanguage... excludedLanguages) {
        return new DriversMixin(excludedLanguages);
    }

    /// Resolves the parsed `$drivers` values into their [ClientLanguage]s, expanding `all` and
    /// validating along the way.
    public List<ClientLanguage> resolve() {
        val cmd = spec.commandLine();

        if ($drivers == null || $drivers.isEmpty()) {
            throw new ParameterException(cmd, "Must provide at least one client driver (or 'all') to run tests against. Use `-h` for help instead.");
        }

        val names = $drivers.stream().allMatch("all"::equalsIgnoreCase)
            ? allExceptExcluded()
            : $drivers;

        return names.stream().map((name) -> parse(cmd, name)).toList();
    }

    private List<String> allExceptExcluded() {
        return ClientLanguage.names().stream()
            .filter((name) -> !excludedLanguages.contains(ClientLanguage.valueOf(name)))
            .toList();
    }

    private ClientLanguage parse(CommandLine cmd, String langStr) {
        ClientLanguage lang;

        try {
            lang = ClientLanguage.valueOf(langStr.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ParameterException(cmd, "Invalid client language: " + langStr + ". Expected one of: " + String.join(", ", ClientLanguage.names()));
        }

        if (excludedLanguages.contains(lang)) {
            throw new ParameterException(cmd, lang.name().toLowerCase() + " is not a client language; there's no client repo behind it.");
        }

        return lang;
    }

    /// Completions for the `DRIVER` parameter: every [ClientLanguage] plus `all`, regardless of what
    /// a given host excludes.
    public static class Completions implements Iterable<String> {
        @Override
        public @NotNull Iterator<String> iterator() {
            return new ArrayList<>(ClientLanguage.names()) {{ add("all"); }}.stream().map(String::toLowerCase).iterator();
        }
    }
}
