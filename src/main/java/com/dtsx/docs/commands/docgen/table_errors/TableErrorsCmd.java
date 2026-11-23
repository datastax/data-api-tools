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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
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

    /**
     * @param condition AsciiDoc attribute expression (e.g. {@code data-api-rerank-collections}) wrapped in
     *                  {@code ifdef::...[]} so the link is only rendered where the target page exists; null means always render.
     */
    private record SeeAlso(String xref, String text, String condition) {
        SeeAlso(String xref, String text) {
            this(xref, text, null);
        }

        SeeAlso when(String condition) {
            return new SeeAlso(xref, text, condition);
        }
    }

    // Attributes defined in the antora.yml of the docs that have the target pages (not defined for HCD)
    private static final String RERANK = "data-api-rerank-collections";
    private static final String BM25 = "data-api-bm25";
    private static final String VECTORIZE = "data-api-vectorize";
    private static final String UDT = "data-api-udt";
    private static final String ASTRA = "env-astra";

    /**
     * Error codes are only unique within a scope, so the see-also lookup is keyed on both.
     */
    private record ErrorKey(String scope, String code) {}

    /**
     * A see-also link to an {@code api-reference} page. Links without an anchor use empty text so the page title is rendered.
     */
    private static SeeAlso link(String page) {
        return new SeeAlso("xref:api-reference:" + page, "");
    }

    /**
     * A see-also link to an anchor within an {@code api-reference} page, using the heading text of the anchor.
     */
    private static SeeAlso link(String page, String text) {
        return new SeeAlso("xref:api-reference:" + page, text);
    }

    /**
     * A map entry for an error code. A null scope means the error has no scope (shown as "NONE" in the source list).
     */
    private static Map.Entry<ErrorKey, List<SeeAlso>> see(String scope, String code, SeeAlso... links) {
        return Map.entry(new ErrorKey(scope, code), List.of(links));
    }

    private static final Map<ErrorKey, List<SeeAlso>> ERROR_CODE_SEE_ALSO_MAP = Map.ofEntries(
        // No scope
        see(null, "HYBRID_FIELD_CONFLICT", link("collection-hybrid-field.adoc").when(RERANK)),
        see(null, "HYBRID_FIELD_UNSUPPORTED_VALUE_TYPE", link("collection-hybrid-field.adoc").when(RERANK)),
        see(null, "HYBRID_FIELD_UNKNOWN_SUBFIELDS", link("collection-hybrid-field.adoc").when(RERANK)),
        see(null, "HYBRID_FIELD_UNSUPPORTED_SUBFIELD_VALUE_TYPE", link("collection-hybrid-field.adoc").when(RERANK)),
        see(null, "INVALID_CREATE_COLLECTION_FIELD", link("collection-methods/create-collection.adoc")),
        see(null, "UNSUPPORTED_TABLE_COMMAND", link("tables.adoc"), link("collections.adoc")),
        see(null, "UNSUPPORTED_COLLECTION_COMMAND", link("tables.adoc"), link("collections.adoc")),
        see(null, "UNSUPPORTED_RERANKING_COMMAND", link("document-methods/find-and-rerank.adoc").when(RERANK)),
        see(null, "INVALID_RERANK_OVERRIDE", link("document-methods/find-and-rerank.adoc#example-override", "Override the collection's rerank provider").when(RERANK)),
        see(null, "MISSING_RERANK_ON", link("document-methods/find-and-rerank.adoc").when(RERANK)),
        see(null, "MISSING_RERANK_QUERY_TEXT", link("document-methods/find-and-rerank.adoc").when(RERANK)),

        // DOCUMENT
        see("DOCUMENT", "DOCUMENT_REPLACE_DIFFERENT_DOCID", link("document-methods/replace-one.adoc")),
        see("DOCUMENT", "SHRED_BAD_BINARY_VECTOR_VALUE", link("collection-vector-field.adoc#binary-encoding-of-vector-embeddings", "Binary encoding of vector embeddings")),
        see("DOCUMENT", "SHRED_BAD_DOCID_TYPE", link("document-id.adoc")),
        see("DOCUMENT", "SHRED_BAD_DOCID_VALUE", link("document-id.adoc")),
        see("DOCUMENT", "SHRED_BAD_DOCUMENT_VECTOR_TYPE", link("collection-vector-field.adoc")),
        see("DOCUMENT", "SHRED_BAD_DOCUMENT_LEXICAL_TYPE", link("collection-lexical-field.adoc").when(RERANK)),
        see("DOCUMENT", "SHRED_BAD_FIELD_NAME", link("collection-field-name-rules.adoc")),
        see("DOCUMENT", "UNSUPPORTED_COLUMN_TYPES", link("table-data-types.adoc")),

        // FILTER
        see("FILTER", "FILTER_PATH_UNINDEXED", link("collection-indexes.adoc#configure-indexing-for-collections", "Configure indexing for collections")),
        see("FILTER", "CANNOT_LEXICAL_FILTER_NON_INDEXED_COLUMNS", link("table-index-methods/create-text-index.adoc").when(BM25)),
        see("FILTER", "FILTER_INVALID_EXPRESSION", link("filter-operator-collections.adoc"), link("filter-operators-tables.adoc")),
        see("FILTER", "FILTER_UNSUPPORTED_OPERATOR", link("filter-operator-collections.adoc"), link("filter-operators-tables.adoc")),
        see("FILTER", "INVALID_MAP_SET_LIST_FILTER", link("filter-operators-tables.adoc")),
        see("FILTER", "UNSUPPORTED_COLUMN_TYPES", link("table-data-types.adoc")),
        see("FILTER", "UNSUPPORTED_FILTERING_FOR_COLUMN_TYPES", link("filter-operators-tables.adoc")),

        // UPDATE
        see("UPDATE", "UNSUPPORTED_UPDATE_OPERATOR", link("update-operators-tables.adoc")),
        see("UPDATE", "INVALID_PUSH_OPERATOR_USAGE", link("update-operator-collections.adoc#push", "Update operators for collections"), link("update-operators-tables.adoc#push", "Update operators for collections")),
        see("UPDATE", "UNSUPPORTED_UPDATE_OPERATION", link("update-operator-collections.adoc"), link("update-operators-tables.adoc")),
        see("UPDATE", "UNSUPPORTED_UPDATE_OPERATION_MODIFIER", link("update-operator-collections.adoc"), link("update-operators-tables.adoc")),
        see("UPDATE", "UNSUPPORTED_UPDATE_OPERATION_PARAM", link("update-operator-collections.adoc"), link("update-operators-tables.adoc")),
        see("UPDATE", "UNSUPPORTED_UPDATE_OPERATION_PATH", link("update-operator-collections.adoc"), link("update-operators-tables.adoc")),
        see("UPDATE", "UNSUPPORTED_UPDATE_OPERATION_TARGET", link("update-operator-collections.adoc"), link("update-operators-tables.adoc")),
        see("UPDATE", "MISSING_UPDATE_OPERATIONS", link("update-operators-tables.adoc")),
        see("UPDATE", "UNSUPPORTED_UPDATE_OPERATIONS_FOR_TABLE", link("update-operators-tables.adoc")),
        see("UPDATE", "UNSUPPORTED_COLUMN_TYPES", link("table-data-types.adoc")),

        // WARNING
        see("WARNING", "ZERO_FILTER_OPERATIONS", link("filter-operators-tables.adoc")),

        // PROJECTION
        see("PROJECTION", "UNSUPPORTED_COLUMN_TYPES", link("table-data-types.adoc")),
        see("PROJECTION", "UNSUPPORTED_PROJECTION_PARAM", link("projections-tables.adoc"), link("projections-collections.adoc")),

        // SCHEMA
        see("SCHEMA", "DEPRECATED_AI_MODEL", link("table-migrate-vectorize-model.adoc").when(ASTRA), link("collection-migrate-vectorize-model.adoc").when(ASTRA), link("admin-methods/find-reranking-providers.adoc").when(ASTRA), link("admin-methods/find-embedding-providers.adoc").when(ASTRA)),
        see("SCHEMA", "END_OF_LIFE_AI_MODEL", link("table-migrate-vectorize-model.adoc").when(ASTRA), link("collection-migrate-vectorize-model.adoc").when(ASTRA), link("admin-methods/find-reranking-providers.adoc").when(ASTRA), link("admin-methods/find-embedding-providers.adoc").when(ASTRA)),
        see("SCHEMA", "INVALID_INDEXING_DEFINITION", link("collection-indexes.adoc")),
        see("SCHEMA", "MISSING_PARTITION_COLUMNS", link("primary-keys.adoc")),
        see("SCHEMA", "MISSING_ALTER_TABLE_OPERATIONS", link("table-methods/alter-table.adoc")),
        see("SCHEMA", "MISSING_ALTER_TYPE_OPERATIONS", link("table-methods/alter-type.adoc").when(UDT)),
        see("SCHEMA", "MISSING_FIELDS_FOR_TYPE_CREATION", link("table-methods/create-type.adoc").when(UDT)),
        see("SCHEMA", "RERANKING_SERVICE_TYPE_UNAVAILABLE", link("admin-methods/find-reranking-providers.adoc").when(ASTRA)),
        see("SCHEMA", "TOO_MANY_COLLECTIONS", link("collection-methods/drop-collection.adoc")),
        see("SCHEMA", "UNKNOWN_DATA_TYPE", link("table-data-types.adoc")),
        see("SCHEMA", "UNKNOWN_PRIMITIVE_DATA_TYPE", link("table-data-types.adoc")),
        see("SCHEMA", "UNSUPPORTED_DATA_TYPE_TABLE_CREATION", link("table-data-types.adoc")),
        see("SCHEMA", "UNSUPPORTED_JSON_TYPE_FOR_TEXT_INDEX", link("table-index-methods/create-text-index.adoc").when(BM25)),
        see("SCHEMA", "UNSUPPORTED_TEXT_ANALYSIS_FOR_DATA_TYPES", link("table-index-methods/create-index.adoc")),
        see("SCHEMA", "UNSUPPORTED_TEXT_INDEX_FOR_DATA_TYPES", link("table-index-methods/create-text-index.adoc").when(BM25)),
        see("SCHEMA", "UNSUPPORTED_VECTOR_INDEX_FOR_DATA_TYPES", link("table-index-methods/create-vector-index.adoc")),
        see("SCHEMA", "CANNOT_ADD_UNSUPPORTED_DATA_TYPE_COLUMNS", link("table-data-types.adoc")),
        see("SCHEMA", "INVALID_INDEX_DEFINITION", link("table-index-methods/create-index.adoc"), link("table-index-methods/create-text-index.adoc").when(BM25), link("table-index-methods/create-vector-index.adoc")),
        see("SCHEMA", "PRIMARY_KEY_DEFINITION_INCORRECT", link("primary-keys.adoc")),
        see("SCHEMA", "INVALID_FORMAT_FOR_INDEX_CREATION_COLUMN", link("table-index-methods/create-index.adoc")),
        see("SCHEMA", "INVALID_VECTORIZE_FIELD_CONFIGURATION", link("table-methods/create-table.adoc#example-vectorize", "Create a table with a column to automatically generate vector embeddings").when(VECTORIZE)),
        see("SCHEMA", "INVALID_VECTORIZE_TABLE_CONFIGURATION", link("table-methods/create-table.adoc#example-vectorize", "Create a table with a column to automatically generate vector embeddings").when(VECTORIZE)),

        // SORT
        see("SORT", "CANNOT_LEXICAL_SORT_NON_INDEXED_COLUMNS", link("table-index-methods/create-text-index.adoc").when(BM25)),
        see("SORT", "CANNOT_SORT_ON_SPECIAL_WITH_OTHERS", link("sort-rows.adoc")),
        see("SORT", "INVALID_REGULAR_SORT_EXPRESSION", link("sort-documents.adoc"), link("sort-rows.adoc")),
        see("SORT", "INVALID_VECTOR_SORT_EXPRESSION", link("sort-documents.adoc"), link("sort-rows.adoc")),
        see("SORT", "MISSING_HYBRID_SORT_VALUE", link("document-methods/find-and-rerank.adoc").when(RERANK)),
        see("SORT", "MISSING_VECTOR_OR_VECTORIZE_IN_HYBRID_SORT", link("document-methods/find-and-rerank.adoc").when(RERANK)),
        see("SORT", "SORT_CLAUSE_INVALID", link("sort-documents.adoc"), link("sort-rows.adoc")),
        see("SORT", "UNSUPPORTED_FIND_AND_RERANK_SORT", link("document-methods/find-and-rerank.adoc").when(RERANK)),
        see("SORT", "UNSUPPORTED_VECTOR_SORT_FOR_COLLECTION", link("collection-methods/create-collection.adoc#example-vector", "Create a collection that can store vector embeddings")),
        see("SORT", "UNSUPPORTED_VECTORIZE_SORT_FOR_COLLECTION", link("collection-methods/create-collection.adoc#example-vectorize", "Create a collection that can automatically generate vector embeddings").when(VECTORIZE))
    );

    private static final String SNIPPET_PREFIX = "SNIPPET.";
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

        val snippets = new HashMap<String, String>();
        root.path("snippets").forEach(s -> snippets.put(SNIPPET_PREFIX + s.path("name").asString(""), s.path("body").asString("")));

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
            val rawScope = entry.hasNonNull("scope") ? entry.get("scope").asString() : null;
            val scope = (rawScope != null) ? rawScope : "n/a";
            val title = escapePipe(entry.path("title").asString(""));
            val body = toAsciiDoc(expandSnippets(entry.path("body").asString(""), snippets));
            val desc = escapePipe(PLACEHOLDER_PATTERN.matcher(body).replaceAll("**[$1]**"));

            sb.append(String.format("| `%s`\n| %s\n| %s\na| %s\n", code, scope, title, desc));

            val seeAlsoList = ERROR_CODE_SEE_ALSO_MAP.get(new ErrorKey(rawScope, code));
            if (seeAlsoList != null && !seeAlsoList.isEmpty()) {
                sb.append("\n").append(renderSeeAlso(seeAlsoList)).append("\n");
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

    /**
     * Renders the "See also" sentence. Asciidoctor only processes {@code ifdef} at the start of a line, so conditional
     * links can't be inlined in the sentence. Instead, the whole sentence is conditional if every link is, and if only
     * some links are, there is one sentence with all links and one with just the unconditional links.
     */
    private String renderSeeAlso(List<SeeAlso> links) {
        val conditions = links.stream().map(SeeAlso::condition).filter(c -> c != null).distinct().toList();

        if (conditions.isEmpty()) {
            return seeAlsoSentence(links);
        }

        if (conditions.size() > 1) {
            throw new IllegalStateException("Unsupported mix of conditions " + conditions + " in see-also links " + links);
        }

        val condition = conditions.getFirst();
        val unconditional = links.stream().filter(l -> l.condition() == null).toList();

        val sb = new StringBuilder(String.format("ifdef::%s[%s]", condition, seeAlsoSentence(links)));
        if (!unconditional.isEmpty()) {
            sb.append(String.format("\nifndef::%s[%s]", condition, seeAlsoSentence(unconditional)));
        }
        return sb.toString();
    }

    private String seeAlsoSentence(List<SeeAlso> links) {
        val rendered = links.stream().map(l -> String.format("%s[%s]", l.xref(), l.text())).toList();

        val joined = switch (rendered.size()) {
            case 1 -> rendered.getFirst();
            case 2 -> rendered.get(0) + " and " + rendered.get(1);
            default -> String.join(", ", rendered.subList(0, rendered.size() - 1)) + ", and " + rendered.getLast();
        };

        return "See also " + joined + ".";
    }

    /**
     * Replaces {@code ${SNIPPET.NAME}} placeholders with the snippet's body. Unknown snippets are left as-is
     * so they still render as a bold placeholder.
     */
    private String expandSnippets(String body, Map<String, String> snippets) {
        return PLACEHOLDER_PATTERN.matcher(body).replaceAll(m -> {
            val snippet = snippets.get(m.group(1));
            return Matcher.quoteReplacement(snippet != null ? snippet : m.group());
        });
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
