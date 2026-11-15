package com.dtsx.dh;

import com.dtsx.dh.commands.clients.ClientsCmd;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.dtsx.dh.lib.ColorUtils.ACCENT_COLOR;

@Command(
    name = "dh",
    mixinStandardHelpOptions = true,
    subcommands = {
        DocsCmd.class,
        ClientsCmd.class,
        CompgenCmd.class,
        LogsCmd.class,
        StartgateCmd.class,
        DbCmd.class,
    }
)
public class HelperCli {
    public static Path CLI_DIR = Path.of(Optional.ofNullable(System.getenv("CLI_DIR")).orElse(".")).toAbsolutePath();

    /// The set of scopes that can get their own `.env.dh.<scope>` file is derived from the
    /// registered subcommand names themselves (via reflection on their `@Command` annotations)
    /// rather than a hand-maintained list, so a new subcommand picks up scoped env support for
    /// free instead of needing a second edit here.
    ///
    /// This is read via reflection, not by building a `CommandLine` and calling
    /// `getSubcommands()`, because that would require constructing the `CommandLine` before
    /// `loadDotenvFiles` runs -- and `applyDotenvFile` has to run first, since its system
    /// properties are what `${VAR}` defaultValue expressions resolve against.
    private static Set<String> registeredSubcommandNames() {
        return Arrays.stream(HelperCli.class.getAnnotation(Command.class).subcommands())
            .map((cls) -> cls.getAnnotation(Command.class).name())
            .collect(Collectors.toSet());
    }

    @SuppressWarnings({ "UnnecessaryModifier", "InstantiationOfUtilityClass" })
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
    /// A bare `.env`, if present, is ignored, with a warning, since it may belong to something else
    /// entirely.
    ///
    /// Values are applied as system properties (which is what `${VAR}` defaultValue expressions
    /// resolve against) and only for keys the real environment doesn't already define, so a stale
    /// file can never shadow what the caller actually exported.
    private static void loadDotenvFiles(String[] args) {
        val scopes = registeredSubcommandNames();

        val scope = Arrays.stream(args)
            .filter(scopes::contains)
            .findFirst();

        val dirs = Stream.of(Path.of("."), CLI_DIR)
            .map((dir) -> dir.toAbsolutePath().normalize())
            .distinct()
            .toList();

        for (val dir : dirs) {
            applyDotenvFile(dir.resolve(".env.dh.common"));
            scope.ifPresent((s) -> applyDotenvFile(dir.resolve(".env.dh." + s)));
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
