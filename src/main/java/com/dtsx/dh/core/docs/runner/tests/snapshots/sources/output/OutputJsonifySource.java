package com.dtsx.dh.core.docs.runner.tests.snapshots.sources.output;

import com.dtsx.dh.commands.docs.test.DocsTestCtx;
import com.dtsx.dh.core.docs.planner.fixtures.FixtureMetadata;
import com.dtsx.dh.core.docs.planner.meta.snapshot.meta.OutputJsonifySourceMeta;
import com.dtsx.dh.core.docs.runner.drivers.ClientDriver;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.SnapshotSource;
import com.dtsx.dh.core.docs.runner.tests.snapshots.sources.SnapshotSourceUtils;
import com.dtsx.dh.lib.ExternalPrograms.RunResult;
import com.dtsx.dh.lib.JacksonUtils;
import lombok.val;

import java.util.Collection;
import java.util.regex.Pattern;

import static com.dtsx.dh.lib.JacksonUtils.runJq;

public class OutputJsonifySource extends SnapshotSource {
    private final OutputJsonifySourceMeta meta;

    public OutputJsonifySource(String name, OutputJsonifySourceMeta meta) {
        super(name);
        this.meta = meta;
    }

    @Override
    public String mkSnapshotImpl(DocsTestCtx ctx, ClientDriver driver, RunResult res, FixtureMetadata md) {
        val output = SnapshotSourceUtils.extractOutput(name, res);
        val rawJsonLines = driver.preprocessToJson(ctx, meta, output);

        var jsonAsString = JacksonUtils.formatJsonCompact(rawJsonLines);
        jsonAsString = JsonScrubber.scrub(jsonAsString);

        if (meta.jq().isPresent()) {
            jsonAsString = runJq(ctx, jsonAsString, meta.jq().get());
        }

        val parsedJson = JacksonUtils.parseJson(jsonAsString, Object.class);
        val shouldSort = meta.sort().orElse(true);

        val deterministicJson = (!shouldSort && parsedJson instanceof Collection<?> c)
            ? c.stream().map(SnapshotSourceUtils::mkJsonDeterministic).toList()
            : SnapshotSourceUtils.mkJsonDeterministic(parsedJson);

        val finalJson = (deterministicJson instanceof Collection<?> c && c.size() == 1 && (c.iterator().next() instanceof Collection<?>))
            ? c.iterator().next()
            : deterministicJson;

        return JacksonUtils.formatJsonPretty(finalJson);
    }

    @SuppressWarnings("SameParameterValue")
    private static final class JsonScrubber {
        private static final Pattern RFC_DATE_PATTERN =
            Pattern.compile("\"\\b\\d{4}-\\d{2}-\\d{2}\\b\"");

        private static final Pattern SLASH_DATE_PATTERN =
            Pattern.compile("\"\\b\\d{2}/\\d{2}/\\d{4}\\b\"");

        public static final Pattern DATE_OBJECT_PATTERN =
            Pattern.compile("\\{\"date\":\\d+?,\"month\":\\d+?,\"year\":\\d+?}");

        private static final Pattern RFC_TIME_PATTERN =
            Pattern.compile("\"\\b\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?\"");

        public static final Pattern TIME_OBJECT_PATTERN =
            Pattern.compile("\\{\"hours\":\\d+?,\"minutes\":\\d+?(,\"nanoseconds\":\\d+?)?,\"seconds\":\\d+?}");

        private static final Pattern RFC_TIMESTAMP_PATTERN =
            Pattern.compile("\"\\b\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d+)?(?:Z|[+-]\\d{2}:\\d{2})\\b\"");

        private static String scrubDate(String json, String replacement) {
            json = RFC_DATE_PATTERN.matcher(json).replaceAll(replacement);
            json = DATE_OBJECT_PATTERN.matcher(json).replaceAll(replacement);
            json = SLASH_DATE_PATTERN.matcher(json).replaceAll(replacement);
            return json;
        }

        private static String scrubTime(String json, String replacement) {
            json = RFC_TIME_PATTERN.matcher(json).replaceAll(replacement);
            json = TIME_OBJECT_PATTERN.matcher(json).replaceAll(replacement);
            return json;
        }

        private static String scrubTimestamp(String json, String replacement) {
            json = RFC_TIMESTAMP_PATTERN.matcher(json).replaceAll(replacement);
            return json;
        }

        public static String scrub(String json) {
            json = scrubDate(json, "\"date_or_time\"");
            json = scrubTime(json, "\"date_or_time\"");
            json = scrubTimestamp(json, "\"date_or_time\"");
            return json;
        }
    }
}
