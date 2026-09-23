package com.dtsx.dh;

import com.dtsx.dh.commands.completions.CompgenCmd;
import com.dtsx.dh.commands.db.DbCmd;
import com.dtsx.dh.commands.docs.DocsCmd;
import com.dtsx.dh.commands.logs.LogsCmd;
import com.dtsx.dh.commands.startgate.StartgateCmd;
import com.dtsx.dh.lib.CliLogger;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.val;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Ansi.Style;
import picocli.CommandLine.Help.ColorScheme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.dtsx.dh.lib.ColorUtils.ACCENT_COLOR;

@Command(
    name = "dh",
    mixinStandardHelpOptions = true,
    subcommands = {
        DocsCmd.class,
        CompgenCmd.class,
        LogsCmd.class,
        StartgateCmd.class,
        DbCmd.class,
    }
)
public class HelperCli {
    public static Path CLI_DIR = Path.of(Optional.ofNullable(System.getenv("CLI_DIR")).orElse(".")).toAbsolutePath();

    /// Scopes that get their own `.env.<scope>` file, keyed by the first CLI argument that
    /// selects them (e.g. `dh docs test ...` -> scope `docs`). Matched anywhere in the args rather
    /// than at position 0, so it keeps working if the root command ever grows options of its own.
    ///
    /// Adding a new scope (e.g. `clients`, `db`) is a one-line change here.
    private static final Set<String> ENV_SCOPES = Set.of("docs");

    @SuppressWarnings("UnnecessaryModifier")
    public static void main(String[] args) {
        loadDotenvFiles(args);

        System.setProperty("APPROVALTESTS_PROJECT_DIRECTORY", CLI_DIR.toString());

        val cli = new CommandLine(new HelperCli())
            .setCaseInsensitiveEnumValuesAllowed(true)
            .setColorScheme(new ColorScheme.Builder()
                .commands(ACCENT_COLOR)
                .options(ACCENT_COLOR)
                .parameters(ACCENT_COLOR)
                .optionParams(Style.italic)
                .build()
            )
            .setExecutionExceptionHandler((ex, _, _) -> {
                CliLogger.exception(ex);
                throw ex;
            });

        val exitCode = cli.execute(args);
        System.exit(exitCode);
    }

    /// Loads `.env.common`, then `.env.<scope>` (if any CLI arg names a known scope),
    /// from both `./` and `CLI_DIR`.
    ///
    /// A bare `.env`, if present, is loaded as though it were `.env.common`, with a deprecation warning.
    ///
    /// Values are applied as system properties (which is what `${VAR}` defaultValue expressions
    /// resolve against) and only for keys the real environment doesn't already define, so a stale
    /// file can never shadow what the caller actually exported.
    private static void loadDotenvFiles(String[] args) {
        val scope = Arrays.stream(args)
            .filter(ENV_SCOPES::contains)
            .findFirst();

        val dirs = Stream.of(Path.of("."), CLI_DIR)
            .map((dir) -> dir.toAbsolutePath().normalize())
            .distinct()
            .toList();

        for (val dir : dirs) {
            val bareEnvFile = dir.resolve(".env");

            if (Files.isRegularFile(bareEnvFile)) {
                // stderr, not CliLogger: `dh compgen`'s stdout gets `source`d by scripts/dev-alias.sh,
                // so anything printed there has to be a valid shell script.
                System.err.println("[WARN] Found a bare `.env` file at `" + bareEnvFile + "` - please rename it to `.env.common`. Loading it as `.env.common` for now.");
                applyDotenvFile(bareEnvFile);
            }

            applyDotenvFile(dir.resolve(".env.common"));

            scope.ifPresent((s) -> applyDotenvFile(dir.resolve(".env." + s)));
        }
    }

    /// Reads the given dotenv file (if it exists) and applies its entries as system properties,
    /// but only for keys not already present in the real environment.
    private static void applyDotenvFile(Path file) {
        if (!Files.isRegularFile(file)) {
            return;
        }

        val dotenv = Dotenv.configure()
            .directory(file.toAbsolutePath().getParent().toString())
            .filename(file.getFileName().toString())
            .ignoreIfMissing()
            .load();

        for (val entry : dotenv.entries()) {
            if (System.getenv(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        }
    }
}
