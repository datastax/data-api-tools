package com.dtsx.dh.core.docs.planner.meta.snapshot;

import com.dtsx.dh.core.docs.planner.PlanException;
import com.dtsx.dh.core.docs.planner.meta.snapshot.SnapshotTestMetaRep.SnapshotsConfig;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.output.OutputCaptureSource;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.output.OutputJsonifySource;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.output.OutputMatchesSource;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.records.DocumentsSource;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.records.RowsSource;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.schema.definitions.CollectionDefinitionSource;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.schema.definitions.TableDefinitionSource;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.schema.definitions.TableIndexDefinitionsSource;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.schema.definitions.UdtDefinitionsSource;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.schema.names.CollectionNamesSource;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.schema.names.TableIndexNamesSource;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.schema.names.TableNamesSource;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.schema.names.UdtNamesSource;
import com.dtsx.dh.lib.JacksonUtils;
import lombok.val;
import tools.jackson.core.type.TypeReference;

import java.util.*;
import java.util.function.BiFunction;

public class SnapshotSourcesParser {
    private static final Map<String, BiFunction<String, Map<String, Object>, SnapshotSource>> PARSERS = new HashMap<>() {{
        for (val stream : List.of("stdout", "stderr")) {
            put(stream + "::capture", mk(OutputCaptureSource::new, new TypeReference<>() {}));
            put(stream + "::matches", mk(OutputMatchesSource::new, new TypeReference<>() {}));
            put(stream + "::jsonify", mk(OutputJsonifySource::new, new TypeReference<>() {}));
        }
        {
            put("collection::documents", mk(DocumentsSource::new, new TypeReference<>() {}));
            put("collection::definition", mk(CollectionDefinitionSource::new, new TypeReference<>() {}));
            put("collection::names", mk(CollectionNamesSource::new, new TypeReference<>() {}));
        }
        {
            put("table::rows", mk(RowsSource::new, new TypeReference<>() {}));
            put("table::definition", mk(TableDefinitionSource::new, new TypeReference<>() {}));
            put("table::names", mk(TableNamesSource::new, new TypeReference<>() {}));
        }
        {
            put("table::index::names", mk(TableIndexNamesSource::new, new TypeReference<>() {}));
            put("table::index::definitions", mk(TableIndexDefinitionsSource::new, new TypeReference<>() {}));
        }
        {
            put("udt::names", mk(UdtNamesSource::new, new TypeReference<>() {}));
            put("udt::definitions", mk(UdtDefinitionsSource::new, new TypeReference<>() {}));
        }
    }};

    public static List<SnapshotSource> parseSources(SnapshotsConfig config) {
        val sources = new ArrayList<SnapshotSource>();

        for (val rawSource : config.sources().entrySet()) {
            val source = rawSource.getKey();
            val params = Objects.requireNonNullElse(rawSource.getValue(), Collections.<String, Object>emptyMap());

            val parser = PARSERS.get(source);

            if (parser == null) {
                throw new PlanException("Unknown snapshot source type: " + source);
            }

            sources.add(parser.apply(source, params));
        }

        return sources;
    }

    private static <M> BiFunction<String, Map<String, Object>, SnapshotSource> mk(BiFunction<String, M, SnapshotSource> constructor, TypeReference<M> typeRef) {
        return (name, params) -> constructor.apply(name, JacksonUtils.convertValue(params, typeRef));
    }
}
