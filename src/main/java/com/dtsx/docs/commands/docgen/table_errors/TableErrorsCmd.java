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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("^\\s*([*-]|\\d+\\.|[a-z]\\.)\\s+\\S.*");
    private static final Pattern ORDERED_ITEM_PATTERN = Pattern.compile("^\\s*\\d+\\.\\s+\\S.*");

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
            val body = toAsciiDoc(entry.path("body").asString(""));
            val desc = escapePipe(PLACEHOLDER_PATTERN.matcher(body).replaceAll("**[$1]**"));

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

    private enum LineKind { TEXT, LIST, CODE }

    /**
     * AsciiDoc needs a blank line between a paragraph, a list, and a literal (indented) block,
     * but the YAML bodies often omit them, so we insert them wherever the kind of line changes.
     * <p>
     * Once a numbered list starts, any other content is attached to the current item with a list
     * continuation (+), and code is wrapped in a delimited literal block. Otherwise the block would
     * end the list and the numbering would restart.
     */
    private String toAsciiDoc(String body) {
        val lines = body.lines().map(String::stripTrailing).toList();
        val out = new ArrayList<String>();
        LineKind prevKind = null;
        boolean inOrdered = false;

        for (int i = 0; i < lines.size(); i++) {
            val line = lines.get(i);

            if (line.isBlank()) {
                out.add("");
                prevKind = null;
                continue;
            }

            val kind = kindOf(lines, i);

            if (ORDERED_ITEM_PATTERN.matcher(line).matches()) {
                inOrdered = true;
            }

            if (inOrdered && kind == LineKind.CODE) {
                var end = i;
                while (end < lines.size() && !lines.get(end).isBlank() && kindOf(lines, end) == LineKind.CODE) {
                    end++;
                }
                val block = lines.subList(i, end);
                val indent = block.stream().mapToInt(l -> l.length() - l.stripLeading().length()).min().orElse(0);

                attachToListItem(out);
                out.add("....");
                block.forEach(l -> out.add(l.substring(indent)));
                out.add("....");

                prevKind = LineKind.CODE;
                i = end - 1;
                continue;
            }

            if (inOrdered && kind == LineKind.TEXT && Character.isWhitespace(line.charAt(0))) {
                attachToListItem(out);
            } else if (prevKind != null && prevKind != kind) {
                out.add("");
            }

            out.add((kind == LineKind.CODE) ? line : line.stripLeading());
            prevKind = kind;
        }

        return String.join("\n", out).trim();
    }

    private void attachToListItem(List<String> out) {
        while (!out.isEmpty() && out.getLast().isEmpty()) {
            out.removeLast();
        }
        out.add("+");
    }

    private LineKind kindOf(List<String> lines, int i) {
        val line = lines.get(i);

        if (LIST_ITEM_PATTERN.matcher(line).matches()) {
            return LineKind.LIST;
        }

        if (!Character.isWhitespace(line.charAt(0))) {
            return LineKind.TEXT;
        }

        // An indented line that introduces a list is prose, not a literal block
        if (line.endsWith(":")) {
            for (int j = i + 1; j < lines.size(); j++) {
                if (!lines.get(j).isBlank()) {
                    return LIST_ITEM_PATTERN.matcher(lines.get(j)).matches() ? LineKind.TEXT : LineKind.CODE;
                }
            }
        }
        return LineKind.CODE;
    }

    private String escapePipe(String text) {
        return text.replace("|", "\\|");
    }
}
