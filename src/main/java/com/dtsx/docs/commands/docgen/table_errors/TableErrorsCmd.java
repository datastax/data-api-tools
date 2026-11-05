package com.dtsx.docs.commands.docgen.table_errors;

import com.dtsx.docs.commands.BaseCmd;
import com.dtsx.docs.lib.CliLogger;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.val;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@Command(
    name = "table-errors",
    description = "Generates an AsciiDoc table of request-errors from an errors.yaml file",
    mixinStandardHelpOptions = true
)
public class TableErrorsCmd extends BaseCmd<TableErrorsCtx> {
    @Mixin @Getter
    private TableErrorsArgs $args;

    private static final String TAG_START = "// tag::generated-table[]";
    private static final String TAG_END   = "// end::generated-table[]";

    private static final Pattern TABLE_REGION_PATTERN = Pattern.compile(
        Pattern.quote(TAG_START) + ".*?" + Pattern.quote(TAG_END),
        Pattern.DOTALL
    );

    private static final Map<String, String[]> ERROR_CODE_SEE_ALSO_MAP = Map.of(
        "DOCUMENT_REPLACE_DIFFERENT_DOCID", new String[] {
            "xref:api-reference:document-methods/replace-one.adoc",
            ""
        },
        "FILTER_PATH_UNINDEXED", new String[] {
            "xref:api-reference:collection-indexes.adoc#configure-indexing-for-collections",
            "Selective indexing"
        }
    );

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    @Override
    @SneakyThrows
    protected int run() {
        val input = ctx.input();
        val output = Path.of(ctx.output());

        CliLogger.println(false, "@|bold Processing error codes from " + input + "...|@");

        val mapper = new ObjectMapper(new YAMLFactory());
        JsonNode root;

        try (val is = mkInputStream(input)) {
            root = mapper.readTree(is);
        }

        val errors = root.findValues("request-errors").stream()
            .flatMap(node -> StreamSupport.stream(node.spliterator(), false))
            .sorted(Comparator.comparing(n -> n.path("code").asString("")))
            .toList();

        // Build the replacement block (everything between and including the tags)
        val sb = new StringBuilder();
        sb.append(TAG_START).append("\n");
        sb.append("[cols=\"1,1,2,3\"]\n|===\n| Error code | Scope | Title | Description\n\n");

        for (val entry : errors) {
            val code = entry.path("code").asString("");
            val scope = entry.hasNonNull("scope") ? entry.get("scope").asString() : "n/a";
            val title = escapePipe(entry.path("title").asString(""));
            val desc = escapePipe(PLACEHOLDER_PATTERN.matcher(entry.path("body").asString("").split("\r?\n", 2)[0].trim()).replaceAll("**[$1]**"));

            sb.append(String.format("| `%s`\n| %s\n| %s\na| %s\n", code, scope, title, desc));

            if (ERROR_CODE_SEE_ALSO_MAP.containsKey(code)) {
                String[] linkInfo = ERROR_CODE_SEE_ALSO_MAP.get(code);
                sb.append(String.format("\nSee also %s[%s].\n", linkInfo[0], linkInfo[1]));
            }
            sb.append("\n");
        }

        sb.append("|===\n");
        sb.append(TAG_END);

        // Read the existing file and splice in the new table
        val existing = Files.readString(output);
        val matcher = TABLE_REGION_PATTERN.matcher(existing);

        if (!matcher.find()) {
            throw new IllegalStateException("Could not find generated-table tags in " + output + ". Expected markers: '" + TAG_START + "' and '" + TAG_END + "'");
        }

        val updated = new StringBuilder(existing);
        updated.replace(matcher.start(), matcher.end(), sb.toString());

        Files.writeString(output, updated.toString());

        CliLogger.println(false, "@|green ✓|@ Written " + errors.size() + " rows to " + output);

        return 0;
    }

    private InputStream mkInputStream(String input) throws Exception {
        if (input.startsWith("http")) {
            return URI.create(input).toURL().openStream();
        } else {
            return Files.newInputStream(Path.of(input));
        }
    }

    private String escapePipe(String text) {
        return text.replace("|", "\\|");
    }
}
