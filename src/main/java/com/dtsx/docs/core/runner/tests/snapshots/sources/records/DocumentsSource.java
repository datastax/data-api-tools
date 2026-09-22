package com.dtsx.docs.core.runner.tests.snapshots.sources.records;

import com.datastax.astra.client.collections.commands.options.CollectionFindOptions;
import com.datastax.astra.client.collections.definition.documents.Document;
import com.dtsx.docs.commands.docs.test.TestCtx;
import com.dtsx.docs.core.planner.meta.snapshot.meta.RecordSourceMeta.DocumentsSourceMeta;
import com.dtsx.docs.core.runner.Placeholders;
import com.dtsx.docs.lib.DataAPIUtils;
import lombok.val;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/// Implementation of [RecordSource] that captures documents from a collection.
public final class DocumentsSource extends RecordSource {
    public DocumentsSource(String name, DocumentsSourceMeta meta) {
        super(name, meta);
    }

    @Override
    protected Optional<String> extractSchemaObjectName(Placeholders placeholders) {
        return placeholders.collectionName();
    }

    @Override
    public Stream<Map<String, Object>> streamRecords(TestCtx ctx, String name, String keyspace) {
        val collection = DataAPIUtils.getCollection(ctx.connectionInfo(), name, keyspace);

        val options = new CollectionFindOptions();
        projection.ifPresent(options::projection);

        return collection.find(filter.orElse(null), options).stream().map(Document::getDocumentMap);
    }
}
