package com.dtsx.dh.core.docs.planner.fixtures;

import com.dtsx.dh.core.docs.planner.fixtures.FixtureMetadata.Initialization;
import com.dtsx.dh.lib.CliLogger;
import com.dtsx.dh.lib.ExecutorUtils;
import com.dtsx.dh.lib.ExecutorUtils.DirectExecutor;
import com.dtsx.dh.lib.ExternalPrograms.ExternalProgram;
import lombok.SneakyThrows;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class BaseFixturePool implements Comparable<BaseFixturePool> {
    public record FixtureIndex(int unwrap) {
        public static final FixtureIndex ZERO = new FixtureIndex(0);

        public String toNameRoot() {
            return "n" + unwrap;
        }
    }

    private final JSFixture baseFixture;
    private final BlockingQueue<FixtureIndex> available;
    private final ConcurrentMap<FixtureIndex, FixtureMetadata> metadataCache;

    public BaseFixturePool(JSFixture baseFixture, int poolSize) {
        this(baseFixture, 0, poolSize, new ConcurrentHashMap<>());
    }

    private BaseFixturePool(JSFixture baseFixture, int start, int end, ConcurrentMap<FixtureIndex, FixtureMetadata> metadataCache) {
        this.baseFixture = baseFixture;
        this.metadataCache = metadataCache;

        this.available = new LinkedBlockingQueue<>(
            IntStream.range(start, end)
                .mapToObj(FixtureIndex::new)
                .toList()
        );
    }

    public BaseFixturePool slice(int start, int end) {
        return new BaseFixturePool(baseFixture, Math.min(start, available.size()), Math.min(end, available.size() + 1), metadataCache);
    }

    public int size() {
        return available.size();
    }

    public JSFixture fixture() {
        return baseFixture;
    }

    @SneakyThrows
    public FixtureIndex acquire() {
        return available.take();
    }

    public void release(FixtureIndex index) {
        available.offer(index);
    }

    public FixtureMetadata meta(ExternalProgram tsx, FixtureIndex index) {
        if (metadataCache.containsKey(index)) {
            return metadataCache.get(index);
        }

        val metadata = baseFixture.meta(tsx, index);
        metadataCache.put(index, metadata);
        return metadata;
    }

    public void setup(ExternalProgram tsx) {
        executeAll(tsx, "Setup", (meta) -> {
            baseFixture.setup(tsx, meta);
        });
    }

    public void beforeEach(ExternalProgram tsx) {
        executeAll(tsx, "BeforeEach", (meta) -> {
            baseFixture.beforeEach(tsx, meta, null);
        });
    }

    public void afterEach(ExternalProgram tsx) {
        executeAll(tsx, "AfterEach", (meta) -> {
            baseFixture.afterEach(tsx, meta, null);
        });
    }

    public void teardown(ExternalProgram tsx) {
        executeAll(tsx, "Teardown", (meta) -> {
            baseFixture.teardown(tsx, meta);
        });
    }

    @SneakyThrows
    private void executeAll(ExternalProgram tsx, String function, Consumer<FixtureMetadata> action) {
        val parallelInit = meta(tsx, FixtureIndex.ZERO).initialization() == Initialization.PARALLEL;

        val initExecutor = (parallelInit)
            ? Executors.newVirtualThreadPerTaskExecutor()
            : DirectExecutor.INSTANCE;

        try (initExecutor) {
            val futures = ExecutorUtils.emptyFuturesList();

            for (var i = 0; i < available().size(); i++) {
                val progress =
                    (available().size() == 1)
                        ? "" :
                    (parallelInit)
                        ? " (%d times in parallel)".formatted(available().size())
                        : " (%d/%d)".formatted(i + 1, available().size());

                val index = new FixtureIndex(i);

                futures.add(initExecutor.submit(() -> {
                    CliLogger.loading("Calling @!%s!@ in @!_fixtures/%s!@%s".formatted(function, baseFixture.fixtureName(), progress), (_) -> {
                        action.accept(meta(tsx, index));
                        return null;
                    });
                }));
            }

            ExecutorUtils.awaitAll(futures);
        }
    }

    protected BlockingQueue<FixtureIndex> available() {
        return available;
    }

    @Override
    public int compareTo(@NotNull BaseFixturePool o) {
        return baseFixture.compareTo(o.baseFixture);
    }
}
